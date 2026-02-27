package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

class DismissNoticeUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(version: String) = repo.setLastShownNoticeVersion(version)
}
