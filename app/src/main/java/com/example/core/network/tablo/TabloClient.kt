package com.example.core.network.tablo

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class TabloResult<out T> {
    data class Success<T>(val data: T) : TabloResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : TabloResult<Nothing>()
}

/**
 * Tablo Local Network REST Client adhering to:
 * https://jessedp.github.io/tablo-api-docs/#tablo-api-introduction
 *
 * Interacts with physical Tablo Network Connected DVRs on port 8885.
 */
class TabloClient(
    val hostIp: String,
    val port: Int = 8885,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val TAG = "TabloClient"
    val baseUrl = "http://$hostIp:$port"
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val serverInfoAdapter: JsonAdapter<TabloServerInfo> =
        moshi.adapter(TabloServerInfo::class.java)
    private val channelDetailAdapter: JsonAdapter<TabloChannelDetail> =
        moshi.adapter(TabloChannelDetail::class.java)
    private val airingListAdapter: JsonAdapter<List<TabloAiring>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, TabloAiring::class.java))
    private val watchResponseAdapter: JsonAdapter<TabloWatchResponse> =
        moshi.adapter(TabloWatchResponse::class.java)

    // Tracks active watch sessions for tuners so they can be released
    private val activeWatchTokens = ConcurrentHashMap<String, String>() // slotId -> watchToken

    /**
     * GET /server/info
     * Queries the Tablo DVR metadata, server ID, tuner count, and software version.
     */
    suspend fun getServerInfo(): TabloResult<TabloServerInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/server/info")
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext TabloResult.Error("Tablo returned HTTP ${response.code}: ${response.message}")
            }

            val body = response.body?.string()
                ?: return@withContext TabloResult.Error("Empty response body from Tablo /server/info")
            val info = serverInfoAdapter.fromJson(body)
                ?: return@withContext TabloResult.Error("Malformed JSON in Tablo /server/info")

            TabloResult.Success(info)
        } catch (e: IOException) {
            Log.e(TAG, "Connection to Tablo at $hostIp:$port failed: ${e.message}")
            TabloResult.Error("Connection to Tablo at $hostIp:$port failed: ${e.message}", e)
        } catch (e: Exception) {
            TabloResult.Error("Unexpected error querying Tablo: ${e.message}", e)
        }
    }

    /**
     * GET /guide/channels
     * Returns channel paths or channel objects from Tablo DVR.
     */
    suspend fun getChannels(): TabloResult<List<TabloChannelDetail>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/guide/channels")
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext TabloResult.Error("Failed to fetch channels from Tablo: HTTP ${response.code}")
            }

            val body = response.body?.string()
                ?: return@withContext TabloResult.Error("Empty channels response from Tablo")

            val channelsList = mutableListOf<TabloChannelDetail>()

            // Tablo API /guide/channels can return either a list of string paths ["/guide/channels/123", ...]
            // or an array of channel detail JSON objects.
            val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
            val stringListAdapter: JsonAdapter<List<String>> = moshi.adapter(stringListType)

            val paths = try {
                stringListAdapter.fromJson(body)
            } catch (_: Exception) {
                null
            }

            if (!paths.isNullOrEmpty()) {
                // Fetch individual channel details for each path
                for (path in paths) {
                    val detail = getChannelDetail(path)
                    if (detail is TabloResult.Success) {
                        channelsList.add(detail.data)
                    }
                }
            } else {
                // Parse directly as List<TabloChannelDetail>
                val detailListType = Types.newParameterizedType(List::class.java, TabloChannelDetail::class.java)
                val detailListAdapter: JsonAdapter<List<TabloChannelDetail>> = moshi.adapter(detailListType)
                val parsedList = detailListAdapter.fromJson(body)
                if (parsedList != null) {
                    channelsList.addAll(parsedList)
                }
            }

            if (channelsList.isEmpty()) {
                TabloResult.Error("No channels found on Tablo DVR. Please run a channel scan on your Tablo.")
            } else {
                TabloResult.Success(channelsList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Tablo channels: ${e.message}")
            TabloResult.Error("Failed retrieving Tablo channels: ${e.message}", e)
        }
    }

    /**
     * GET /guide/channels/{id}
     * Fetches detailed channel metadata and associated program airings.
     */
    suspend fun getChannelDetail(channelPath: String): TabloResult<TabloChannelDetail> = withContext(Dispatchers.IO) {
        val cleanPath = if (channelPath.startsWith("/")) channelPath else "/$channelPath"
        val fullUrl = "$baseUrl$cleanPath"

        try {
            val request = Request.Builder()
                .url(fullUrl)
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext TabloResult.Error("Failed to fetch channel $channelPath: HTTP ${response.code}")
            }

            val body = response.body?.string()
                ?: return@withContext TabloResult.Error("Empty channel body")
            val detail = channelDetailAdapter.fromJson(body)
                ?: return@withContext TabloResult.Error("Malformed channel JSON")

            // If guide_airings is not directly embedded, attempt fetching airings via /guide/channels/{id}/airings
            val airings = if (!detail.guideAirings.isNullOrEmpty()) {
                detail.guideAirings
            } else {
                fetchChannelAirings(cleanPath)
            }

            val withPathAndAirings = detail.copy(
                path = if (detail.path.isNullOrBlank()) cleanPath else detail.path,
                guideAirings = airings
            )
            TabloResult.Success(withPathAndAirings)
        } catch (e: Exception) {
            TabloResult.Error("Error fetching channel $channelPath: ${e.message}", e)
        }
    }

    /**
     * Attempts to query /guide/channels/{id}/airings if not embedded
     */
    private fun fetchChannelAirings(channelPath: String): List<TabloAiring> {
        return try {
            val airingsUrl = "$baseUrl$channelPath/airings"
            val request = Request.Builder()
                .url(airingsUrl)
                .header("Accept", "application/json")
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    airingListAdapter.fromJson(body) ?: emptyList()
                } else emptyList()
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * POST /guide/channels/{id}/watch or {watchPath}/watch
     * Initiates live streaming playback on Tablo hardware tuner, returning playlist_url and token.
     */
    suspend fun startWatch(
        channelPathOrId: String,
        slotId: String = "slot_default"
    ): TabloResult<TabloWatchResponse> = withContext(Dispatchers.IO) {
        val cleanPath = channelPathOrId.trim()
        val watchEndpoint = when {
            cleanPath.endsWith("/watch") -> cleanPath
            cleanPath.startsWith("/") -> "$cleanPath/watch"
            cleanPath.startsWith("http") -> cleanPath
            else -> "/guide/channels/$cleanPath/watch"
        }

        val fullUrl = if (watchEndpoint.startsWith("http")) watchEndpoint else "$baseUrl$watchEndpoint"
        Log.d(TAG, "Requesting watch stream from Tablo: $fullUrl")

        try {
            val emptyBody = "{}".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(fullUrl)
                .post(emptyBody)
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.code == 409 || response.code == 503) {
                return@withContext TabloResult.Error("All Tablo tuners are in use. Stop an active stream to tune this channel.")
            }
            if (!response.isSuccessful) {
                return@withContext TabloResult.Error("Tablo /watch returned HTTP ${response.code}: ${response.message}")
            }

            val body = response.body?.string()
                ?: return@withContext TabloResult.Error("Empty /watch response from Tablo")
            val watchData = watchResponseAdapter.fromJson(body)
                ?: return@withContext TabloResult.Error("Malformed /watch response JSON")

            if (watchData.playlistUrl.isNullOrBlank()) {
                return@withContext TabloResult.Error("Tablo did not return a valid playlist_url")
            }

            watchData.token?.let { token ->
                activeWatchTokens[slotId] = token
            }

            // Tablo sometimes returns relative path like /stream/pl.m3u8
            val resolvedPlaylist = if (watchData.playlistUrl.startsWith("http")) {
                watchData.playlistUrl
            } else {
                "$baseUrl${if (watchData.playlistUrl.startsWith("/")) "" else "/"}${watchData.playlistUrl}"
            }

            TabloResult.Success(watchData.copy(playlistUrl = resolvedPlaylist))
        } catch (e: Exception) {
            Log.e(TAG, "Watch call failed: ${e.message}")
            TabloResult.Error("Failed starting stream on Tablo: ${e.message}", e)
        }
    }

    /**
     * DELETE /watch/{token}
     * Stops live playback session on Tablo to release the hardware tuner.
     */
    suspend fun stopWatch(slotId: String): Boolean = withContext(Dispatchers.IO) {
        val token = activeWatchTokens.remove(slotId) ?: return@withContext true
        try {
            val request = Request.Builder()
                .url("$baseUrl/watch/$token")
                .delete()
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping watch session for $slotId: ${e.message}")
            false
        }
    }
}
