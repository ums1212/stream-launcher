package org.comon.streamlauncher.data.local.room.livewallpaper

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveWallpaperDao {
    @Query("SELECT * FROM live_wallpapers ORDER BY createdAt DESC")
    fun getAll(): Flow<List<LiveWallpaperEntity>>

    @Query("SELECT * FROM live_wallpapers WHERE id = :id LIMIT 1")
    fun getById(id: Int): LiveWallpaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: LiveWallpaperEntity): Long

    @Query("DELETE FROM live_wallpapers WHERE id = :id")
    fun deleteById(id: Int): Int
}
