package com.example.ui.components

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.core.model.Channel
import com.example.core.model.MultiviewSlot

private val GreyAudioBorder = Color(0xFF9E9E9E)
private val TabloPurple = Color(0xFF6B4EB2)

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerSlotView(
    slot: MultiviewSlot,
    player: ExoPlayer,
    isFocused: Boolean,
    onSlotClick: () -> Unit,
    onChangeChannelClick: () -> Unit,
    onSwapClick: () -> Unit,
    onToggleAudioClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onFullscreenClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // CRITICAL USER REQUIREMENT:
    // "When there are 2 screens, allow for the user to move which video uses audio but that should ONLY be denoted with a greay border."
    val hasAudio = slot.isAudioActive

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .then(
                if (hasAudio) {
                    Modifier.border(2.5.dp, GreyAudioBorder, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onSlotClick)
            .focusable()
            .onFocusChanged {
                if (it.isFocused) {
                    onSlotClick()
                }
            }
    ) {
        // ExoPlayer View
        if (slot.channel != null && slot.playbackUrl != null) {
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(
                        R.layout.view_multiview_player,
                        null,
                        false
                    ) as PlayerView).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player != player) {
                        view.player = player
                    }
                },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (slot.channel != null) {
            // Slot has an assigned channel, ready to tune
            SlotChannelPreviewCard(
                slot = slot,
                channel = slot.channel,
                isFocused = isFocused,
                onPlayClick = onSlotClick,
                onChangeChannelClick = onChangeChannelClick
            )
        } else {
            // Empty slot state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF14161E)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E212B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TabloPurple),
                        modifier = Modifier.size(52.dp)
                    ) {
                        IconButton(onClick = onChangeChannelClick) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Channel",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "Screen ${slot.slotIndex + 1}: Select Channel",
                        color = Color(0xFF8F93A0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Loading spinner
        if (slot.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Tuning Tablo OTA...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Error Banner
        if (slot.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = slot.errorMessage,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = Color(0xFF1E212B),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable { onChangeChannelClick() }
                    ) {
                        Text(
                            text = "Change Channel",
                            color = TabloPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Slot Overlay Controls (Matches Screenshot 3 bottom-right controls: Star, Swap, Fullscreen, Audio)
        if (slot.channel != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333640))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Star (Favorite)
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (slot.channel.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Favorite",
                                tint = if (slot.channel.isFavorite) Color(0xFFFFC107) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Swap Channel
                        IconButton(
                            onClick = onChangeChannelClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Change Channel",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Fullscreen
                        IconButton(
                            onClick = onFullscreenClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Audio Toggle / Indicator
                        IconButton(
                            onClick = onToggleAudioClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (hasAudio) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = if (hasAudio) "Audio Active" else "Audio Muted",
                                tint = if (hasAudio) Color.White else Color(0xFF8F93A0),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Top-left channel badge overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333640))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${slot.channel.channelNumber} ${slot.channel.callSign.ifBlank { slot.channel.name }}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview Card shown before channel begins streaming.
 */
@Composable
fun SlotChannelPreviewCard(
    slot: MultiviewSlot,
    channel: Channel,
    isFocused: Boolean,
    onPlayClick: () -> Unit,
    onChangeChannelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF14161F))
            .clickable(onClick = onPlayClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1F222E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF343846)),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${channel.channelNumber} • ${channel.callSign.ifBlank { channel.name }}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Text(
                text = channel.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            channel.currentProgram?.let { prog ->
                Text(
                    text = prog.title,
                    color = Color(0xFF8F93A0),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = TabloPurple,
                modifier = Modifier.clickable(onClick = onPlayClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Stream Live",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Stream Live",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
