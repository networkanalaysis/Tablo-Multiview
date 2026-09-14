package com.example.ui.multiview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Channel
import com.example.core.model.MultiviewLayout
import com.example.core.model.MultiviewSlot
import com.example.core.player.ExoPlayerSlotManager
import com.example.ui.components.VideoPlayerSlotView

private val PlayerDarkBg = Color(0xFF0C0D10)
private val TabloPurple = Color(0xFF6B4EB2)
private val CardBg = Color(0xFF191B22)

@Composable
fun MultiviewScreen(
    slots: List<MultiviewSlot>,
    layout: MultiviewLayout,
    focusedSlotIndex: Int,
    playerManager: ExoPlayerSlotManager,
    onSelectSlot: (Int) -> Unit,
    onChangeChannelClick: (Int) -> Unit,
    onSwapSlotsClick: (Int, Int) -> Unit,
    onToggleAudioClick: (Int) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onLayoutChange: (MultiviewLayout) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isScreenMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PlayerDarkBg)
    ) {
        // Top Bar: Back button on left, Screen count changer on top right (Matches Screenshot 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button (Circular)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333640)),
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Top Right Corner: Add or change screens (1, 2, 3, or 4 screens)
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333640)),
                    modifier = Modifier.clickable { isScreenMenuOpen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (layout) {
                                MultiviewLayout.SINGLE -> Icons.Default.CropLandscape
                                MultiviewLayout.DUAL_SPLIT -> Icons.Default.ViewSidebar
                                MultiviewLayout.TRIPLE_FOCUS -> Icons.Default.ViewQuilt
                                MultiviewLayout.QUAD_GRID -> Icons.Default.GridView
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when (layout) {
                                MultiviewLayout.SINGLE -> "1 Screen"
                                MultiviewLayout.DUAL_SPLIT -> "2 Screens"
                                MultiviewLayout.TRIPLE_FOCUS -> "3 Screens"
                                MultiviewLayout.QUAD_GRID -> "4 Screens"
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "▾",
                            color = Color(0xFF8F93A0),
                            fontSize = 12.sp
                        )
                    }
                }

                DropdownMenu(
                    expanded = isScreenMenuOpen,
                    onDismissRequest = { isScreenMenuOpen = false },
                    modifier = Modifier.background(CardBg)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CropLandscape,
                                    contentDescription = null,
                                    tint = if (layout == MultiviewLayout.SINGLE) TabloPurple else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "1 Screen (Single)",
                                    color = if (layout == MultiviewLayout.SINGLE) TabloPurple else Color.White,
                                    fontWeight = if (layout == MultiviewLayout.SINGLE) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            onLayoutChange(MultiviewLayout.SINGLE)
                            isScreenMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ViewSidebar,
                                    contentDescription = null,
                                    tint = if (layout == MultiviewLayout.DUAL_SPLIT) TabloPurple else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "2 Screens (Dual Split)",
                                    color = if (layout == MultiviewLayout.DUAL_SPLIT) TabloPurple else Color.White,
                                    fontWeight = if (layout == MultiviewLayout.DUAL_SPLIT) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            onLayoutChange(MultiviewLayout.DUAL_SPLIT)
                            isScreenMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ViewQuilt,
                                    contentDescription = null,
                                    tint = if (layout == MultiviewLayout.TRIPLE_FOCUS) TabloPurple else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "3 Screens (Triple Focus)",
                                    color = if (layout == MultiviewLayout.TRIPLE_FOCUS) TabloPurple else Color.White,
                                    fontWeight = if (layout == MultiviewLayout.TRIPLE_FOCUS) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            onLayoutChange(MultiviewLayout.TRIPLE_FOCUS)
                            isScreenMenuOpen = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = if (layout == MultiviewLayout.QUAD_GRID) TabloPurple else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "4 Screens (Quad Grid)",
                                    color = if (layout == MultiviewLayout.QUAD_GRID) TabloPurple else Color.White,
                                    fontWeight = if (layout == MultiviewLayout.QUAD_GRID) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = {
                            onLayoutChange(MultiviewLayout.QUAD_GRID)
                            isScreenMenuOpen = false
                        }
                    )
                }
            }
        }

        // Video Player Display Area (1, 2, 3, or 4 screens)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            when (layout) {
                MultiviewLayout.SINGLE -> {
                    val slot = slots.getOrNull(0) ?: MultiviewSlot(slotIndex = 0)
                    VideoPlayerSlotView(
                        slot = slot,
                        player = playerManager.getOrCreatePlayer(0),
                        isFocused = focusedSlotIndex == 0,
                        onSlotClick = {
                            onSelectSlot(0)
                            onToggleAudioClick(0)
                        },
                        onChangeChannelClick = { onChangeChannelClick(0) },
                        onSwapClick = { onSwapSlotsClick(0, 1) },
                        onToggleAudioClick = { onToggleAudioClick(0) },
                        onToggleFavorite = { slot.channel?.let { onToggleFavorite(it) } },
                        onFullscreenClick = { },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MultiviewLayout.DUAL_SPLIT -> {
                    // 2 SCREENS MODE:
                    // "When there are 2 screens, allow for the user to move which video uses audio but that should ONLY be denoted with a greay border."
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val slot0 = slots.getOrNull(0) ?: MultiviewSlot(slotIndex = 0)
                        val slot1 = slots.getOrNull(1) ?: MultiviewSlot(slotIndex = 1)

                        VideoPlayerSlotView(
                            slot = slot0,
                            player = playerManager.getOrCreatePlayer(0),
                            isFocused = focusedSlotIndex == 0,
                            onSlotClick = {
                                onSelectSlot(0)
                                onToggleAudioClick(0)
                            },
                            onChangeChannelClick = { onChangeChannelClick(0) },
                            onSwapClick = { onSwapSlotsClick(0, 1) },
                            onToggleAudioClick = { onToggleAudioClick(0) },
                            onToggleFavorite = { slot0.channel?.let { onToggleFavorite(it) } },
                            onFullscreenClick = { onLayoutChange(MultiviewLayout.SINGLE) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        VideoPlayerSlotView(
                            slot = slot1,
                            player = playerManager.getOrCreatePlayer(1),
                            isFocused = focusedSlotIndex == 1,
                            onSlotClick = {
                                onSelectSlot(1)
                                onToggleAudioClick(1)
                            },
                            onChangeChannelClick = { onChangeChannelClick(1) },
                            onSwapClick = { onSwapSlotsClick(1, 0) },
                            onToggleAudioClick = { onToggleAudioClick(1) },
                            onToggleFavorite = { slot1.channel?.let { onToggleFavorite(it) } },
                            onFullscreenClick = { onLayoutChange(MultiviewLayout.SINGLE) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
                MultiviewLayout.TRIPLE_FOCUS -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val slot0 = slots.getOrNull(0) ?: MultiviewSlot(slotIndex = 0)
                        val slot1 = slots.getOrNull(1) ?: MultiviewSlot(slotIndex = 1)
                        val slot2 = slots.getOrNull(2) ?: MultiviewSlot(slotIndex = 2)

                        // Main Big Screen (Slot 0)
                        VideoPlayerSlotView(
                            slot = slot0,
                            player = playerManager.getOrCreatePlayer(0),
                            isFocused = focusedSlotIndex == 0,
                            onSlotClick = {
                                onSelectSlot(0)
                                onToggleAudioClick(0)
                            },
                            onChangeChannelClick = { onChangeChannelClick(0) },
                            onSwapClick = { onSwapSlotsClick(0, 1) },
                            onToggleAudioClick = { onToggleAudioClick(0) },
                            onToggleFavorite = { slot0.channel?.let { onToggleFavorite(it) } },
                            onFullscreenClick = { onLayoutChange(MultiviewLayout.SINGLE) },
                            modifier = Modifier
                                .weight(1.8f)
                                .fillMaxHeight()
                        )

                        // 2 Smaller Stacked Screens (Slots 1 and 2)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VideoPlayerSlotView(
                                slot = slot1,
                                player = playerManager.getOrCreatePlayer(1),
                                isFocused = focusedSlotIndex == 1,
                                onSlotClick = {
                                    onSelectSlot(1)
                                    onToggleAudioClick(1)
                                },
                                onChangeChannelClick = { onChangeChannelClick(1) },
                                onSwapClick = { onSwapSlotsClick(1, 0) },
                                onToggleAudioClick = { onToggleAudioClick(1) },
                                onToggleFavorite = { slot1.channel?.let { onToggleFavorite(it) } },
                                onFullscreenClick = { onLayoutChange(MultiviewLayout.SINGLE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )

                            VideoPlayerSlotView(
                                slot = slot2,
                                player = playerManager.getOrCreatePlayer(2),
                                isFocused = focusedSlotIndex == 2,
                                onSlotClick = {
                                    onSelectSlot(2)
                                    onToggleAudioClick(2)
                                },
                                onChangeChannelClick = { onChangeChannelClick(2) },
                                onSwapClick = { onSwapSlotsClick(2, 0) },
                                onToggleAudioClick = { onToggleAudioClick(2) },
                                onToggleFavorite = { slot2.channel?.let { onToggleFavorite(it) } },
                                onFullscreenClick = { onLayoutChange(MultiviewLayout.SINGLE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
                MultiviewLayout.QUAD_GRID -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Row (Slots 0 and 1)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val slot0 = slots.getOrNull(0) ?: MultiviewSlot(slotIndex = 0)
                            val slot1 = slots.getOrNull(1) ?: MultiviewSlot(slotIndex = 1)

                            VideoPlayerSlotView(
                                slot = slot0,
                                player = playerManager.getOrCreatePlayer(0),
                                isFocused = focusedSlotIndex == 0,
                                onSlotClick = {
                                    onSelectSlot(0)
                                    onToggleAudioClick(0)
                                },
                                onChangeChannelClick = { onChangeChannelClick(0) },
                                onSwapClick = { onSwapSlotsClick(0, 1) },
                                onToggleAudioClick = { onToggleAudioClick(0) },
                                onToggleFavorite = { slot0.channel?.let { onToggleFavorite(it) } },
                                onFullscreenClick = { onLayoutChange(MultiviewLayout.SINGLE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )

                            VideoPlayerSlotView(
                                slot = slot1,
                                player = playerManager.getOrCreatePlayer(1),
                                isFocused = focusedSlotIndex == 1,
                                onSlotClick = {
                                    onSelectSlot(1)
                                    onToggleAudioClick(1)
                                },
                                onChangeChannelClick = { onChangeChannelClick(1) },
                                onSwapClick = { onSwapSlotsClick(1, 0) },
                                onToggleAudioClick = { onToggleAudioClick(1) },
                                onToggleFavorite = { slot1.channel?.let { onToggleFavorite(it) } },
                                onFullscreenClick = { onLayoutChange(MultiviewLayout.SINGLE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }

                        // Bottom Row (Slots 2 and 3)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val slot2 = slots.getOrNull(2) ?: MultiviewSlot(slotIndex = 2)
                            val slot3 = slots.getOrNull(3) ?: MultiviewSlot(slotIndex = 3)

                            VideoPlayerSlotView(
                                slot = slot2,
                                player = playerManager.getOrCreatePlayer(2),
                                isFocused = focusedSlotIndex == 2,
                                onSlotClick = {
                                    onSelectSlot(2)
                                    onToggleAudioClick(2)
                                },
                                onChangeChannelClick = { onChangeChannelClick(2) },
                                onSwapClick = { onSwapSlotsClick(2, 3) },
                                onToggleAudioClick = { onToggleAudioClick(2) },
                                onToggleFavorite = { slot2.channel?.let { onToggleFavorite(it) } },
                                onFullscreenClick = { onLayoutChange(MultiviewLayout.SINGLE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )

                            VideoPlayerSlotView(
                                slot = slot3,
                                player = playerManager.getOrCreatePlayer(3),
                                isFocused = focusedSlotIndex == 3,
                                onSlotClick = {
                                    onSelectSlot(3)
                                    onToggleAudioClick(3)
                                },
                                onChangeChannelClick = { onChangeChannelClick(3) },
                                onSwapClick = { onSwapSlotsClick(3, 2) },
                                onToggleAudioClick = { onToggleAudioClick(3) },
                                onToggleFavorite = { slot3.channel?.let { onToggleFavorite(it) } },
                                onFullscreenClick = { onLayoutChange(MultiviewLayout.SINGLE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}
