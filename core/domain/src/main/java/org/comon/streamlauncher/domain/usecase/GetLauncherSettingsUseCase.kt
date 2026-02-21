package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.LauncherSettings
import org.comon.streamlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

class GetLauncherSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<LauncherSettings> = settingsRepository.getSettings()
}
