package com.example.core.network.iptv

import com.example.core.model.Channel
import com.example.core.model.ProgramGuideItem
import com.example.core.model.SourceType
import java.util.UUID

object M3UParser {
    private val TVG_ID_REGEX = """tvg-id="([^"]*)"""".toRegex()
    private val TVG_NAME_REGEX = """tvg-name="([^"]*)"""".toRegex()
    private val TVG_LOGO_REGEX = """tvg-logo="([^"]*)"""".toRegex()
    private val TVG_CHNO_REGEX = """tvg-chno="([^"]*)"""".toRegex()
    private val GROUP_TITLE_REGEX = """group-title="([^"]*)"""".toRegex()

    fun parse(content: String, sourceId: String, sourceName: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentLogo: String? = null
        var currentChNo: String? = null
        var currentGroup: String? = null
        var currentChannelName: String? = null

        var counter = 1
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                currentTvgId = TVG_ID_REGEX.find(trimmed)?.groupValues?.get(1)
                currentTvgName = TVG_NAME_REGEX.find(trimmed)?.groupValues?.get(1)
                currentLogo = TVG_LOGO_REGEX.find(trimmed)?.groupValues?.get(1)
                currentChNo = TVG_CHNO_REGEX.find(trimmed)?.groupValues?.get(1)
                currentGroup = GROUP_TITLE_REGEX.find(trimmed)?.groupValues?.get(1) ?: "General IPTV"

                val commaIndex = trimmed.lastIndexOf(',')
                currentChannelName = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    trimmed.substring(commaIndex + 1).trim()
                } else {
                    currentTvgName ?: "Channel $counter"
                }
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // Stream URL line
                val streamUrl = trimmed
                val name = currentChannelName ?: currentTvgName ?: "IPTV Stream $counter"
                val chNumber = currentChNo ?: "$counter"
                val callSign = (currentTvgId ?: name).take(6).uppercase()

                val now = System.currentTimeMillis()
                val channel = Channel(
                    id = "m3u_${sourceId}_${UUID.nameUUIDFromBytes(streamUrl.toByteArray())}",
                    name = name,
                    channelNumber = chNumber,
                    callSign = callSign,
                    logoUrl = currentLogo,
                    streamUrl = streamUrl,
                    watchPath = null,
                    sourceType = SourceType.M3U,
                    sourceId = sourceId,
                    groupTitle = currentGroup ?: "IPTV Streams",
                    isFavorite = false,
                    currentProgram = ProgramGuideItem(
                        id = "prog_${counter}",
                        channelId = streamUrl,
                        title = "$name Live Stream",
                        description = "Broadcasting continuous live coverage on $sourceName",
                        startTimeEpoch = now,
                        endTimeEpoch = now + 3600_000L,
                        category = currentGroup ?: "Live TV"
                    ),
                    resolution = "1080p"
                )
                channels.add(channel)
                counter++

                // Reset per channel
                currentTvgId = null
                currentTvgName = null
                currentLogo = null
                currentChNo = null
                currentGroup = null
                currentChannelName = null
            }
        }
        return channels
    }
}
