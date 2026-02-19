package org.comon.streamlauncher.domain.repository

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.AppInfo

interface AppRepository {
    fun getInstalledApps(): Flow<List<AppInfo>>
}
