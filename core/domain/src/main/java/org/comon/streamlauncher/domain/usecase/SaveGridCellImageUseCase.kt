package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

class SaveGridCellImageUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(cell: GridCell, idle: String?, expanded: String?) =
        settingsRepository.setGridCellImage(cell, idle, expanded)
}
