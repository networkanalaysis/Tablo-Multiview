package com.example.core.model

enum class SourceType(val displayName: String, val badgeColorHex: Long) {
    TABLO("Tablo OTA", 0xFF00E5FF),
    M3U("IPTV M3U", 0xFF818CF8),
    XTREAM("Xtream Codes", 0xFF34D399),
    DISPATCHARR("Dispatcharr", 0xFFF59E0B)
}

enum class MultiviewLayout(val displayName: String, val maxSlots: Int, val iconName: String) {
    SINGLE("Single Screen", 1, "fullscreen"),
    DUAL_SPLIT("2-Screen Split", 2, "splitscreen_vertical"),
    TRIPLE_FOCUS("1 Big + 2 Small", 3, "view_quilt"),
    QUAD_GRID("Sunday Ticket 4-Way", 4, "grid_view")
}
