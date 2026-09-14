package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.MultiviewLayout
import com.example.ui.theme.AerioCardDark
import com.example.ui.theme.AerioCyan
import com.example.ui.theme.AerioNeonGreen

@Composable
fun TopBarNav(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    currentLayout: MultiviewLayout,
    onSelectLayout: (MultiviewLayout) -> Unit,
    tabloConnected: Boolean,
    activeTuners: Int = 4,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AerioCardDark,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand & Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AerioCyan,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = "AerioTV",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AerioTV",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AerioCyan.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "TABLO OTA",
                        color = AerioCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Main Nav Tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabItem(
                    label = "Multiview",
                    icon = Icons.Default.GridView,
                    isSelected = selectedTab == 0,
                    onClick = { onSelectTab(0) }
                )
                NavTabItem(
                    label = "Live Guide",
                    icon = Icons.Default.LiveTv,
                    isSelected = selectedTab == 1,
                    onClick = { onSelectTab(1) }
                )
                NavTabItem(
                    label = "Tablo & Sources",
                    icon = Icons.Default.Router,
                    isSelected = selectedTab == 2,
                    onClick = { onSelectTab(2) }
                )
            }

            // Right side: Layout Switcher (if on Multiview tab) or Tuner indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedTab == 0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF070B14))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        LayoutSwitchButton(
                            icon = Icons.Default.GridView,
                            tooltip = "Quad 2x2",
                            isSelected = currentLayout == MultiviewLayout.QUAD_GRID,
                            onClick = { onSelectLayout(MultiviewLayout.QUAD_GRID) }
                        )
                        LayoutSwitchButton(
                            icon = Icons.Default.ViewSidebar,
                            tooltip = "Dual Split",
                            isSelected = currentLayout == MultiviewLayout.DUAL_SPLIT,
                            onClick = { onSelectLayout(MultiviewLayout.DUAL_SPLIT) }
                        )
                        LayoutSwitchButton(
                            icon = Icons.Default.ViewQuilt,
                            tooltip = "1+2 Focus",
                            isSelected = currentLayout == MultiviewLayout.TRIPLE_FOCUS,
                            onClick = { onSelectLayout(MultiviewLayout.TRIPLE_FOCUS) }
                        )
                        LayoutSwitchButton(
                            icon = Icons.Default.CropLandscape,
                            tooltip = "Single Fullscreen",
                            isSelected = currentLayout == MultiviewLayout.SINGLE,
                            onClick = { onSelectLayout(MultiviewLayout.SINGLE) }
                        )
                    }
                } else {
                    // Tablo Tuners status pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (tabloConnected) AerioNeonGreen else Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (tabloConnected) "Tablo Online ($activeTuners Tuners)" else "Tablo Ready",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) Color(0xFF1E293B) else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AerioCyan.copy(alpha = 0.6f)) else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) AerioCyan else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LayoutSwitchButton(
    icon: ImageVector,
    tooltip: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) AerioCyan else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .size(30.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = if (isSelected) Color.Black else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
