package com.example.core.model

data class ProgramGuideItem(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String,
    val startTimeEpoch: Long,
    val endTimeEpoch: Long,
    val category: String = "General",
    val rating: String? = null,
    val seasonEpisode: String? = null
) {
    val progressPercent: Float
        get() {
            val now = System.currentTimeMillis()
            if (now <= startTimeEpoch) return 0f
            if (now >= endTimeEpoch) return 1f
            val total = endTimeEpoch - startTimeEpoch
            return if (total > 0) (now - startTimeEpoch).toFloat() / total else 0f
        }
}

data class Channel(
    val id: String,
    val name: String,
    val channelNumber: String,
    val callSign: String,
    val logoUrl: String? = null,
    val streamUrl: String? = null,
    val watchPath: String? = null,
    val sourceType: SourceType,
    val sourceId: String,
    val groupTitle: String = "OTA Broadcast",
    val isFavorite: Boolean = false,
    val currentProgram: ProgramGuideItem? = null,
    val upcomingProgram: ProgramGuideItem? = null,
    val resolution: String = "1080i",
    val signalQuality: Int = 100
)

data class MediaSourceConfig(
    val id: String,
    val name: String,
    val type: SourceType,
    val hostOrUrl: String,
    val port: Int? = null,
    val username: String? = null,
    val password: String? = null,
    val apiKey: String? = null,
    val serverDetails: String? = null,
    val isActive: Boolean = true,
    val channelCount: Int = 0
)

data class MultiviewSlot(
    val slotIndex: Int,
    val channel: Channel? = null,
    val playbackUrl: String? = null,
    val isAudioActive: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isMuted: Boolean = false
)

data class MultiviewPreset(
    val id: String,
    val name: String,
    val arrangement: MultiviewLayout,
    val channelIds: List<String> = emptyList(),
    val channelNames: List<String> = emptyList(),
    val createdAtEpoch: Long = System.currentTimeMillis()
)
