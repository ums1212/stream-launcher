package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

class CheckNoticeUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(currentVersion: String): Boolean =
        repo.getLastShownNoticeVersion() != currentVersion
}
