package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

class CheckFirstLaunchUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(): Boolean = !settingsRepository.hasShownHomeSettingsOnFirstLaunch()
}
