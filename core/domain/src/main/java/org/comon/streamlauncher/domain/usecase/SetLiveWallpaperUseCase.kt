package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

class SetLiveWallpaperUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(id: Int?, uri: String?) = repository.setLiveWallpaper(id, uri)
}
