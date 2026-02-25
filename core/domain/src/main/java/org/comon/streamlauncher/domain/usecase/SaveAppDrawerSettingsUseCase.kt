package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

class SaveAppDrawerSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(columns: Int, rows: Int, iconSizeRatio: Float) {
        settingsRepository.setAppDrawerSettings(columns, rows, iconSizeRatio)
    }
}
