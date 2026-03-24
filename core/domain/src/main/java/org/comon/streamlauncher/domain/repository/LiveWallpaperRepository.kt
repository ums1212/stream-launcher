package org.comon.streamlauncher.domain.repository

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.LiveWallpaper

interface LiveWallpaperRepository {
    fun getAllLiveWallpapers(): Flow<List<LiveWallpaper>>
    suspend fun getLiveWallpaperById(id: Int): LiveWallpaper?
    suspend fun saveLiveWallpaper(name: String, sourceUri: String): LiveWallpaper
    suspend fun deleteLiveWallpaper(id: Int)
}
