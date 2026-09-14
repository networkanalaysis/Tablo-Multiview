package com.example.ui.multiview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Channel
import com.example.core.model.MultiviewLayout
import com.example.core.model.MultiviewPreset
import com.example.ui.components.ChannelPickerDialog
import java.util.UUID

private val DarkBg = Color(0xFF101216)
private val CardBg = Color(0xFF191B22)
private val SlotBg = Color(0xFF14161C)
private val TabloPurple = Color(0xFF6B4EB2)
private val TabloPurpleBorder = Color(0xFF7E57C2)
private val TextMuted = Color(0xFF8F93A0)

@Composable
fun CreateLayoutScreen(
    channels: List<Channel>,
    onSavePreset: (MultiviewPreset, List<Channel?>) -> Unit,
    onBackClick: () -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    var layoutName by remember { mutableStateOf("") }
    var selectedArrangement by remember { mutableStateOf(MultiviewLayout.QUAD_GRID) }
    var isArrangementDropdownOpen by remember { mutableStateOf(false) }

    // Slots holds selected Channel for each slot
    val selectedChannels = remember {
        mutableStateListOf<Channel?>(null, null, null, null)
    }

    var pickerSlotIndex by remember { mutableStateOf<Int?>(null) }

    // Ensure slot list size matches layout maxSlots
    val activeSlotsCount = selectedArrangement.maxSlots

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Content Area (Scrollable)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Create Layout",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "If left blank, the arrangement name will be used.",
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Layout Name Input
            OutlinedTextField(
                value = layoutName,
                onValueChange = { layoutName = it },
                placeholder = {
                    Text(text = "Layout Name (Optional)", color = TextMuted, fontSize = 14.sp)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg,
                    focusedBorderColor = TabloPurple,
                    unfocusedBorderColor = Color(0xFF2B2E38),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Arrangement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Arrangement",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Arrangement Selector Card (Matches Screenshot 2)
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2E38)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isArrangementDropdownOpen = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Arrangement Icon
                        Icon(
                            imageVector = getArrangementIcon(selectedArrangement),
                            contentDescription = null,
                            tint = TabloPurpleBorder,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedArrangement.displayName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedArrangement.maxSlots} streams",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = "Select arrangement",
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isArrangementDropdownOpen,
                    onDismissRequest = { isArrangementDropdownOpen = false },
                    modifier = Modifier.background(CardBg)
                ) {
                    MultiviewLayout.values().forEach { layout ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getArrangementIcon(layout),
                                        contentDescription = null,
                                        tint = if (selectedArrangement == layout) TabloPurple else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = layout.displayName,
                                            color = if (selectedArrangement == layout) TabloPurpleBorder else Color.White,
                                            fontWeight = if (selectedArrangement == layout) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "${layout.maxSlots} streams",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedArrangement = layout
                                isArrangementDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Channels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Channels",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of Slots (Matches Screenshot 2: dark box with purple border, (+) circle icon)
            SlotsGridPreview(
                layout = selectedArrangement,
                selectedChannels = selectedChannels,
                onSlotClick = { slotIdx -> pickerSlotIndex = slotIdx },
                onClearSlot = { slotIdx -> selectedChannels[slotIdx] = null }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Save Preset Button (Also available at bottom for touch ergonomics)
            Button(
                onClick = {
                    val finalName = layoutName.ifBlank { selectedArrangement.displayName }
                    val activeChannels = (0 until activeSlotsCount).map { selectedChannels.getOrNull(it) }
                    val preset = MultiviewPreset(
                        id = "preset_${UUID.randomUUID()}",
                        name = finalName,
                        arrangement = selectedArrangement,
                        channelIds = activeChannels.mapNotNull { it?.id },
                        channelNames = activeChannels.mapNotNull { it?.name }
                    )
                    onSavePreset(preset, activeChannels)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TabloPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Save & Launch Layout",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Channel Picker Dialog
    pickerSlotIndex?.let { slotIdx ->
        ChannelPickerDialog(
            slotIndex = slotIdx,
            channels = channels,
            onSelectChannel = { channel ->
                selectedChannels[slotIdx] = channel
                pickerSlotIndex = null
            },
            onDismiss = { pickerSlotIndex = null },
            onToggleFavorite = onToggleFavorite
        )
    }
}

@Composable
private fun SlotsGridPreview(
    layout: MultiviewLayout,
    selectedChannels: List<Channel?>,
    onSlotClick: (Int) -> Unit,
    onClearSlot: (Int) -> Unit
) {
    when (layout) {
        MultiviewLayout.SINGLE -> {
            PresetSlotCard(
                slotIndex = 0,
                channel = selectedChannels.getOrNull(0),
                onClick = { onSlotClick(0) },
                onClear = { onClearSlot(0) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        }
        MultiviewLayout.DUAL_SPLIT -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PresetSlotCard(
                    slotIndex = 0,
                    channel = selectedChannels.getOrNull(0),
                    onClick = { onSlotClick(0) },
                    onClear = { onClearSlot(0) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                PresetSlotCard(
                    slotIndex = 1,
                    channel = selectedChannels.getOrNull(1),
                    onClick = { onSlotClick(1) },
                    onClear = { onClearSlot(1) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
        MultiviewLayout.TRIPLE_FOCUS -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PresetSlotCard(
                    slotIndex = 0,
                    channel = selectedChannels.getOrNull(0),
                    onClick = { onSlotClick(0) },
                    onClear = { onClearSlot(0) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PresetSlotCard(
                        slotIndex = 1,
                        channel = selectedChannels.getOrNull(1),
                        onClick = { onSlotClick(1) },
                        onClear = { onClearSlot(1) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    PresetSlotCard(
                        slotIndex = 2,
                        channel = selectedChannels.getOrNull(2),
                        onClick = { onSlotClick(2) },
                        onClear = { onClearSlot(2) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
        MultiviewLayout.QUAD_GRID -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PresetSlotCard(
                        slotIndex = 0,
                        channel = selectedChannels.getOrNull(0),
                        onClick = { onSlotClick(0) },
                        onClear = { onClearSlot(0) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    PresetSlotCard(
                        slotIndex = 1,
                        channel = selectedChannels.getOrNull(1),
                        onClick = { onSlotClick(1) },
                        onClear = { onClearSlot(1) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PresetSlotCard(
                        slotIndex = 2,
                        channel = selectedChannels.getOrNull(2),
                        onClick = { onSlotClick(2) },
                        onClear = { onClearSlot(2) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    PresetSlotCard(
                        slotIndex = 3,
                        channel = selectedChannels.getOrNull(3),
                        onClick = { onSlotClick(3) },
                        onClear = { onClearSlot(3) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

/**
 * Individual Slot Card matching Screenshot 2:
 * Empty state: purple outline, center (+) purple circle with "Tap to select channel"
 * Selected state: channel badge and name
 */
@Composable
private fun PresetSlotCard(
    slotIndex: Int,
    channel: Channel?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SlotBg,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = if (channel != null) TabloPurple else TabloPurpleBorder.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        if (channel == null) {
            // Empty state (Matches Screenshot 2)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = TabloPurple,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Channel",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tap to select channel",
                    color = Color(0xFFB0B4C0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Selected channel display
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E212A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TabloPurple),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${channel.channelNumber} • ${channel.callSign.ifBlank { channel.name }}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    channel.currentProgram?.let { prog ->
                        Text(
                            text = prog.title,
                            color = TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun getArrangementIcon(layout: MultiviewLayout): ImageVector {
    return when (layout) {
        MultiviewLayout.SINGLE -> Icons.Default.CropLandscape
        MultiviewLayout.DUAL_SPLIT -> Icons.Default.ViewSidebar
        MultiviewLayout.TRIPLE_FOCUS -> Icons.Default.ViewQuilt
        MultiviewLayout.QUAD_GRID -> Icons.Default.GridView
    }
}
