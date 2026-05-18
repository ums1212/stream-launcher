package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.repository.AppRepository
import javax.inject.Inject

class RefreshInstalledAppsUseCase @Inject constructor(private val repository: AppRepository) {
    suspend operator fun invoke(): List<AppEntity> = repository.refreshInstalledApps()
}
