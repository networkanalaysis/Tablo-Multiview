package com.example.core.network.tablo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TabloServerInfo(
    @Json(name = "server_id") val serverId: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "tuners") val tuners: Int = 2,
    @Json(name = "version") val version: String? = null,
    @Json(name = "is_available") val isAvailable: Boolean = true,
    @Json(name = "local_address") val localAddress: String? = null,
    @Json(name = "board_type") val boardType: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "setup_completed") val setupCompleted: Boolean = true
)

@JsonClass(generateAdapter = true)
data class TabloChannelDetail(
    @Json(name = "object_id") val objectId: Long? = null,
    @Json(name = "path") val path: String? = null,
    @Json(name = "channel") val channel: TabloChannelMeta? = null,
    @Json(name = "guide_airings") val guideAirings: List<TabloAiring>? = null
)

@JsonClass(generateAdapter = true)
data class TabloChannelMeta(
    @Json(name = "major") val major: Int = 1,
    @Json(name = "minor") val minor: Int = 1,
    @Json(name = "network") val network: String? = null,
    @Json(name = "call_sign") val callSign: String? = null,
    @Json(name = "resolution") val resolution: String? = "1080i"
) {
    val displayChannelNumber: String
        get() = if (minor > 0) "$major.$minor" else "$major"

    val displayTitle: String
        get() = when {
            !network.isNullOrBlank() && !callSign.isNullOrBlank() -> "$network ($callSign)"
            !network.isNullOrBlank() -> network
            !callSign.isNullOrBlank() -> callSign
            else -> "Channel $displayChannelNumber"
        }
}

@JsonClass(generateAdapter = true)
data class TabloAiring(
    @Json(name = "airing_id") val airingId: Long? = null,
    @Json(name = "show_title") val showTitle: String? = null,
    @Json(name = "episode_title") val episodeTitle: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "air_date") val airDate: String? = null,
    @Json(name = "duration") val durationSeconds: Int? = null,
    @Json(name = "snapshot_image") val snapshotImage: String? = null
)

@JsonClass(generateAdapter = true)
data class TabloWatchResponse(
    @Json(name = "playlist_url") val playlistUrl: String? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "expires") val expires: String? = null,
    @Json(name = "video_details") val videoDetails: TabloVideoDetails? = null
)

@JsonClass(generateAdapter = true)
data class TabloVideoDetails(
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null,
    @Json(name = "tuner") val tuner: Int? = null
)

@JsonClass(generateAdapter = true)
data class TabloAssocCpe(
    @Json(name = "serverid") val serverId: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "private_ip") val privateIp: String? = null,
    @Json(name = "public_ip") val publicIp: String? = null,
    @Json(name = "slip") val slip: String? = null,
    @Json(name = "board") val board: String? = null,
    @Json(name = "tuners") val tuners: Int? = null
)

@JsonClass(generateAdapter = true)
data class TabloAssocResponse(
    @Json(name = "cpes") val cpes: List<TabloAssocCpe>? = null,
    @Json(name = "success") val success: Boolean = true
)

data class TabloDiscoveredDevice(
    val ipAddress: String,
    val serverId: String,
    val name: String,
    val model: String,
    val tunerCount: Int,
    val version: String,
    val discoveryMethod: String,
    val isAvailable: Boolean = true
)
