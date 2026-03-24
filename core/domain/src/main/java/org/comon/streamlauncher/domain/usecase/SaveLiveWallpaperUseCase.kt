package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.LiveWallpaper
import org.comon.streamlauncher.domain.repository.LiveWallpaperRepository
import javax.inject.Inject

class SaveLiveWallpaperUseCase @Inject constructor(
    private val repository: LiveWallpaperRepository,
) {
    suspend operator fun invoke(name: String, sourceUri: String): LiveWallpaper =
        repository.saveLiveWallpaper(name, sourceUri)
}
