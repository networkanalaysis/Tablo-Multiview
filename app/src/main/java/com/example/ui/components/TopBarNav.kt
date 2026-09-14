package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TabloPurple = Color(0xFF6B4EB2)
private val TabloPurpleActive = Color(0xFF5A4585)
private val NavBg = Color(0xFF14161C)
private val PillContainerBg = Color(0xFF1D2028)
private val TextMuted = Color(0xFF8F93A0)

enum class TabloNavTab(val title: String) {
    SEARCH("Search"),
    HOME("Home"),
    GUIDE("Guide"),
    LIBRARY("Library"),
    LAYOUTS("Layouts")
}

@Composable
fun TopBarNav(
    activeTab: TabloNavTab,
    onTabSelected: (TabloNavTab) -> Unit,
    onSettingsClick: () -> Unit,
    isCreatingLayout: Boolean = false,
    onBackClick: () -> Unit = {},
    onSaveLayoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = NavBg,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFF22252F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Action: Back button if creating layout, otherwise Brand / Logo
            if (isCreatingLayout) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E212B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333640)),
                    modifier = Modifier.size(38.dp)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onTabSelected(TabloNavTab.GUIDE) }
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TabloPurple,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = "Tablo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tablo",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Center: Pill Navigation Bar (Matches Screenshots 1 & 2: Search | Home | Guide | Library | Layouts)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PillContainerBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2E38)),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(3.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabloNavTab.values().forEach { tab ->
                        val isSelected = activeTab == tab
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) TabloPurpleActive else Color.Transparent,
                            modifier = Modifier.clickable { onTabSelected(tab) }
                        ) {
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Right Action: Save pill button if creating layout, otherwise Settings / Filter icons
            if (isCreatingLayout) {
                Button(
                    onClick = onSaveLayoutClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TabloPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Save",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Device",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
