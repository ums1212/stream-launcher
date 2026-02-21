package org.comon.streamlauncher.domain.repository

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.LauncherSettings

interface SettingsRepository {
    fun getSettings(): Flow<LauncherSettings>
    suspend fun setColorPresetIndex(index: Int)
    suspend fun setGridCellImage(cell: GridCell, idle: String?, expanded: String?)
}
