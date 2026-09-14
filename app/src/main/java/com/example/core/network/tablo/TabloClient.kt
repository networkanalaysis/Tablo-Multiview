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

class TabloClient(
    private val hostIp: String,
    private val port: Int = 8885,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val TAG = "TabloClient"
    private val baseUrl = "http://$hostIp:$port"
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val serverInfoAdapter: JsonAdapter<TabloServerInfo> =
        moshi.adapter(TabloServerInfo::class.java)
    private val channelDetailAdapter: JsonAdapter<TabloChannelDetail> =
        moshi.adapter(TabloChannelDetail::class.java)
    private val watchResponseAdapter: JsonAdapter<TabloWatchResponse> =
        moshi.adapter(TabloWatchResponse::class.java)

    private val activeWatchTokens = ConcurrentHashMap<String, String>() // slotIndex -> watchToken

    val isDemo: Boolean = hostIp.contains("demo", ignoreCase = true) || hostIp == "127.0.0.1" || hostIp == "localhost"

    /**
     * GET /server/info
     */
    suspend fun getServerInfo(): TabloResult<TabloServerInfo> = withContext(Dispatchers.IO) {
        if (isDemo) {
            return@withContext TabloResult.Success(
                TabloServerInfo(
                    serverId = "sid_demo_livingroom_quad",
                    name = "Living Room Tablo (OTA Quad)",
                    model = "Tablo QUAD OTA Tuner",
                    tuners = 4,
                    version = "2.2.44",
                    isAvailable = true,
                    localAddress = "192.168.1.120",
                    boardType = "quad",
                    timezone = "America/New_York",
                    setupCompleted = true
                )
            )
        }

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
            Log.e(TAG, "Connection failed: ${e.message}")
            TabloResult.Error("Connection to Tablo at $hostIp:$port failed: ${e.message}", e)
        } catch (e: Exception) {
            TabloResult.Error("Unexpected error querying Tablo: ${e.message}", e)
        }
    }

    /**
     * GET /guide/channels
     * Returns channel paths or channel objects
     */
    suspend fun getChannels(): TabloResult<List<TabloChannelDetail>> = withContext(Dispatchers.IO) {
        if (isDemo) {
            return@withContext TabloResult.Success(getDemoChannels())
        }

        try {
            val request = Request.Builder()
                .url("$baseUrl/guide/channels")
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext TabloResult.Error("Failed to fetch channels: HTTP ${response.code}")
            }

            val body = response.body?.string() ?: return@withContext TabloResult.Error("Empty channels response")

            // Tablo API /guide/channels can return either a list of string paths ["/guide/channels/123", ...]
            // or an array of channel detail JSON objects. We handle both cleanly.
            val channelsList = mutableListOf<TabloChannelDetail>()

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
                // Try parsing directly as List<TabloChannelDetail>
                val detailListType = Types.newParameterizedType(List::class.java, TabloChannelDetail::class.java)
                val detailListAdapter: JsonAdapter<List<TabloChannelDetail>> = moshi.adapter(detailListType)
                val parsedList = detailListAdapter.fromJson(body)
                if (parsedList != null) {
                    channelsList.addAll(parsedList)
                }
            }

            if (channelsList.isEmpty()) {
                TabloResult.Error("No channels found on Tablo. Run a channel scan on the device.")
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

            // Ensure path is populated
            val withPath = if (detail.path.isNullOrBlank()) detail.copy(path = cleanPath) else detail
            TabloResult.Success(withPath)
        } catch (e: Exception) {
            TabloResult.Error("Error fetching $channelPath: ${e.message}", e)
        }
    }

    /**
     * POST /guide/channels/{id}/watch or {watchPath}/watch
     * Initiates live streaming playback on Tablo, returns playlist_url
     */
    suspend fun startWatch(
        channelPathOrId: String,
        slotId: String = "slot_default"
    ): TabloResult<TabloWatchResponse> = withContext(Dispatchers.IO) {
        if (isDemo) {
            val demoStream = getDemoStreamUrl(channelPathOrId)
            return@withContext TabloResult.Success(
                TabloWatchResponse(
                    playlistUrl = demoStream,
                    token = "demo_tok_${System.currentTimeMillis()}",
                    expires = "2026-12-31T23:59:59Z",
                    videoDetails = TabloVideoDetails(width = 1920, height = 1080, tuner = 1)
                )
            )
        }

        val cleanPath = channelPathOrId.trim()
        val watchEndpoint = when {
            cleanPath.endsWith("/watch") -> cleanPath
            cleanPath.startsWith("/") -> "$cleanPath/watch"
            cleanPath.startsWith("http") -> cleanPath
            else -> "/guide/channels/$cleanPath/watch"
        }

        val fullUrl = if (watchEndpoint.startsWith("http")) watchEndpoint else "$baseUrl$watchEndpoint"
        Log.d(TAG, "Requesting watch stream from: $fullUrl")

        try {
            val emptyBody = "{}".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(fullUrl)
                .post(emptyBody)
                .header("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.code == 409 || response.code == 503) {
                return@withContext TabloResult.Error("All Tablo tuners are in use. Stop a stream to tune this channel.")
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

            // Sometimes Tablo returns relative URL like /stream/pl.m3u8
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
     * Stops live playback session on Tablo to release tuner
     */
    suspend fun stopWatch(slotId: String): Boolean = withContext(Dispatchers.IO) {
        val token = activeWatchTokens.remove(slotId) ?: return@withContext true
        if (isDemo) return@withContext true

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

    private fun getDemoStreamUrl(channelPath: String): String {
        val clean = channelPath.lowercase()
        return when {
            clean.endsWith("1001") || clean.contains("cbs") ->
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
            clean.endsWith("1002") || clean.contains("nbc") ->
                "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
            clean.endsWith("1003") || clean.contains("fox") ->
                "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"
            clean.endsWith("1004") || clean.contains("abc") ->
                "https://test-streams.mux.dev/test_001/stream.m3u8"
            clean.endsWith("1005") || clean.contains("pbs") ->
                "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"
            else ->
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        }
    }

    private fun getDemoChannels(): List<TabloChannelDetail> {
        val now = System.currentTimeMillis()
        val hour = 3600 * 1000L

        return listOf(
            TabloChannelDetail(
                objectId = 1001,
                path = "/guide/channels/1001",
                channel = TabloChannelMeta(major = 2, minor = 1, network = "CBS", callSign = "WCBS-HD", resolution = "1080i"),
                guideAirings = listOf(
                    TabloAiring(
                        airingId = 2001,
                        showTitle = "NFL on CBS: Live Game",
                        episodeTitle = "Kansas City Chiefs at Buffalo Bills",
                        description = "AFC clash live in high-definition with Jim Nantz & Tony Romo.",
                        airDate = "2026-09-14T20:00:00Z",
                        durationSeconds = 10800
                    )
                )
            ),
            TabloChannelDetail(
                objectId = 1002,
                path = "/guide/channels/1002",
                channel = TabloChannelMeta(major = 4, minor = 1, network = "NBC", callSign = "WNBC-HD", resolution = "1080i"),
                guideAirings = listOf(
                    TabloAiring(
                        airingId = 2002,
                        showTitle = "Sunday Night Football",
                        episodeTitle = "Philadelphia Eagles vs Dallas Cowboys",
                        description = "NFC East rivalry live in prime time from AT&T Stadium.",
                        airDate = "2026-09-14T20:15:00Z",
                        durationSeconds = 11400
                    )
                )
            ),
            TabloChannelDetail(
                objectId = 1003,
                path = "/guide/channels/1003",
                channel = TabloChannelMeta(major = 5, minor = 1, network = "FOX", callSign = "WNYW-HD", resolution = "720p"),
                guideAirings = listOf(
                    TabloAiring(
                        airingId = 2003,
                        showTitle = "FOX NFL Sunday Live",
                        episodeTitle = "San Francisco 49ers at Green Bay Packers",
                        description = "Live game coverage with Kevin Burkhardt and Tom Brady in the booth.",
                        airDate = "2026-09-14T19:00:00Z",
                        durationSeconds = 10800
                    )
                )
            ),
            TabloChannelDetail(
                objectId = 1004,
                path = "/guide/channels/1004",
                channel = TabloChannelMeta(major = 7, minor = 1, network = "ABC", callSign = "WABC-HD", resolution = "720p"),
                guideAirings = listOf(
                    TabloAiring(
                        airingId = 2004,
                        showTitle = "College Football Countdown",
                        episodeTitle = "Top 25 Matchup",
                        description = "Saturday prime showcase featuring ranked conference powerhouses.",
                        airDate = "2026-09-14T19:30:00Z",
                        durationSeconds = 12000
                    )
                )
            ),
            TabloChannelDetail(
                objectId = 1005,
                path = "/guide/channels/1005",
                channel = TabloChannelMeta(major = 13, minor = 1, network = "PBS", callSign = "WNET-HD", resolution = "1080i"),
                guideAirings = listOf(
                    TabloAiring(
                        airingId = 2005,
                        showTitle = "PBS NewsHour",
                        episodeTitle = "Evening Edition",
                        description = "In-depth analysis of major national and international headlines.",
                        airDate = "2026-09-14T22:00:00Z",
                        durationSeconds = 3600
                    )
                )
            ),
            TabloChannelDetail(
                objectId = 1006,
                path = "/guide/channels/1006",
                channel = TabloChannelMeta(major = 11, minor = 1, network = "CW", callSign = "WPIX-HD", resolution = "1080i"),
                guideAirings = listOf(
                    TabloAiring(
                        airingId = 2006,
                        showTitle = "ACC Basketball Live",
                        episodeTitle = "Duke at North Carolina",
                        description = "Historic college rivalry live from the Dean E. Smith Center.",
                        airDate = "2026-09-14T21:00:00Z",
                        durationSeconds = 7200
                    )
                )
            )
        )
    }
}
