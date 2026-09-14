package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.core.model.MultiviewLayout
import com.example.ui.ActiveScreen
import com.example.ui.MainViewModel
import com.example.ui.auth.TabloSignInScreen
import com.example.ui.components.ChannelPickerDialog
import com.example.ui.components.TabloNavTab
import com.example.ui.components.TabloSettingsDialog
import com.example.ui.components.TopBarNav
import com.example.ui.guide.LiveGuideScreen
import com.example.ui.multiview.CreateLayoutScreen
import com.example.ui.multiview.LayoutsTabScreen
import com.example.ui.multiview.MultiviewScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TabloApp(viewModel)
            }
        }
    }
}

@Composable
fun TabloApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle system back button navigation
    BackHandler(enabled = uiState.currentScreen != ActiveScreen.SIGN_IN && uiState.currentScreen != ActiveScreen.GUIDE) {
        when (uiState.currentScreen) {
            ActiveScreen.CREATE_LAYOUT -> viewModel.navigateTo(ActiveScreen.LAYOUTS)
            ActiveScreen.LAYOUTS -> viewModel.selectNavTab(TabloNavTab.GUIDE)
            ActiveScreen.PLAYER -> viewModel.navigateTo(ActiveScreen.GUIDE)
            else -> viewModel.selectNavTab(TabloNavTab.GUIDE)
        }
    }

    if (uiState.currentScreen == ActiveScreen.SIGN_IN) {
        // App begins with user signing into their Tablo account
        TabloSignInScreen(
            discoveredDevices = uiState.discoveredTablos,
            isDiscovering = uiState.isDiscoveringTablo,
            onRefreshDiscovery = { viewModel.startTabloDiscovery() },
            onSignIn = { email, password, hostIp ->
                viewModel.signInAndConnect(email, password, hostIp)
            },
            isLoading = uiState.isSigningIn,
            errorMessage = uiState.signInError
        )
    } else {
        val isPlayerScreen = uiState.currentScreen == ActiveScreen.PLAYER
        val isCreatingLayout = uiState.currentScreen == ActiveScreen.CREATE_LAYOUT

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // Video player screen has its own top bar with back button & top-right screen counter
                if (!isPlayerScreen) {
                    TopBarNav(
                        activeTab = uiState.activeNavTab,
                        onTabSelected = { tab -> viewModel.selectNavTab(tab) },
                        onSettingsClick = { viewModel.setSettingsDialogOpen(true) },
                        isCreatingLayout = isCreatingLayout,
                        onBackClick = { viewModel.navigateTo(ActiveScreen.LAYOUTS) },
                        onSaveLayoutClick = { /* Triggered in CreateLayoutScreen */ }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF101216))
                    .padding(if (isPlayerScreen) androidx.compose.foundation.layout.PaddingValues() else innerPadding)
            ) {
                when (uiState.currentScreen) {
                    ActiveScreen.SIGN_IN -> {
                        // Handled above
                    }
                    ActiveScreen.GUIDE -> {
                        // Channel guide listing all channels and program guide (Screenshot 1)
                        LiveGuideScreen(
                            channels = uiState.channels,
                            onSelectChannelToWatch = { channel ->
                                viewModel.playChannelInPlayer(channel)
                            },
                            onToggleFavorite = { channel ->
                                viewModel.toggleFavorite(channel)
                            },
                            onRefreshGuide = { viewModel.syncChannels() },
                            isRefreshing = uiState.isSyncingChannels
                        )
                    }
                    ActiveScreen.LAYOUTS -> {
                        LayoutsTabScreen(
                            presets = uiState.presets,
                            onCreateLayoutClick = { viewModel.navigateTo(ActiveScreen.CREATE_LAYOUT) },
                            onSelectPreset = { preset -> viewModel.launchPreset(preset) },
                            onDeletePreset = { preset -> viewModel.deletePreset(preset) },
                            onQuickLayoutSelect = { layout ->
                                viewModel.setMultiviewLayout(layout)
                                viewModel.navigateTo(ActiveScreen.PLAYER)
                            }
                        )
                    }
                    ActiveScreen.CREATE_LAYOUT -> {
                        // Ability to create a multiview preset (Screenshot 2)
                        CreateLayoutScreen(
                            channels = uiState.channels,
                            onSavePreset = { preset, activeChannels ->
                                viewModel.savePresetAndLaunch(preset, activeChannels)
                            },
                            onBackClick = { viewModel.navigateTo(ActiveScreen.LAYOUTS) },
                            onToggleFavorite = { channel -> viewModel.toggleFavorite(channel) }
                        )
                    }
                    ActiveScreen.PLAYER -> {
                        // Video screen with 1, 2, 3, or 4 screens & grey border audio (Screenshot 3)
                        MultiviewScreen(
                            slots = uiState.slots,
                            layout = uiState.multiviewLayout,
                            focusedSlotIndex = uiState.focusedSlotIndex,
                            playerManager = viewModel.playerManager,
                            onSelectSlot = { slotIndex -> viewModel.setFocusedSlot(slotIndex) },
                            onChangeChannelClick = { slotIndex -> viewModel.openChannelPicker(slotIndex) },
                            onSwapSlotsClick = { slotA, slotB -> viewModel.swapSlots(slotA, slotB) },
                            onToggleAudioClick = { slotIndex -> viewModel.setActiveAudioSlot(slotIndex) },
                            onToggleFavorite = { channel -> viewModel.toggleFavorite(channel) },
                            onLayoutChange = { layout -> viewModel.setMultiviewLayout(layout) },
                            onBackClick = { viewModel.navigateTo(ActiveScreen.GUIDE) }
                        )
                    }
                }

                // Channel Picker Dialog when changing channel in player slot
                uiState.channelPickerSlotIndex?.let { targetSlot ->
                    ChannelPickerDialog(
                        slotIndex = targetSlot,
                        channels = uiState.channels,
                        onSelectChannel = { channel ->
                            viewModel.tuneChannel(targetSlot, channel)
                            viewModel.closeChannelPicker()
                        },
                        onDismiss = { viewModel.closeChannelPicker() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }

                // Settings & Account Dialog
                if (uiState.isSettingsDialogOpen) {
                    TabloSettingsDialog(
                        session = uiState.session,
                        channelCount = uiState.channels.size,
                        isSyncing = uiState.isSyncingChannels,
                        onSyncChannels = { viewModel.syncChannels() },
                        onSignOut = { viewModel.signOut() },
                        onDismiss = { viewModel.setSettingsDialogOpen(false) }
                    )
                }
            }
        }
    }
}
