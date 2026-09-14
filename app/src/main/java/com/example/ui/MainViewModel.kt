package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.AerioRepository
import com.example.core.model.Channel
import com.example.core.model.MediaSourceConfig
import com.example.core.model.MultiviewLayout
import com.example.core.model.MultiviewSlot
import com.example.core.model.SourceType
import com.example.core.network.tablo.TabloDiscoveredDevice
import com.example.core.player.ExoPlayerSlotManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AerioUiState(
    val channels: List<Channel> = emptyList(),
    val sources: List<MediaSourceConfig> = emptyList(),
    val discoveredTablos: List<TabloDiscoveredDevice> = emptyList(),
    val isDiscoveringTablo: Boolean = false,
    val multiviewLayout: MultiviewLayout = MultiviewLayout.QUAD_GRID,
    val slots: List<MultiviewSlot> = (0..3).map {
        MultiviewSlot(slotIndex = it, isAudioActive = (it == 0))
    },
    val focusedSlotIndex: Int = 0,
    val activeAudioSlotIndex: Int = 0,
    val channelPickerSlotIndex: Int? = null,
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val statusMessage: String? = null,
    val currentTab: Int = 0 // 0: Multiview, 1: Live Guide, 2: Tablo & Sources
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = AerioRepository(application)
    val playerManager = ExoPlayerSlotManager(application)

    private val _uiState = MutableStateFlow(AerioUiState())
    val uiState: StateFlow<AerioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfEmpty()
        }

        // Collect channels from Room
        viewModelScope.launch {
            repository.allChannels.collect { chList ->
                _uiState.value = _uiState.value.copy(channels = chList)
                // Initialize default multiview slots if slots are unassigned
                populateDefaultSlotsIfEmpty(chList)
            }
        }

        // Collect sources from Room
        viewModelScope.launch {
            repository.allSources.collect { srcList ->
                _uiState.value = _uiState.value.copy(sources = srcList)
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
    }

    private fun populateDefaultSlotsIfEmpty(channels: List<Channel>) {
        val currentSlots = _uiState.value.slots
        val hasAnyChannel = currentSlots.any { it.channel != null }
        if (!hasAnyChannel && channels.isNotEmpty()) {
            val tabloChannels = channels.filter { it.sourceType == SourceType.TABLO }
            val candidateList = if (tabloChannels.size >= 4) tabloChannels else channels
            val newSlots = currentSlots.mapIndexed { idx, slot ->
                val ch = candidateList.getOrNull(idx)
                slot.copy(channel = ch, playbackUrl = null, isAudioActive = (idx == 0))
            }
            _uiState.value = _uiState.value.copy(slots = newSlots)
        }
    }

    fun setMultiviewLayout(layout: MultiviewLayout) {
        _uiState.value = _uiState.value.copy(multiviewLayout = layout)
        // Ensure focused and audio slots are valid within the layout
        if (_uiState.value.focusedSlotIndex >= layout.maxSlots) {
            setFocusedSlot(0)
        }
        if (_uiState.value.activeAudioSlotIndex >= layout.maxSlots) {
            setActiveAudioSlot(0)
        }
    }

    fun setFocusedSlot(slotIndex: Int) {
        _uiState.value = _uiState.value.copy(focusedSlotIndex = slotIndex)
        // In Sunday Ticket style, focusing a slot also selects its audio
        setActiveAudioSlot(slotIndex)

        // If slot is ready with channel but not yet streaming, tune it upon user selection
        val slot = _uiState.value.slots.getOrNull(slotIndex)
        if (slot?.channel != null && slot.playbackUrl == null && !slot.isLoading) {
            tuneChannel(slotIndex, slot.channel)
        }
    }

    fun tuneAllSlots() {
        val visibleSlots = _uiState.value.slots.take(_uiState.value.multiviewLayout.maxSlots)
        visibleSlots.forEachIndexed { index, slot ->
            slot.channel?.let { ch ->
                if (slot.playbackUrl == null) {
                    viewModelScope.launch {
                        delay(index * 300L)
                        tuneChannel(slot.slotIndex, ch)
                    }
                }
            }
        }
    }

    fun setActiveAudioSlot(slotIndex: Int) {
        _uiState.value = _uiState.value.copy(activeAudioSlotIndex = slotIndex)
        playerManager.setActiveAudioSlot(slotIndex)
    }

    fun tuneChannel(slotIndex: Int, channel: Channel) {
        viewModelScope.launch {
            // Update slot state to loading
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
                        slot.copy(isLoading = false, errorMessage = error.message ?: "Failed to load channel")
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

        // Retune players
        slots[indexA].channel?.let { tuneChannel(indexA, it) }
        slots[indexB].channel?.let { tuneChannel(indexB, it) }
    }

    fun openChannelPicker(slotIndex: Int) {
        _uiState.value = _uiState.value.copy(channelPickerSlotIndex = slotIndex)
    }

    fun closeChannelPicker() {
        _uiState.value = _uiState.value.copy(channelPickerSlotIndex = null)
    }

    fun selectNavigationTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(currentTab = tabIndex)
    }

    fun setCategoryFilter(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, !channel.isFavorite)
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
            if (devices.isEmpty()) {
                showMessage("No Tablo devices found via UDP/Cloud. Try entering manual IP or Add Demo Tablo.")
            } else {
                showMessage("Discovered ${devices.size} Tablo device(s) on your network.")
            }
        }
    }

    fun addDiscoveredTablo(device: TabloDiscoveredDevice) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDiscoveringTablo = true)
            val result = repository.addTabloSource(
                ipAddress = device.ipAddress,
                customName = device.name,
                tuners = device.tunerCount,
                model = device.model
            )
            _uiState.value = _uiState.value.copy(isDiscoveringTablo = false)
            result.onSuccess {
                showMessage("Connected to Tablo: ${device.name} (${device.tunerCount} Tuners)")
            }.onFailure {
                showMessage("Error adding Tablo: ${it.message}")
            }
        }
    }

    fun addTabloManualIp(ip: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDiscoveringTablo = true)
            val verified = repository.verifyTabloIp(ip)
            val result = repository.addTabloSource(
                ipAddress = ip.trim(),
                customName = verified?.name ?: "Tablo ($ip)",
                tuners = verified?.tunerCount ?: 2,
                model = verified?.model ?: "Tablo OTA"
            )
            _uiState.value = _uiState.value.copy(isDiscoveringTablo = false)
            result.onSuccess {
                showMessage("Tablo at $ip successfully added!")
            }.onFailure {
                showMessage("Could not connect to Tablo at $ip: ${it.message}")
            }
        }
    }

    fun addDemoTablo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDiscoveringTablo = true)
            val result = repository.addTabloSource(
                ipAddress = "demo",
                customName = "Living Room Tablo (OTA Quad)",
                tuners = 4,
                model = "Tablo QUAD Tuner"
            )
            _uiState.value = _uiState.value.copy(isDiscoveringTablo = false)
            result.onSuccess {
                showMessage("Tablo QUAD OTA demo connected with live broadcast streams!")
            }
        }
    }

    fun addM3uSource(name: String, urlOrContent: String) {
        viewModelScope.launch {
            val result = repository.addM3uSource(name, urlOrContent)
            result.onSuccess {
                showMessage("Added M3U source: $name (${it.channelCount} channels)")
            }.onFailure {
                showMessage("Failed to parse M3U: ${it.message}")
            }
        }
    }

    fun addXtreamSource(name: String, serverUrl: String, user: String, pass: String) {
        viewModelScope.launch {
            val result = repository.addXtreamSource(name, serverUrl, user, pass)
            result.onSuccess {
                showMessage("Added Xtream source: $name (${it.channelCount} channels)")
            }.onFailure {
                showMessage("Failed connecting to Xtream server: ${it.message}")
            }
        }
    }

    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            repository.deleteSource(sourceId)
            showMessage("Source removed")
        }
    }

    fun showMessage(msg: String) {
        _uiState.value = _uiState.value.copy(statusMessage = msg)
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.releaseAll()
    }
}
