package com.example.core.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.core.model.Channel
import com.example.core.model.MediaSourceConfig
import com.example.core.model.ProgramGuideItem
import com.example.core.model.SourceType
import com.example.core.network.iptv.M3UParser
import com.example.core.network.iptv.XtreamClient
import com.example.core.network.tablo.TabloClient
import com.example.core.network.tablo.TabloDiscovery
import com.example.core.network.tablo.TabloRepository
import com.example.core.network.tablo.TabloResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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

    /**
     * Initializes default demo Tablo & IPTV sources if database is empty
     */
    suspend fun initializeDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        val initialSources = listOf(
            MediaSourceConfig(
                id = "src_tablo_demo",
                name = "Living Room Tablo (OTA Tuner)",
                type = SourceType.TABLO,
                hostOrUrl = "demo",
                port = 8885,
                serverDetails = "4 Tuners • OTA Antenna 100% Signal",
                isActive = true,
                channelCount = 6
            ),
            MediaSourceConfig(
                id = "src_iptv_sports",
                name = "AerioTV Stadium Streams (IPTV)",
                type = SourceType.M3U,
                hostOrUrl = "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/us_sports.m3u",
                serverDetails = "IPTV HLS Feeds",
                isActive = true,
                channelCount = 4
            )
        )

        initialSources.forEach { config ->
            sourceDao.insertSource(SourceConfigEntity.fromDomain(config))
            if (config.type == SourceType.TABLO) {
                getOrCreateTabloRepo(config)
            }
        }

        // Pre-populate sample channels for instant-play Sunday Ticket experience
        val defaultChannels = listOf(
            createSeedChannel(
                id = "tablo_src_tablo_demo_cbs",
                name = "CBS (WCBS-HD)",
                chNum = "2.1",
                callSign = "WCBS",
                logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1b/CBS_logo_%282020%29.svg/320px-CBS_logo_%282020%29.svg.png",
                watchPath = "/guide/channels/1001",
                sourceId = "src_tablo_demo",
                sourceType = SourceType.TABLO,
                progTitle = "NFL on CBS: Chiefs vs Bills",
                progDesc = "AFC battle live in 1080i high definition from Highmark Stadium.",
                category = "Sports"
            ),
            createSeedChannel(
                id = "tablo_src_tablo_demo_nbc",
                name = "NBC (WNBC-HD)",
                chNum = "4.1",
                callSign = "WNBC",
                logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/NBC_logo_%282022%29.svg/320px-NBC_logo_%282022%29.svg.png",
                watchPath = "/guide/channels/1002",
                sourceId = "src_tablo_demo",
                sourceType = SourceType.TABLO,
                progTitle = "Sunday Night Football: Eagles at Cowboys",
                progDesc = "NFC East showdown with live field-level cameras and stereo broadcast.",
                category = "Sports"
            ),
            createSeedChannel(
                id = "tablo_src_tablo_demo_fox",
                name = "FOX (WNYW-HD)",
                chNum = "5.1",
                callSign = "WNYW",
                logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/Fox_Broadcasting_Company_Logo.svg/320px-Fox_Broadcasting_Company_Logo.svg.png",
                watchPath = "/guide/channels/1003",
                sourceId = "src_tablo_demo",
                sourceType = SourceType.TABLO,
                progTitle = "FOX NFL Sunday: 49ers at Packers",
                progDesc = "NFC playoff rematch live from Lambeau Field.",
                category = "Sports"
            ),
            createSeedChannel(
                id = "tablo_src_tablo_demo_abc",
                name = "ABC (WABC-HD)",
                chNum = "7.1",
                callSign = "WABC",
                logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/22/American_Broadcasting_Company_Logo.svg/320px-American_Broadcasting_Company_Logo.svg.png",
                watchPath = "/guide/channels/1004",
                sourceId = "src_tablo_demo",
                sourceType = SourceType.TABLO,
                progTitle = "College Football Prime: Texas vs Oklahoma",
                progDesc = "Red River Rivalry live in 720p 60fps.",
                category = "Sports"
            ),
            createSeedChannel(
                id = "tablo_src_tablo_demo_pbs",
                name = "PBS (WNET-HD)",
                chNum = "13.1",
                callSign = "WNET",
                logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/10/PBS_logo_%282019%29.svg/320px-PBS_logo_%282019%29.svg.png",
                watchPath = "/guide/channels/1005",
                sourceId = "src_tablo_demo",
                sourceType = SourceType.TABLO,
                progTitle = "PBS NewsHour",
                progDesc = "Comprehensive global and domestic journalism.",
                category = "News"
            ),
            createSeedChannel(
                id = "iptv_sports_redbull",
                name = "Red Bull TV Live",
                chNum = "101",
                callSign = "RBTV",
                logo = null,
                watchPath = null,
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                sourceId = "src_iptv_sports",
                sourceType = SourceType.M3U,
                progTitle = "Action Sports Extreme Showcase",
                progDesc = "World-class snowboarding, motocross, and downhill mountain biking.",
                category = "Sports"
            ),
            createSeedChannel(
                id = "iptv_sports_nasatv",
                name = "NASA TV Live Feed",
                chNum = "102",
                callSign = "NASA",
                logo = null,
                watchPath = null,
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                sourceId = "src_iptv_sports",
                sourceType = SourceType.M3U,
                progTitle = "International Space Station Live",
                progDesc = "Live spacewalk views and scientific telemetry from orbit.",
                category = "Documentary"
            )
        )

        channelDao.insertChannels(defaultChannels.map { ChannelEntity.fromDomain(it) })
    }

    private fun createSeedChannel(
        id: String,
        name: String,
        chNum: String,
        callSign: String,
        logo: String?,
        watchPath: String?,
        streamUrl: String? = null,
        sourceId: String,
        sourceType: SourceType,
        progTitle: String,
        progDesc: String,
        category: String
    ): Channel {
        val now = System.currentTimeMillis()
        return Channel(
            id = id,
            name = name,
            channelNumber = chNum,
            callSign = callSign,
            logoUrl = logo,
            streamUrl = streamUrl,
            watchPath = watchPath,
            sourceType = sourceType,
            sourceId = sourceId,
            groupTitle = if (sourceType == SourceType.TABLO) "Tablo OTA" else "IPTV Channels",
            isFavorite = false,
            currentProgram = ProgramGuideItem(
                id = "seed_prog_$id",
                channelId = id,
                title = progTitle,
                description = progDesc,
                startTimeEpoch = now,
                endTimeEpoch = now + 10800_000L,
                category = category
            ),
            resolution = "1080i"
        )
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
     * Connects and registers a Tablo device as a new source and syncs channels
     */
    suspend fun addTabloSource(
        ipAddress: String,
        customName: String? = null,
        tuners: Int = 2,
        model: String = "Tablo OTA"
    ): Result<MediaSourceConfig> = withContext(Dispatchers.IO) {
        val client = TabloClient(ipAddress)
        val serverInfo = when (val res = client.getServerInfo()) {
            is TabloResult.Success -> res.data
            is TabloResult.Error -> {
                if (ipAddress.equals("demo", ignoreCase = true)) {
                    null
                } else {
                    return@withContext Result.failure(Exception(res.message))
                }
            }
        }

        val name = customName ?: serverInfo?.name ?: "Tablo ($ipAddress)"
        val sourceId = "tablo_${ipAddress.replace(".", "_")}"
        val tunerCount = serverInfo?.tuners ?: tuners

        val config = MediaSourceConfig(
            id = sourceId,
            name = name,
            type = SourceType.TABLO,
            hostOrUrl = ipAddress,
            port = 8885,
            serverDetails = "$tunerCount Tuners • ${serverInfo?.model ?: model}",
            isActive = true,
            channelCount = 0
        )

        sourceDao.insertSource(SourceConfigEntity.fromDomain(config))

        // Get Tablo repository and fetch channels
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
     * Adds an M3U Playlist source
     */
    suspend fun addM3uSource(
        name: String,
        urlOrContent: String
    ): Result<MediaSourceConfig> = withContext(Dispatchers.IO) {
        try {
            val sourceId = "m3u_${System.currentTimeMillis()}"
            val content = if (urlOrContent.startsWith("http://") || urlOrContent.startsWith("https://")) {
                val okHttp = OkHttpClient()
                val req = Request.Builder().url(urlOrContent).build()
                val resp = okHttp.newCall(req).execute()
                resp.body?.string() ?: ""
            } else {
                urlOrContent
            }

            val channels = M3UParser.parse(content, sourceId, name)
            val config = MediaSourceConfig(
                id = sourceId,
                name = name,
                type = SourceType.M3U,
                hostOrUrl = urlOrContent,
                serverDetails = "${channels.size} IPTV Channels",
                isActive = true,
                channelCount = channels.size
            )

            sourceDao.insertSource(SourceConfigEntity.fromDomain(config))
            channelDao.insertChannels(channels.map { ChannelEntity.fromDomain(it) })
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds an Xtream Codes source
     */
    suspend fun addXtreamSource(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<MediaSourceConfig> = withContext(Dispatchers.IO) {
        try {
            val sourceId = "xtream_${System.currentTimeMillis()}"
            val client = XtreamClient(serverUrl, username, password)
            val channels = client.getLiveStreams(sourceId)

            val config = MediaSourceConfig(
                id = sourceId,
                name = name,
                type = SourceType.XTREAM,
                hostOrUrl = serverUrl,
                username = username,
                password = password,
                serverDetails = "${channels.size} Xtream Live Streams",
                isActive = true,
                channelCount = channels.size
            )

            sourceDao.insertSource(SourceConfigEntity.fromDomain(config))
            channelDao.insertChannels(channels.map { ChannelEntity.fromDomain(it) })
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves the playable streaming URL for a channel.
     * If the channel is from a Tablo source, POSTs to the Tablo /watch endpoint
     * to obtain the live HLS playlist_url.
     */
    suspend fun resolveStreamUrl(channel: Channel, slotId: String): Result<String> = withContext(Dispatchers.IO) {
        when (channel.sourceType) {
            SourceType.TABLO -> {
                val sourceConfig = sourceDao.getSourceById(channel.sourceId)
                val hostOrIp = when {
                    sourceConfig != null -> sourceConfig.hostOrUrl
                    channel.sourceId.contains("demo", ignoreCase = true) -> "demo"
                    else -> channel.sourceId.removePrefix("tablo_").replace("_", ".")
                }
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

    private fun getOrCreateTabloRepo(config: MediaSourceConfig): TabloRepository {
        return activeTabloRepos.getOrPut(config.id) {
            val client = activeTabloClients.getOrPut(config.id) {
                TabloClient(config.hostOrUrl, config.port ?: 8885)
            }
            TabloRepository(client, config.id, config.name)
        }
    }
}
