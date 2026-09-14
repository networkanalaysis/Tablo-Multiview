package com.example.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Channel
import com.example.core.model.ProgramGuideItem
import com.example.core.model.SourceType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GuideDarkBg = Color(0xFF101216)
private val ChannelCardBg = Color(0xFF1B1D23)
private val ProgramSlotBg = Color(0xFF242730)
private val TabloPurple = Color(0xFF5A4585)
private val TabloPurpleActive = Color(0xFF6B4EB2)
private val TextMuted = Color(0xFF8F93A0)

@Composable
fun LiveGuideScreen(
    channels: List<Channel>,
    onSelectChannelToWatch: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onRefreshGuide: () -> Unit = {},
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Antenna") }

    val filteredChannels = channels.filter { ch ->
        val matchesCategory = when (selectedFilter) {
            "All" -> true
            "Favorites" -> ch.isFavorite
            "Antenna" -> ch.sourceType == SourceType.TABLO
            "Streaming TV" -> ch.sourceType != SourceType.TABLO
            else -> true
        }

        val matchesSearch = if (searchQuery.isBlank()) true else {
            ch.name.contains(searchQuery, ignoreCase = true) ||
            ch.callSign.contains(searchQuery, ignoreCase = true) ||
            ch.channelNumber.contains(searchQuery, ignoreCase = true) ||
            ch.currentProgram?.title?.contains(searchQuery, ignoreCase = true) == true ||
            ch.upcomingProgram?.title?.contains(searchQuery, ignoreCase = true) == true
        }

        matchesCategory && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GuideDarkBg)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Title & Search Row
        Text(
            text = "All Channels",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Search Input field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search channels and programs...",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ChannelCardBg,
                unfocusedContainerColor = ChannelCardBg,
                focusedBorderColor = TabloPurpleActive,
                unfocusedBorderColor = Color(0xFF2E313A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Pills Row (Matches Screenshot 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterPill(
                title = "All",
                icon = Icons.Default.GridOn,
                isSelected = selectedFilter == "All",
                onClick = { selectedFilter = "All" }
            )
            FilterPill(
                title = "Favorites",
                icon = Icons.Default.Star,
                isSelected = selectedFilter == "Favorites",
                onClick = { selectedFilter = "Favorites" }
            )
            FilterPill(
                title = "Antenna",
                icon = Icons.Default.CellTower,
                isSelected = selectedFilter == "Antenna",
                onClick = { selectedFilter = "Antenna" }
            )
            FilterPill(
                title = "Streaming TV",
                icon = Icons.Default.LiveTv,
                isSelected = selectedFilter == "Streaming TV",
                onClick = { selectedFilter = "Streaming TV" }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = TabloPurpleActive,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = onRefreshGuide,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Guide",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Channels Guide List
        if (filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (channels.isEmpty()) "No channels synced from Tablo DVR yet." else "No channels match your search.",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (channels.isEmpty()) "Run a channel scan on your Tablo DVR or tap refresh." else "Try clearing your search query or filters.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    ChannelGuideRow(
                        channel = channel,
                        onChannelClick = { onSelectChannelToWatch(channel) },
                        onToggleFavorite = { onToggleFavorite(channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) TabloPurpleActive else ChannelCardBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) TabloPurpleActive else Color(0xFF2E313A)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                color = if (isSelected) Color.White else TextMuted,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Channel Guide Row matching Screenshot 1:
 * Left side: Star + Channel Badge (Network, Channel number, Callsign)
 * Right side: Program schedule cards (Current program, Next program, or No program data available)
 */
@Composable
private fun ChannelGuideRow(
    channel: Channel,
    onChannelClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ChannelCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF252830)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onChannelClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Star Icon (Favorite toggle)
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorite",
                    tint = if (channel.isFavorite) Color(0xFFFFC107) else Color(0xFF757575),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Channel Badge (Dark rounded container with Network & Channel number)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF14161B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E313A)),
                modifier = Modifier
                    .width(110.dp)
                    .height(72.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = channel.callSign.ifBlank { channel.name },
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = channel.channelNumber,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = channel.name.take(12),
                        color = TextMuted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Program Guide Horizontal Schedule
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val current = channel.currentProgram
                val upcoming = channel.upcomingProgram

                if (current != null) {
                    ProgramScheduleSlotCard(
                        program = current,
                        isCurrent = true,
                        modifier = Modifier.width(260.dp)
                    )
                }

                if (upcoming != null) {
                    ProgramScheduleSlotCard(
                        program = upcoming,
                        isCurrent = false,
                        modifier = Modifier.width(260.dp)
                    )
                }

                if (current == null && upcoming == null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ProgramSlotBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C303A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "No program data available",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramScheduleSlotCard(
    program: ProgramGuideItem,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val startStr = timeFormat.format(Date(program.startTimeEpoch))
    val endStr = timeFormat.format(Date(program.endTimeEpoch))

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ProgramSlotBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrent) TabloPurple.copy(alpha = 0.5f) else Color(0xFF2C303A)
        ),
        modifier = modifier.height(72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = program.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "$startStr - $endStr",
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1
            )

            if (program.description.isNotBlank()) {
                Text(
                    text = program.description,
                    color = Color(0xFFB0B4C0),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
