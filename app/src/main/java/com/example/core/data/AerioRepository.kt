package com.example.core.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.core.model.Channel
import com.example.core.model.MediaSourceConfig
import com.example.core.model.MultiviewPreset
import com.example.core.model.SourceType
import com.example.core.network.tablo.TabloClient
import com.example.core.network.tablo.TabloDiscovery
import com.example.core.network.tablo.TabloRepository
import com.example.core.network.tablo.TabloResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AerioRepository(
    context: Context,
    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "aeriotv_db"
    ).fallbackToDestructiveMigration().build()
) {
    private val TAG = "AerioRepository"
    private val channelDao = database.channelDao()
    private val sourceDao = database.sourceConfigDao()
    private val layoutPresetDao = database.layoutPresetDao()
    private val tabloDiscovery = TabloDiscovery()

    // Cache of active Tablo repositories keyed by sourceId
    private val activeTabloRepos = ConcurrentHashMap<String, TabloRepository>()
    private val activeTabloClients = ConcurrentHashMap<String, TabloClient>()

    val allChannels: Flow<List<Channel>> = channelDao.getAllChannels().map { list ->
        list.map { it.toDomain() }
    }

    val favoriteChannels: Flow<List<Channel>> = channelDao.getFavorites().map { list ->
        list.map { it.toDomain() }
    }

    val allSources: Flow<List<MediaSourceConfig>> = sourceDao.getAllSources().map { list ->
        list.map { it.toDomain() }
    }

    val allPresets: Flow<List<MultiviewPreset>> = layoutPresetDao.getAllPresets().map { list ->
        list.map { it.toDomain() }
    }

    /**
     * Cleans up legacy demo test data to ensure a production-ready state.
     */
    suspend fun cleanupLegacyTestData() = withContext(Dispatchers.IO) {
        try {
            sourceDao.deleteSource("src_tablo_demo")
            sourceDao.deleteSource("src_iptv_sports")
            channelDao.deleteChannelsBySourceId("src_tablo_demo")
            channelDao.deleteChannelsBySourceId("src_iptv_sports")
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup legacy test data notice: ${e.message}")
        }
    }

    /**
     * Discovers Tablo devices on the local network (UDP & Association server)
     */
    suspend fun discoverTablos() = tabloDiscovery.discoverDevices()

    /**
     * Verifies manual IP for Tablo
     */
    suspend fun verifyTabloIp(ip: String) = tabloDiscovery.verifyTabloDevice(ip)

    /**
     * Connects and registers a Tablo device as a new source and syncs channels from Tablo API
     */
    suspend fun addTabloSource(
        ipAddress: String,
        customName: String? = null,
        tuners: Int = 2,
        model: String = "Tablo OTA"
    ): Result<MediaSourceConfig> = withContext(Dispatchers.IO) {
        val cleanIp = ipAddress.trim().removePrefix("http://").removePrefix("https://").substringBefore(":")
        val client = TabloClient(cleanIp)
        val serverInfo = when (val res = client.getServerInfo()) {
            is TabloResult.Success -> res.data
            is TabloResult.Error -> {
                return@withContext Result.failure(Exception(res.message))
            }
        }

        val name = customName ?: serverInfo.name ?: "Tablo ($cleanIp)"
        val sourceId = "tablo_${cleanIp.replace(".", "_")}"
        val tunerCount = if (serverInfo.tuners > 0) serverInfo.tuners else tuners

        val config = MediaSourceConfig(
            id = sourceId,
            name = name,
            type = SourceType.TABLO,
            hostOrUrl = cleanIp,
            port = 8885,
            serverDetails = "$tunerCount Tuners • ${serverInfo.model ?: model}",
            isActive = true,
            channelCount = 0
        )

        sourceDao.insertSource(SourceConfigEntity.fromDomain(config))

        // Query channels directly from Tablo API: GET /guide/channels
        val repo = getOrCreateTabloRepo(config)
        when (val chanRes = repo.loadChannels()) {
            is TabloResult.Success -> {
                channelDao.deleteChannelsBySourceId(sourceId)
                channelDao.insertChannels(chanRes.data.map { ChannelEntity.fromDomain(it) })
                val updatedConfig = config.copy(channelCount = chanRes.data.size)
                sourceDao.insertSource(SourceConfigEntity.fromDomain(updatedConfig))
                Result.success(updatedConfig)
            }
            is TabloResult.Error -> {
                Result.success(config)
            }
        }
    }

    /**
     * Resolves the playable streaming URL for a channel.
     * Initiates watch session on Tablo hardware tuner via POST /guide/channels/{id}/watch.
     */
    suspend fun resolveStreamUrl(channel: Channel, slotId: String): Result<String> = withContext(Dispatchers.IO) {
        when (channel.sourceType) {
            SourceType.TABLO -> {
                val sourceConfig = sourceDao.getSourceById(channel.sourceId)
                val hostOrIp = sourceConfig?.hostOrUrl ?: channel.sourceId.removePrefix("tablo_").replace("_", ".")
                val port = sourceConfig?.port ?: 8885
                val client = activeTabloClients.getOrPut(channel.sourceId) {
                    TabloClient(hostOrIp, port)
                }
                val repo = activeTabloRepos.getOrPut(channel.sourceId) {
                    TabloRepository(client, channel.sourceId, channel.name)
                }

                when (val res = repo.resolveLiveStream(channel, slotId)) {
                    is TabloResult.Success -> Result.success(res.data)
                    is TabloResult.Error -> Result.failure(Exception(res.message))
                }
            }
            SourceType.M3U, SourceType.XTREAM, SourceType.DISPATCHARR -> {
                val url = channel.streamUrl
                if (!url.isNullOrBlank()) {
                    Result.success(url)
                } else {
                    Result.failure(Exception("Channel has no stream URL"))
                }
            }
        }
    }

    suspend fun releaseStream(channel: Channel, slotId: String) = withContext(Dispatchers.IO) {
        if (channel.sourceType == SourceType.TABLO) {
            activeTabloRepos[channel.sourceId]?.releaseStream(slotId)
        }
    }

    suspend fun toggleFavorite(channelId: String, isFavorite: Boolean) {
        channelDao.setFavorite(channelId, isFavorite)
    }

    suspend fun deleteSource(sourceId: String) {
        sourceDao.deleteSource(sourceId)
        channelDao.deleteChannelsBySourceId(sourceId)
        activeTabloRepos.remove(sourceId)
        activeTabloClients.remove(sourceId)
    }

    suspend fun savePreset(preset: MultiviewPreset) = withContext(Dispatchers.IO) {
        layoutPresetDao.insertPreset(LayoutPresetEntity.fromDomain(preset))
    }

    suspend fun deletePreset(presetId: String) = withContext(Dispatchers.IO) {
        layoutPresetDao.deletePreset(presetId)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        channelDao.clearAll()
        sourceDao.clearAll()
        activeTabloRepos.clear()
        activeTabloClients.clear()
    }

    private fun getOrCreateTabloRepo(config: MediaSourceConfig): TabloRepository {
        return activeTabloRepos.getOrPut(config.id) {
            val client = activeTabloClients.getOrPut(config.id) {
                TabloClient(config.hostOrUrl, config.port ?: 8885)
            }
            TabloRepository(client, config.id, config.name)
        }
    }
}
