package org.comon.streamlauncher.data.local.room.livewallpaper

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.comon.streamlauncher.domain.model.LiveWallpaper

@Entity(tableName = "live_wallpapers")
data class LiveWallpaperEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val fileUri: String,
    val thumbnailUri: String?,
    val createdAt: Long,
)

fun LiveWallpaperEntity.toDomain() = LiveWallpaper(
    id = id,
    name = name,
    fileUri = fileUri,
    thumbnailUri = thumbnailUri,
    createdAt = createdAt,
)

