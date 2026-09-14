package com.example.core.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class PlayerSlotStatus(
    val slotIndex: Int,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val isAudioActive: Boolean = false,
    val currentUrl: String? = null
)

class ExoPlayerSlotManager(private val context: Context) {
    private val TAG = "ExoPlayerSlotManager"
    private val players = ConcurrentHashMap<Int, ExoPlayer>()

    private val _statusMap = MutableStateFlow<Map<Int, PlayerSlotStatus>>(emptyMap())
    val statusMap: StateFlow<Map<Int, PlayerSlotStatus>> = _statusMap.asStateFlow()

    private var activeAudioSlotIndex: Int = 0

    @OptIn(UnstableApi::class)
    fun getOrCreatePlayer(slotIndex: Int): ExoPlayer {
        return players.getOrPut(slotIndex) {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    2000,  // minBufferMs
                    10000, // maxBufferMs
                    1000,  // bufferForPlaybackMs
                    1500   // bufferForPlaybackAfterRebufferMs
                )
                .build()

            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 AerioTV/1.0")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(15000)

            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)

            val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
                setEnableDecoderFallback(true)
            }

            val player = ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_ALL
                    volume = if (slotIndex == activeAudioSlotIndex) 1.0f else 0.0f
                    playWhenReady = false

                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val isBuffering = playbackState == Player.STATE_BUFFERING
                            val isPlaying = playbackState == Player.STATE_READY && playWhenReady
                            updateSlotStatus(slotIndex) { current ->
                                current.copy(
                                    isBuffering = isBuffering,
                                    isPlaying = isPlaying,
                                    hasError = false,
                                    errorMessage = null
                                )
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "Slot $slotIndex player error: ${error.message}")
                            updateSlotStatus(slotIndex) { current ->
                                current.copy(
                                    isBuffering = false,
                                    isPlaying = false,
                                    hasError = true,
                                    errorMessage = error.localizedMessage ?: "Playback error"
                                )
                            }
                        }
                    })
                }

            updateSlotStatus(slotIndex) {
                PlayerSlotStatus(
                    slotIndex = slotIndex,
                    isAudioActive = (slotIndex == activeAudioSlotIndex)
                )
            }

            player
        }
    }

    @OptIn(UnstableApi::class)
    fun playStream(slotIndex: Int, streamUrl: String) {
        val player = getOrCreatePlayer(slotIndex)
        try {
            val uri = Uri.parse(streamUrl)
            val isHls = streamUrl.contains(".m3u8") || streamUrl.contains("stream/pl")
            val mediaItemBuilder = MediaItem.Builder().setUri(uri)
            if (isHls) {
                mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            }
            val mediaItem = mediaItemBuilder.build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true

            updateSlotStatus(slotIndex) {
                it.copy(
                    isBuffering = true,
                    hasError = false,
                    errorMessage = null,
                    currentUrl = streamUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed setting media for slot $slotIndex: ${e.message}")
            updateSlotStatus(slotIndex) {
                it.copy(
                    isBuffering = false,
                    hasError = true,
                    errorMessage = e.message ?: "Failed to play stream"
                )
            }
        }
    }

    /**
     * Sets which slot outputs audio (Sunday Ticket style audio switching)
     */
    fun setActiveAudioSlot(slotIndex: Int) {
        activeAudioSlotIndex = slotIndex
        players.forEach { (index, player) ->
            player.volume = if (index == slotIndex) 1.0f else 0.0f
        }
        val currentMap = _statusMap.value.toMutableMap()
        currentMap.keys.forEach { index ->
            val status = currentMap[index] ?: PlayerSlotStatus(slotIndex = index)
            currentMap[index] = status.copy(isAudioActive = (index == slotIndex))
        }
        _statusMap.value = currentMap
    }

    fun stopSlot(slotIndex: Int) {
        players[slotIndex]?.let { player ->
            player.stop()
            player.clearMediaItems()
        }
        updateSlotStatus(slotIndex) {
            it.copy(isPlaying = false, isBuffering = false, hasError = false, currentUrl = null)
        }
    }

    fun releaseAll() {
        players.forEach { (_, player) ->
            try {
                player.stop()
                player.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing player: ${e.message}")
            }
        }
        players.clear()
        _statusMap.value = emptyMap()
    }

    private fun updateSlotStatus(slotIndex: Int, transform: (PlayerSlotStatus) -> PlayerSlotStatus) {
        val currentMap = _statusMap.value.toMutableMap()
        val current = currentMap[slotIndex] ?: PlayerSlotStatus(
            slotIndex = slotIndex,
            isAudioActive = (slotIndex == activeAudioSlotIndex)
        )
        currentMap[slotIndex] = transform(current)
        _statusMap.value = currentMap
    }
}
