package com.example.ui.multiview

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.AerioAudioActive
import com.example.ui.theme.AerioCyan
import com.example.ui.theme.AerioNavyBg

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
    onTuneFocusedClick: (Int) -> Unit = {},
    onTuneAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AerioNavyBg)
            .padding(8.dp)
    ) {
        // Video Stage Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (layout) {
                MultiviewLayout.QUAD_GRID -> {
                    QuadGridLayout(
                        slots = slots,
                        focusedSlotIndex = focusedSlotIndex,
                        playerManager = playerManager,
                        onSelectSlot = onSelectSlot,
                        onChangeChannelClick = onChangeChannelClick,
                        onSwapSlotsClick = onSwapSlotsClick,
                        onToggleAudioClick = onToggleAudioClick
                    )
                }
                MultiviewLayout.DUAL_SPLIT -> {
                    DualSplitLayout(
                        slots = slots,
                        focusedSlotIndex = focusedSlotIndex,
                        playerManager = playerManager,
                        onSelectSlot = onSelectSlot,
                        onChangeChannelClick = onChangeChannelClick,
                        onSwapSlotsClick = onSwapSlotsClick,
                        onToggleAudioClick = onToggleAudioClick
                    )
                }
                MultiviewLayout.TRIPLE_FOCUS -> {
                    TripleFocusLayout(
                        slots = slots,
                        focusedSlotIndex = focusedSlotIndex,
                        playerManager = playerManager,
                        onSelectSlot = onSelectSlot,
                        onChangeChannelClick = onChangeChannelClick,
                        onSwapSlotsClick = onSwapSlotsClick,
                        onToggleAudioClick = onToggleAudioClick
                    )
                }
                MultiviewLayout.SINGLE -> {
                    val slot = slots.getOrNull(0) ?: MultiviewSlot(slotIndex = 0)
                    VideoPlayerSlotView(
                        slot = slot,
                        player = playerManager.getOrCreatePlayer(0),
                        isFocused = focusedSlotIndex == 0,
                        onSlotClick = { onSelectSlot(0) },
                        onChangeChannelClick = { onChangeChannelClick(0) },
                        onSwapClick = { onSwapSlotsClick(0, 1) },
                        onToggleAudioClick = { onToggleAudioClick(0) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Remote Navigation & Audio status bar
        MultiviewBottomBar(
            focusedSlot = slots.getOrNull(focusedSlotIndex),
            totalSlots = layout.maxSlots,
            canTuneAll = slots.take(layout.maxSlots).any { it.channel != null && it.playbackUrl == null },
            onChangeChannel = { onChangeChannelClick(focusedSlotIndex) },
            onSwapNext = {
                val nextSlot = (focusedSlotIndex + 1) % layout.maxSlots
                onSwapSlotsClick(focusedSlotIndex, nextSlot)
            },
            onTuneFocused = { onTuneFocusedClick(focusedSlotIndex) },
            onTuneAll = onTuneAllClick
        )
    }
}

@Composable
private fun QuadGridLayout(
    slots: List<MultiviewSlot>,
    focusedSlotIndex: Int,
    playerManager: ExoPlayerSlotManager,
    onSelectSlot: (Int) -> Unit,
    onChangeChannelClick: (Int) -> Unit,
    onSwapSlotsClick: (Int, Int) -> Unit,
    onToggleAudioClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Slot 0 & Slot 1
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val slot0 = slots.getOrNull(0) ?: MultiviewSlot(slotIndex = 0)
            val slot1 = slots.getOrNull(1) ?: MultiviewSlot(slotIndex = 1)

            VideoPlayerSlotView(
                slot = slot0,
                player = playerManager.getOrCreatePlayer(0),
                isFocused = focusedSlotIndex == 0,
                onSlotClick = { onSelectSlot(0) },
                onChangeChannelClick = { onChangeChannelClick(0) },
                onSwapClick = { onSwapSlotsClick(0, 1) },
                onToggleAudioClick = { onToggleAudioClick(0) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            VideoPlayerSlotView(
                slot = slot1,
                player = playerManager.getOrCreatePlayer(1),
                isFocused = focusedSlotIndex == 1,
                onSlotClick = { onSelectSlot(1) },
                onChangeChannelClick = { onChangeChannelClick(1) },
                onSwapClick = { onSwapSlotsClick(1, 0) },
                onToggleAudioClick = { onToggleAudioClick(1) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Row 2: Slot 2 & Slot 3
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val slot2 = slots.getOrNull(2) ?: MultiviewSlot(slotIndex = 2)
            val slot3 = slots.getOrNull(3) ?: MultiviewSlot(slotIndex = 3)

            VideoPlayerSlotView(
                slot = slot2,
                player = playerManager.getOrCreatePlayer(2),
                isFocused = focusedSlotIndex == 2,
                onSlotClick = { onSelectSlot(2) },
                onChangeChannelClick = { onChangeChannelClick(2) },
                onSwapClick = { onSwapSlotsClick(2, 3) },
                onToggleAudioClick = { onToggleAudioClick(2) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            VideoPlayerSlotView(
                slot = slot3,
                player = playerManager.getOrCreatePlayer(3),
                isFocused = focusedSlotIndex == 3,
                onSlotClick = { onSelectSlot(3) },
                onChangeChannelClick = { onChangeChannelClick(3) },
                onSwapClick = { onSwapSlotsClick(3, 2) },
                onToggleAudioClick = { onToggleAudioClick(3) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun DualSplitLayout(
    slots: List<MultiviewSlot>,
    focusedSlotIndex: Int,
    playerManager: ExoPlayerSlotManager,
    onSelectSlot: (Int) -> Unit,
    onChangeChannelClick: (Int) -> Unit,
    onSwapSlotsClick: (Int, Int) -> Unit,
    onToggleAudioClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val slot0 = slots.getOrNull(0) ?: MultiviewSlot(slotIndex = 0)
        val slot1 = slots.getOrNull(1) ?: MultiviewSlot(slotIndex = 1)

        VideoPlayerSlotView(
            slot = slot0,
            player = playerManager.getOrCreatePlayer(0),
            isFocused = focusedSlotIndex == 0,
            onSlotClick = { onSelectSlot(0) },
            onChangeChannelClick = { onChangeChannelClick(0) },
            onSwapClick = { onSwapSlotsClick(0, 1) },
            onToggleAudioClick = { onToggleAudioClick(0) },
            modifier = Modifier.weight(1f).fillMaxHeight()
        )

        VideoPlayerSlotView(
            slot = slot1,
            player = playerManager.getOrCreatePlayer(1),
            isFocused = focusedSlotIndex == 1,
            onSlotClick = { onSelectSlot(1) },
            onChangeChannelClick = { onChangeChannelClick(1) },
            onSwapClick = { onSwapSlotsClick(1, 0) },
            onToggleAudioClick = { onToggleAudioClick(1) },
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

@Composable
private fun TripleFocusLayout(
    slots: List<MultiviewSlot>,
    focusedSlotIndex: Int,
    playerManager: ExoPlayerSlotManager,
    onSelectSlot: (Int) -> Unit,
    onChangeChannelClick: (Int) -> Unit,
    onSwapSlotsClick: (Int, Int) -> Unit,
    onToggleAudioClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Main 65% focus screen
        val slot0 = slots.getOrNull(0) ?: MultiviewSlot(slotIndex = 0)
        VideoPlayerSlotView(
            slot = slot0,
            player = playerManager.getOrCreatePlayer(0),
            isFocused = focusedSlotIndex == 0,
            onSlotClick = { onSelectSlot(0) },
            onChangeChannelClick = { onChangeChannelClick(0) },
            onSwapClick = { onSwapSlotsClick(0, 1) },
            onToggleAudioClick = { onToggleAudioClick(0) },
            modifier = Modifier.weight(1.8f).fillMaxHeight()
        )

        // Right 35% column with 2 stacked screens
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val slot1 = slots.getOrNull(1) ?: MultiviewSlot(slotIndex = 1)
            val slot2 = slots.getOrNull(2) ?: MultiviewSlot(slotIndex = 2)

            VideoPlayerSlotView(
                slot = slot1,
                player = playerManager.getOrCreatePlayer(1),
                isFocused = focusedSlotIndex == 1,
                onSlotClick = { onSelectSlot(1) },
                onChangeChannelClick = { onChangeChannelClick(1) },
                onSwapClick = { onSwapSlotsClick(1, 0) },
                onToggleAudioClick = { onToggleAudioClick(1) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )

            VideoPlayerSlotView(
                slot = slot2,
                player = playerManager.getOrCreatePlayer(2),
                isFocused = focusedSlotIndex == 2,
                onSlotClick = { onSelectSlot(2) },
                onChangeChannelClick = { onChangeChannelClick(2) },
                onSwapClick = { onSwapSlotsClick(2, 0) },
                onToggleAudioClick = { onToggleAudioClick(2) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MultiviewBottomBar(
    focusedSlot: MultiviewSlot?,
    totalSlots: Int,
    canTuneAll: Boolean = false,
    onChangeChannel: () -> Unit,
    onSwapNext: () -> Unit,
    onTuneFocused: () -> Unit = {},
    onTuneAll: () -> Unit = {}
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Focused slot information
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = AerioAudioActive,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Audio Active",
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AUDIO: SLOT ${(focusedSlot?.slotIndex ?: 0) + 1}",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                focusedSlot?.channel?.let { ch ->
                    Text(
                        text = "${ch.channelNumber} ${ch.callSign}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    ch.currentProgram?.let { prog ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "— ${prog.title}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                } ?: run {
                    Text(
                        text = "No channel tuned",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }

            // Remote guide hints & actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (focusedSlot?.channel != null && focusedSlot.playbackUrl == null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AerioCyan,
                        modifier = Modifier.clickable(onClick = onTuneFocused)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Stream Live",
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Stream Live",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (canTuneAll) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AerioCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable(onClick = onTuneAll)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Stream All",
                                tint = AerioCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Stream All",
                                color = AerioCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.clickable(onClick = onChangeChannel)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = "Change Channel",
                            tint = AerioCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Change Channel",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.clickable(onClick = onSwapNext)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Swap Position",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "D-Pad: Focus & Audio",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
