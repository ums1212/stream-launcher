package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.repository.AppRepository

class GetInstalledAppsUseCase(private val repository: AppRepository) {
    operator fun invoke(): Flow<List<AppEntity>> = repository.getInstalledApps()
}
