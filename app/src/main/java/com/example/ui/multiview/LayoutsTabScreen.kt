package com.example.ui.multiview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.MultiviewLayout
import com.example.core.model.MultiviewPreset

private val DarkBg = Color(0xFF101216)
private val CardBg = Color(0xFF191B22)
private val TabloPurple = Color(0xFF6B4EB2)
private val TextMuted = Color(0xFF8F93A0)

@Composable
fun LayoutsTabScreen(
    presets: List<MultiviewPreset>,
    onCreateLayoutClick: () -> Unit,
    onSelectPreset: (MultiviewPreset) -> Unit,
    onDeletePreset: (MultiviewPreset) -> Unit,
    onQuickLayoutSelect: (MultiviewLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Title and Create Layout Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Multiview Layouts",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Create custom multiview arrangements with your favorite Tablo channels",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = onCreateLayoutClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TabloPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Create Layout", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Layout Grid
        Text(
            text = "Quick Screen Modes",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickModeCard(
                title = "1 Screen",
                subtitle = "Single",
                icon = Icons.Default.CropLandscape,
                onClick = { onQuickLayoutSelect(MultiviewLayout.SINGLE) },
                modifier = Modifier.weight(1f)
            )
            QuickModeCard(
                title = "2 Screens",
                subtitle = "Dual Split",
                icon = Icons.Default.ViewSidebar,
                onClick = { onQuickLayoutSelect(MultiviewLayout.DUAL_SPLIT) },
                modifier = Modifier.weight(1f)
            )
            QuickModeCard(
                title = "3 Screens",
                subtitle = "Triple",
                icon = Icons.Default.ViewQuilt,
                onClick = { onQuickLayoutSelect(MultiviewLayout.TRIPLE_FOCUS) },
                modifier = Modifier.weight(1f)
            )
            QuickModeCard(
                title = "4 Screens",
                subtitle = "Quad Grid",
                icon = Icons.Default.GridView,
                onClick = { onQuickLayoutSelect(MultiviewLayout.QUAD_GRID) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Saved Layout Presets List
        Text(
            text = "Saved Presets",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (presets.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2E38)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No saved presets yet",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Create Layout' above to build a multi-stream preset with your channels.",
                        color = TextMuted,
                        fontSize = 12.sp
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
                items(presets, key = { it.id }) { preset ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2E38)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPreset(preset) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TabloPurple,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (preset.arrangement) {
                                            MultiviewLayout.SINGLE -> Icons.Default.CropLandscape
                                            MultiviewLayout.DUAL_SPLIT -> Icons.Default.ViewSidebar
                                            MultiviewLayout.TRIPLE_FOCUS -> Icons.Default.ViewQuilt
                                            MultiviewLayout.QUAD_GRID -> Icons.Default.GridView
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${preset.arrangement.displayName} • ${preset.channelNames.joinToString(", ").ifBlank { "${preset.arrangement.maxSlots} streams" }}",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { onDeletePreset(preset) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete preset",
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2E38)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TabloPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
