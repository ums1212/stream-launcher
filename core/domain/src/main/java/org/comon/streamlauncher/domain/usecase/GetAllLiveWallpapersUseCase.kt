package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.LiveWallpaper
import org.comon.streamlauncher.domain.repository.LiveWallpaperRepository
import javax.inject.Inject

class GetAllLiveWallpapersUseCase @Inject constructor(
    private val repository: LiveWallpaperRepository,
) {
    operator fun invoke(): Flow<List<LiveWallpaper>> = repository.getAllLiveWallpapers()
}
