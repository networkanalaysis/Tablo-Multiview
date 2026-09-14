package com.example.core.network.tablo

import com.example.core.model.Channel
import com.example.core.model.ProgramGuideItem
import com.example.core.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TabloRepository(
    val client: TabloClient,
    private val sourceId: String,
    private val sourceName: String
) {
    /**
     * Fetches Tablo channels and converts them into domain Channel items
     */
    suspend fun loadChannels(): TabloResult<List<Channel>> = withContext(Dispatchers.IO) {
        when (val result = client.getChannels()) {
            is TabloResult.Error -> result
            is TabloResult.Success -> {
                val channels = result.data.map { detail ->
                    mapTabloChannelToDomain(detail)
                }
                TabloResult.Success(channels)
            }
        }
    }

    /**
     * Resolves the live watch stream for a Tablo channel
     */
    suspend fun resolveLiveStream(channel: Channel, slotId: String): TabloResult<String> = withContext(Dispatchers.IO) {
        val watchPath = channel.watchPath ?: channel.id
        when (val watchResult = client.startWatch(watchPath, slotId)) {
            is TabloResult.Error -> watchResult
            is TabloResult.Success -> {
                val url = watchResult.data.playlistUrl
                if (!url.isNullOrBlank()) {
                    TabloResult.Success(url)
                } else {
                    TabloResult.Error("No valid playlist_url returned from Tablo")
                }
            }
        }
    }

    suspend fun releaseStream(slotId: String): Boolean {
        return client.stopWatch(slotId)
    }

    private fun mapTabloChannelToDomain(detail: TabloChannelDetail): Channel {
        val meta = detail.channel ?: TabloChannelMeta()
        val path = detail.path ?: "/guide/channels/${detail.objectId ?: 0}"
        val channelNum = meta.displayChannelNumber
        val callSign = meta.callSign ?: (meta.network ?: "CH $channelNum")
        val channelTitle = meta.displayTitle

        val airings = detail.guideAirings ?: emptyList()
        val firstAiring = airings.firstOrNull()
        val secondAiring = airings.getOrNull(1)

        val now = System.currentTimeMillis()
        val guideProgram = firstAiring?.let { airing ->
            val startEpoch = parseAirDate(airing.airDate) ?: now
            val durationMs = (airing.durationSeconds ?: 3600) * 1000L
            ProgramGuideItem(
                id = "prog_${airing.airingId ?: System.currentTimeMillis()}",
                channelId = path,
                title = airing.displayShowTitle,
                description = airing.description ?: "Over-the-air television broadcast",
                startTimeEpoch = startEpoch,
                endTimeEpoch = startEpoch + durationMs,
                category = categorizeShow(airing.displayShowTitle),
                seasonEpisode = airing.episodeTitle
            )
        }

        val upcomingProgram = secondAiring?.let { airing ->
            val firstEnd = guideProgram?.endTimeEpoch ?: (now + 3600_000L)
            val startEpoch = parseAirDate(airing.airDate) ?: firstEnd
            val durationMs = (airing.durationSeconds ?: 3600) * 1000L
            ProgramGuideItem(
                id = "prog_up_${airing.airingId ?: (System.currentTimeMillis() + 1)}",
                channelId = path,
                title = airing.displayShowTitle,
                description = airing.description ?: "Upcoming broadcast",
                startTimeEpoch = startEpoch,
                endTimeEpoch = startEpoch + durationMs,
                category = categorizeShow(airing.displayShowTitle),
                seasonEpisode = airing.episodeTitle
            )
        }

        val logoUrl = when (meta.network?.uppercase()) {
            "CBS" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1b/CBS_logo_%282020%29.svg/320px-CBS_logo_%282020%29.svg.png"
            "NBC" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/NBC_logo_%282022%29.svg/320px-NBC_logo_%282022%29.svg.png"
            "FOX" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/Fox_Broadcasting_Company_Logo.svg/320px-Fox_Broadcasting_Company_Logo.svg.png"
            "ABC" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/2/22/American_Broadcasting_Company_Logo.svg/320px-American_Broadcasting_Company_Logo.svg.png"
            "PBS" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/1/10/PBS_logo_%282019%29.svg/320px-PBS_logo_%282019%29.svg.png"
            "CW" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/2/26/The_CW_logo_2024.svg/320px-The_CW_logo_2024.svg.png"
            else -> null
        }

        return Channel(
            id = "tablo_${sourceId}_${detail.objectId ?: path.hashCode()}",
            name = channelTitle,
            channelNumber = channelNum,
            callSign = callSign,
            logoUrl = logoUrl,
            streamUrl = null, // Dynamically resolved on tune via /watch
            watchPath = path,
            sourceType = SourceType.TABLO,
            sourceId = sourceId,
            groupTitle = "Tablo OTA Broadcast",
            isFavorite = false,
            currentProgram = guideProgram,
            upcomingProgram = upcomingProgram,
            resolution = meta.resolution ?: "1080i"
        )
    }

    private fun categorizeShow(title: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("nfl") || lower.contains("football") || lower.contains("basketball") ||
                    lower.contains("soccer") || lower.contains("baseball") || lower.contains("sports") -> "Sports"
            lower.contains("news") || lower.contains("report") || lower.contains("action") -> "News"
            else -> "Broadcast"
        }
    }

    private fun parseAirDate(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateStr)?.time
        } catch (_: Exception) {
            null
        }
    }
}
