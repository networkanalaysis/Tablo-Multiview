package com.example.core.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.core.model.Channel
import com.example.core.model.MediaSourceConfig
import com.example.core.model.MultiviewLayout
import com.example.core.model.MultiviewPreset
import com.example.core.model.ProgramGuideItem
import com.example.core.model.SourceType
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val channelNumber: String,
    val callSign: String,
    val logoUrl: String?,
    val streamUrl: String?,
    val watchPath: String?,
    val sourceType: String,
    val sourceId: String,
    val groupTitle: String,
    val isFavorite: Boolean,
    val resolution: String,
    val programTitle: String?,
    val programDesc: String?,
    val programStart: Long,
    val programEnd: Long,
    val programCategory: String?,
    val upProgramTitle: String? = null,
    val upProgramDesc: String? = null,
    val upProgramStart: Long = 0L,
    val upProgramEnd: Long = 0L
) {
    fun toDomain(): Channel {
        val prog = if (!programTitle.isNullOrBlank()) {
            ProgramGuideItem(
                id = "prog_$id",
                channelId = id,
                title = programTitle,
                description = programDesc ?: "",
                startTimeEpoch = programStart,
                endTimeEpoch = programEnd,
                category = programCategory ?: "General"
            )
        } else null

        val upProg = if (!upProgramTitle.isNullOrBlank()) {
            ProgramGuideItem(
                id = "up_prog_$id",
                channelId = id,
                title = upProgramTitle,
                description = upProgramDesc ?: "",
                startTimeEpoch = upProgramStart,
                endTimeEpoch = upProgramEnd,
                category = programCategory ?: "General"
            )
        } else null

        val type = try {
            SourceType.valueOf(sourceType)
        } catch (_: Exception) {
            SourceType.TABLO
        }

        return Channel(
            id = id,
            name = name,
            channelNumber = channelNumber,
            callSign = callSign,
            logoUrl = logoUrl,
            streamUrl = streamUrl,
            watchPath = watchPath,
            sourceType = type,
            sourceId = sourceId,
            groupTitle = groupTitle,
            isFavorite = isFavorite,
            currentProgram = prog,
            upcomingProgram = upProg,
            resolution = resolution
        )
    }

    companion object {
        fun fromDomain(channel: Channel): ChannelEntity {
            return ChannelEntity(
                id = channel.id,
                name = channel.name,
                channelNumber = channel.channelNumber,
                callSign = channel.callSign,
                logoUrl = channel.logoUrl,
                streamUrl = channel.streamUrl,
                watchPath = channel.watchPath,
                sourceType = channel.sourceType.name,
                sourceId = channel.sourceId,
                groupTitle = channel.groupTitle,
                isFavorite = channel.isFavorite,
                resolution = channel.resolution,
                programTitle = channel.currentProgram?.title,
                programDesc = channel.currentProgram?.description,
                programStart = channel.currentProgram?.startTimeEpoch ?: 0L,
                programEnd = channel.currentProgram?.endTimeEpoch ?: 0L,
                programCategory = channel.currentProgram?.category,
                upProgramTitle = channel.upcomingProgram?.title,
                upProgramDesc = channel.upcomingProgram?.description,
                upProgramStart = channel.upcomingProgram?.startTimeEpoch ?: 0L,
                upProgramEnd = channel.upcomingProgram?.endTimeEpoch ?: 0L
            )
        }
    }
}

@Entity(tableName = "source_configs")
data class SourceConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val hostOrUrl: String,
    val port: Int?,
    val username: String?,
    val password: String?,
    val apiKey: String?,
    val serverDetails: String?,
    val isActive: Boolean,
    val channelCount: Int = 0
) {
    fun toDomain(): MediaSourceConfig {
        val srcType = try {
            SourceType.valueOf(type)
        } catch (_: Exception) {
            SourceType.TABLO
        }
        return MediaSourceConfig(
            id = id,
            name = name,
            type = srcType,
            hostOrUrl = hostOrUrl,
            port = port,
            username = username,
            password = password,
            apiKey = apiKey,
            serverDetails = serverDetails,
            isActive = isActive,
            channelCount = channelCount
        )
    }

    companion object {
        fun fromDomain(config: MediaSourceConfig): SourceConfigEntity {
            return SourceConfigEntity(
                id = config.id,
                name = config.name,
                type = config.type.name,
                hostOrUrl = config.hostOrUrl,
                port = config.port,
                username = config.username,
                password = config.password,
                apiKey = config.apiKey,
                serverDetails = config.serverDetails,
                isActive = config.isActive,
                channelCount = config.channelCount
            )
        }
    }
}

@Entity(tableName = "layout_presets")
data class LayoutPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val arrangement: String,
    val channelIdsCsv: String,
    val channelNamesCsv: String,
    val createdAtEpoch: Long
) {
    fun toDomain(): MultiviewPreset {
        val layout = try {
            MultiviewLayout.valueOf(arrangement)
        } catch (_: Exception) {
            MultiviewLayout.QUAD_GRID
        }
        val ids = if (channelIdsCsv.isNotBlank()) channelIdsCsv.split(",") else emptyList()
        val names = if (channelNamesCsv.isNotBlank()) channelNamesCsv.split("|||") else emptyList()
        return MultiviewPreset(
            id = id,
            name = name,
            arrangement = layout,
            channelIds = ids,
            channelNames = names,
            createdAtEpoch = createdAtEpoch
        )
    }

    companion object {
        fun fromDomain(preset: MultiviewPreset): LayoutPresetEntity {
            return LayoutPresetEntity(
                id = preset.id,
                name = preset.name,
                arrangement = preset.arrangement.name,
                channelIdsCsv = preset.channelIds.joinToString(","),
                channelNamesCsv = preset.channelNames.joinToString("|||"),
                createdAtEpoch = preset.createdAtEpoch
            )
        }
    }
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY CAST(channelNumber AS REAL) ASC, channelNumber ASC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE sourceType = :sourceType")
    fun getChannelsBySourceType(sourceType: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId")
    fun getChannelsBySourceId(sourceId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun setFavorite(channelId: String, isFavorite: Boolean)

    @Query("DELETE FROM channels WHERE sourceId = :sourceId")
    suspend fun deleteChannelsBySourceId(sourceId: String)

    @Query("DELETE FROM channels")
    suspend fun clearAll()
}

@Dao
interface SourceConfigDao {
    @Query("SELECT * FROM source_configs ORDER BY name ASC")
    fun getAllSources(): Flow<List<SourceConfigEntity>>

    @Query("SELECT * FROM source_configs WHERE id = :id LIMIT 1")
    suspend fun getSourceById(id: String): SourceConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceConfigEntity)

    @Update
    suspend fun updateSource(source: SourceConfigEntity)

    @Query("DELETE FROM source_configs WHERE id = :id")
    suspend fun deleteSource(id: String)

    @Query("DELETE FROM source_configs")
    suspend fun clearAll()
}

@Dao
interface LayoutPresetDao {
    @Query("SELECT * FROM layout_presets ORDER BY createdAtEpoch DESC")
    fun getAllPresets(): Flow<List<LayoutPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: LayoutPresetEntity)

    @Query("DELETE FROM layout_presets WHERE id = :id")
    suspend fun deletePreset(id: String)
}

@Database(
    entities = [ChannelEntity::class, SourceConfigEntity::class, LayoutPresetEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun sourceConfigDao(): SourceConfigDao
    abstract fun layoutPresetDao(): LayoutPresetDao
}
