package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.LiveWallpaperRepository
import javax.inject.Inject

class DeleteLiveWallpaperUseCase @Inject constructor(
    private val repository: LiveWallpaperRepository,
) {
    suspend operator fun invoke(id: Int) = repository.deleteLiveWallpaper(id)
}
