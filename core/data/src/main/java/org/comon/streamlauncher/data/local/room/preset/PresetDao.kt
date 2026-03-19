package org.comon.streamlauncher.data.local.room.preset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY createdAt DESC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPreset(preset: PresetEntity)

    @Delete
    fun deletePreset(preset: PresetEntity)

    @Query("SELECT COUNT(*) FROM presets")
    fun getPresetCount(): Int

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    fun getById(id: Int): PresetEntity?
    
@Query("SELECT EXISTS(SELECT 1 FROM presets WHERE marketPresetId = :marketPresetId)")
    fun existsByMarketPresetId(marketPresetId: String): Boolean

    @Query("UPDATE presets SET marketPresetId = :marketPresetId WHERE id = :id")
    fun updateMarketPresetId(id: Int, marketPresetId: String): Int
}
