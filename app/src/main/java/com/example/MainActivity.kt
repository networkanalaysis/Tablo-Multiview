package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.core.model.MultiviewLayout
import com.example.core.model.SourceType
import com.example.ui.MainViewModel
import com.example.ui.components.ChannelPickerDialog
import com.example.ui.components.TopBarNav
import com.example.ui.guide.LiveGuideScreen
import com.example.ui.multiview.MultiviewScreen
import com.example.ui.sources.SourcesScreen
import com.example.ui.theme.AerioNavyBg
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AerioTvApp(viewModel)
            }
        }
    }
}

@Composable
fun AerioTvApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show status messages
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissMessage()
        }
    }

    val tabloSources = uiState.sources.filter { it.type == SourceType.TABLO }
    val tabloConnected = tabloSources.any { it.isActive }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBarNav(
                selectedTab = uiState.currentTab,
                onSelectTab = { viewModel.selectNavigationTab(it) },
                currentLayout = uiState.multiviewLayout,
                onSelectLayout = { viewModel.setMultiviewLayout(it) },
                tabloConnected = tabloConnected,
                activeTuners = 4
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AerioNavyBg)
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                0 -> {
                    MultiviewScreen(
                        slots = uiState.slots,
                        layout = uiState.multiviewLayout,
                        focusedSlotIndex = uiState.focusedSlotIndex,
                        playerManager = viewModel.playerManager,
                        onSelectSlot = { viewModel.setFocusedSlot(it) },
                        onChangeChannelClick = { viewModel.openChannelPicker(it) },
                        onSwapSlotsClick = { slotA, slotB -> viewModel.swapSlots(slotA, slotB) },
                        onToggleAudioClick = { viewModel.setActiveAudioSlot(it) },
                        onTuneFocusedClick = { slotIdx ->
                            uiState.slots.getOrNull(slotIdx)?.channel?.let { ch ->
                                viewModel.tuneChannel(slotIdx, ch)
                            }
                        },
                        onTuneAllClick = { viewModel.tuneAllSlots() }
                    )
                }
                1 -> {
                    LiveGuideScreen(
                        channels = uiState.channels,
                        onWatchFullscreen = { channel ->
                            viewModel.setMultiviewLayout(MultiviewLayout.SINGLE)
                            viewModel.tuneChannel(0, channel)
                            viewModel.selectNavigationTab(0)
                        },
                        onWatchInMultiviewSlot = { channel, slotIndex ->
                            viewModel.tuneChannel(slotIndex, channel)
                            viewModel.selectNavigationTab(0)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }
                2 -> {
                    SourcesScreen(
                        sources = uiState.sources,
                        discoveredTablos = uiState.discoveredTablos,
                        isDiscoveringTablo = uiState.isDiscoveringTablo,
                        onStartTabloDiscovery = { viewModel.startTabloDiscovery() },
                        onAddDiscoveredTablo = { viewModel.addDiscoveredTablo(it) },
                        onAddTabloManualIp = { viewModel.addTabloManualIp(it) },
                        onAddDemoTablo = { viewModel.addDemoTablo() },
                        onAddM3uSource = { name, url -> viewModel.addM3uSource(name, url) },
                        onAddXtreamSource = { name, url, u, p -> viewModel.addXtreamSource(name, url, u, p) },
                        onDeleteSource = { viewModel.deleteSource(it) }
                    )
                }
            }

            // Channel Picker Dialog for Slot Tuning
            uiState.channelPickerSlotIndex?.let { targetSlot ->
                ChannelPickerDialog(
                    slotIndex = targetSlot,
                    channels = uiState.channels,
                    onSelectChannel = { ch ->
                        viewModel.tuneChannel(targetSlot, ch)
                        viewModel.closeChannelPicker()
                    },
                    onDismiss = { viewModel.closeChannelPicker() },
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
            }
        }
    }
}
