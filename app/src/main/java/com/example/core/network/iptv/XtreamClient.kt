package com.example.core.network.iptv

import com.example.core.model.Channel
import com.example.core.model.ProgramGuideItem
import com.example.core.model.SourceType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class XtreamStreamItem(
    @Json(name = "stream_id") val streamId: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "stream_icon") val streamIcon: String? = null,
    @Json(name = "epg_channel_id") val epgChannelId: String? = null,
    @Json(name = "category_name") val categoryName: String? = null,
    @Json(name = "num") val num: Int? = null
)

class XtreamClient(
    private val serverUrl: String,
    private val username: String,
    private val password: String,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    suspend fun getLiveStreams(sourceId: String): List<Channel> = withContext(Dispatchers.IO) {
        val cleanBase = serverUrl.trimEnd('/')
        val apiUrl = "$cleanBase/player_api.php?username=$username&password=$password&action=get_live_streams"

        try {
            val request = Request.Builder().url(apiUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val listType = Types.newParameterizedType(List::class.java, XtreamStreamItem::class.java)
            val adapter = moshi.adapter<List<XtreamStreamItem>>(listType)
            val streams = adapter.fromJson(body) ?: emptyList()

            val now = System.currentTimeMillis()
            streams.mapIndexed { idx, item ->
                val streamId = item.streamId ?: idx
                val streamUrl = "$cleanBase/live/$username/$password/$streamId.m3u8"
                val chNumber = (item.num ?: (idx + 1)).toString()
                val chName = item.name ?: "Stream $streamId"

                Channel(
                    id = "xtream_${sourceId}_$streamId",
                    name = chName,
                    channelNumber = chNumber,
                    callSign = (item.epgChannelId ?: chName).take(6).uppercase(),
                    logoUrl = item.streamIcon,
                    streamUrl = streamUrl,
                    watchPath = null,
                    sourceType = SourceType.XTREAM,
                    sourceId = sourceId,
                    groupTitle = item.categoryName ?: "Xtream Live",
                    isFavorite = false,
                    currentProgram = ProgramGuideItem(
                        id = "prog_xtream_$streamId",
                        channelId = "$streamId",
                        title = "$chName Live",
                        description = "Continuous live stream broadcast",
                        startTimeEpoch = now,
                        endTimeEpoch = now + 3600_000L,
                        category = item.categoryName ?: "Live TV"
                    ),
                    resolution = "1080p"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
