package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.AerioRepository
import com.example.core.model.Channel
import com.example.core.model.MediaSourceConfig
import com.example.core.model.MultiviewLayout
import com.example.core.model.MultiviewPreset
import com.example.core.model.MultiviewSlot
import com.example.core.network.tablo.TabloAuthManager
import com.example.core.network.tablo.TabloDiscoveredDevice
import com.example.core.network.tablo.TabloUserSession
import com.example.core.player.ExoPlayerSlotManager
import com.example.ui.components.TabloNavTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ActiveScreen {
    SIGN_IN,
    GUIDE,
    LAYOUTS,
    CREATE_LAYOUT,
    PLAYER
}

data class AerioUiState(
    val channels: List<Channel> = emptyList(),
    val sources: List<MediaSourceConfig> = emptyList(),
    val presets: List<MultiviewPreset> = emptyList(),
    val session: TabloUserSession = TabloUserSession(),
    val discoveredTablos: List<TabloDiscoveredDevice> = emptyList(),
    val isDiscoveringTablo: Boolean = false,
    val isSigningIn: Boolean = false,
    val signInError: String? = null,
    val isSyncingChannels: Boolean = false,
    val isSettingsDialogOpen: Boolean = false,
    val currentScreen: ActiveScreen = ActiveScreen.GUIDE,
    val activeNavTab: TabloNavTab = TabloNavTab.GUIDE,
    val multiviewLayout: MultiviewLayout = MultiviewLayout.DUAL_SPLIT,
    val slots: List<MultiviewSlot> = (0..3).map {
        MultiviewSlot(slotIndex = it, isAudioActive = (it == 0))
    },
    val focusedSlotIndex: Int = 0,
    val activeAudioSlotIndex: Int = 0,
    val channelPickerSlotIndex: Int? = null,
    val statusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = AerioRepository(application)
    val authManager = TabloAuthManager(application)
    val playerManager = ExoPlayerSlotManager(application)

    private val _uiState = MutableStateFlow(AerioUiState())
    val uiState: StateFlow<AerioUiState> = _uiState.asStateFlow()

    init {
        // Collect session state
        viewModelScope.launch {
            authManager.session.collect { sess ->
                _uiState.value = _uiState.value.copy(
                    session = sess,
                    currentScreen = if (sess.isLoggedIn) ActiveScreen.GUIDE else ActiveScreen.SIGN_IN
                )
            }
        }

        // Cleanup any legacy demo test data
        viewModelScope.launch {
            repository.cleanupLegacyTestData()
        }

        // Collect channels from Room
        viewModelScope.launch {
            repository.allChannels.collect { chList ->
                _uiState.value = _uiState.value.copy(channels = chList)
                populateDefaultSlotsIfEmpty(chList)
            }
        }

        // Collect sources from Room
        viewModelScope.launch {
            repository.allSources.collect { srcList ->
                _uiState.value = _uiState.value.copy(sources = srcList)
            }
        }

        // Collect presets from Room
        viewModelScope.launch {
            repository.allPresets.collect { presetList ->
                _uiState.value = _uiState.value.copy(presets = presetList)
            }
        }

        // Keep player status in sync with UI slots
        viewModelScope.launch {
            playerManager.statusMap.collect { statusMap ->
                val currentSlots = _uiState.value.slots.map { slot ->
                    val status = statusMap[slot.slotIndex]
                    if (status != null) {
                        slot.copy(
                            isLoading = status.isBuffering,
                            errorMessage = if (status.hasError) status.errorMessage else null,
                            isAudioActive = status.isAudioActive
                        )
                    } else {
                        slot
                    }
                }
                _uiState.value = _uiState.value.copy(slots = currentSlots)
            }
        }

        // Start background discovery for local Tablo devices
        startTabloDiscovery()
    }

    private fun populateDefaultSlotsIfEmpty(channels: List<Channel>) {
        val currentSlots = _uiState.value.slots
        val hasAnyChannel = currentSlots.any { it.channel != null }
        if (!hasAnyChannel && channels.isNotEmpty()) {
            val newSlots = currentSlots.mapIndexed { idx, slot ->
                val ch = channels.getOrNull(idx)
                slot.copy(channel = ch, playbackUrl = null, isAudioActive = (idx == 0))
            }
            _uiState.value = _uiState.value.copy(slots = newSlots)
        }
    }

    fun signInAndConnect(email: String, password: String, hostIp: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSigningIn = true, signInError = null)
            val result = repository.addTabloSource(ipAddress = hostIp)
            result.onSuccess { config ->
                authManager.saveSession(
                    email = email,
                    hostIp = config.hostOrUrl,
                    serverId = config.id,
                    deviceName = config.name,
                    tuners = 4
                )
                _uiState.value = _uiState.value.copy(
                    isSigningIn = false,
                    signInError = null,
                    currentScreen = ActiveScreen.GUIDE,
                    activeNavTab = TabloNavTab.GUIDE
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSigningIn = false,
                    signInError = error.message ?: "Failed to connect to Tablo at $hostIp. Verify device is online."
                )
            }
        }
    }

    fun signOut() {
        authManager.signOut()
        viewModelScope.launch {
            playerManager.releaseAll()
            _uiState.value = _uiState.value.copy(
                currentScreen = ActiveScreen.SIGN_IN,
                isSettingsDialogOpen = false
            )
        }
    }

    fun syncChannels() {
        val host = _uiState.value.session.tabloHostIp
        if (host.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncingChannels = true)
            repository.addTabloSource(ipAddress = host)
            _uiState.value = _uiState.value.copy(isSyncingChannels = false)
        }
    }

    fun startTabloDiscovery() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDiscoveringTablo = true)
            val devices = repository.discoverTablos()
            _uiState.value = _uiState.value.copy(
                isDiscoveringTablo = false,
                discoveredTablos = devices
            )
        }
    }

    fun navigateTo(screen: ActiveScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun selectNavTab(tab: TabloNavTab) {
        when (tab) {
            TabloNavTab.GUIDE -> {
                _uiState.value = _uiState.value.copy(
                    activeNavTab = tab,
                    currentScreen = ActiveScreen.GUIDE
                )
            }
            TabloNavTab.LAYOUTS -> {
                _uiState.value = _uiState.value.copy(
                    activeNavTab = tab,
                    currentScreen = ActiveScreen.LAYOUTS
                )
            }
            TabloNavTab.HOME, TabloNavTab.SEARCH, TabloNavTab.LIBRARY -> {
                _uiState.value = _uiState.value.copy(
                    activeNavTab = tab,
                    currentScreen = ActiveScreen.GUIDE
                )
            }
        }
    }

    fun setMultiviewLayout(layout: MultiviewLayout) {
        _uiState.value = _uiState.value.copy(multiviewLayout = layout)
        if (_uiState.value.focusedSlotIndex >= layout.maxSlots) {
            setFocusedSlot(0)
        }
        if (_uiState.value.activeAudioSlotIndex >= layout.maxSlots) {
            setActiveAudioSlot(0)
        }
    }

    fun setFocusedSlot(slotIndex: Int) {
        _uiState.value = _uiState.value.copy(focusedSlotIndex = slotIndex)
        setActiveAudioSlot(slotIndex)

        val slot = _uiState.value.slots.getOrNull(slotIndex)
        if (slot?.channel != null && slot.playbackUrl == null && !slot.isLoading) {
            tuneChannel(slotIndex, slot.channel)
        }
    }

    fun setActiveAudioSlot(slotIndex: Int) {
        _uiState.value = _uiState.value.copy(activeAudioSlotIndex = slotIndex)
        playerManager.setActiveAudioSlot(slotIndex)
        val updatedSlots = _uiState.value.slots.map { slot ->
            slot.copy(isAudioActive = (slot.slotIndex == slotIndex))
        }
        _uiState.value = _uiState.value.copy(slots = updatedSlots)
    }

    fun playChannelInPlayer(channel: Channel) {
        // Switch to Player screen with Single layout
        setMultiviewLayout(MultiviewLayout.SINGLE)
        _uiState.value = _uiState.value.copy(currentScreen = ActiveScreen.PLAYER)
        tuneChannel(0, channel)
    }

    fun tuneChannel(slotIndex: Int, channel: Channel) {
        viewModelScope.launch {
            val updatedSlots = _uiState.value.slots.map { slot ->
                if (slot.slotIndex == slotIndex) {
                    slot.copy(channel = channel, isLoading = true, errorMessage = null, playbackUrl = null)
                } else slot
            }
            _uiState.value = _uiState.value.copy(slots = updatedSlots)

            val slotId = "slot_$slotIndex"
            val result = repository.resolveStreamUrl(channel, slotId)
            result.onSuccess { streamUrl ->
                val readySlots = _uiState.value.slots.map { slot ->
                    if (slot.slotIndex == slotIndex) {
                        slot.copy(playbackUrl = streamUrl, isLoading = false, errorMessage = null)
                    } else slot
                }
                _uiState.value = _uiState.value.copy(slots = readySlots)
                playerManager.playStream(slotIndex, streamUrl)
            }.onFailure { error ->
                val errorSlots = _uiState.value.slots.map { slot ->
                    if (slot.slotIndex == slotIndex) {
                        slot.copy(isLoading = false, errorMessage = error.message ?: "Failed to tune channel")
                    } else slot
                }
                _uiState.value = _uiState.value.copy(slots = errorSlots)
            }
        }
    }

    fun swapSlots(indexA: Int, indexB: Int) {
        if (indexA == indexB) return
        val slots = _uiState.value.slots.toMutableList()
        val slotA = slots.getOrNull(indexA) ?: return
        val slotB = slots.getOrNull(indexB) ?: return

        slots[indexA] = slotA.copy(channel = slotB.channel, playbackUrl = slotB.playbackUrl)
        slots[indexB] = slotB.copy(channel = slotA.channel, playbackUrl = slotA.playbackUrl)
        _uiState.value = _uiState.value.copy(slots = slots)

        slots[indexA].channel?.let { tuneChannel(indexA, it) }
        slots[indexB].channel?.let { tuneChannel(indexB, it) }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, !channel.isFavorite)
        }
    }

    fun savePresetAndLaunch(preset: MultiviewPreset, channels: List<Channel?>) {
        viewModelScope.launch {
            repository.savePreset(preset)
            setMultiviewLayout(preset.arrangement)

            // Tune selected channels into slots
            val currentSlots = _uiState.value.slots.toMutableList()
            channels.forEachIndexed { idx, ch ->
                if (idx < currentSlots.size) {
                    currentSlots[idx] = currentSlots[idx].copy(channel = ch, playbackUrl = null)
                }
            }
            _uiState.value = _uiState.value.copy(
                slots = currentSlots,
                currentScreen = ActiveScreen.PLAYER
            )

            // Start tuning active slots
            channels.forEachIndexed { idx, ch ->
                if (ch != null && idx < preset.arrangement.maxSlots) {
                    delay(idx * 300L)
                    tuneChannel(idx, ch)
                }
            }
        }
    }

    fun launchPreset(preset: MultiviewPreset) {
        viewModelScope.launch {
            setMultiviewLayout(preset.arrangement)
            val channelMap = _uiState.value.channels.associateBy { it.id }
            val currentSlots = _uiState.value.slots.toMutableList()

            preset.channelIds.forEachIndexed { idx, chId ->
                if (idx < currentSlots.size) {
                    val ch = channelMap[chId]
                    currentSlots[idx] = currentSlots[idx].copy(channel = ch, playbackUrl = null)
                }
            }

            _uiState.value = _uiState.value.copy(
                slots = currentSlots,
                currentScreen = ActiveScreen.PLAYER
            )

            preset.channelIds.forEachIndexed { idx, chId ->
                val ch = channelMap[chId]
                if (ch != null && idx < preset.arrangement.maxSlots) {
                    delay(idx * 300L)
                    tuneChannel(idx, ch)
                }
            }
        }
    }

    fun deletePreset(preset: MultiviewPreset) {
        viewModelScope.launch {
            repository.deletePreset(preset.id)
        }
    }

    fun setSettingsDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isSettingsDialogOpen = isOpen)
    }

    fun openChannelPicker(slotIndex: Int) {
        _uiState.value = _uiState.value.copy(channelPickerSlotIndex = slotIndex)
    }

    fun closeChannelPicker() {
        _uiState.value = _uiState.value.copy(channelPickerSlotIndex = null)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.releaseAll()
    }
}
