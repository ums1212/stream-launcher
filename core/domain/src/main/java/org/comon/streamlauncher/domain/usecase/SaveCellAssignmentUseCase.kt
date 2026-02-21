package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

class SaveCellAssignmentUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(cell: GridCell, packageNames: List<String>) {
        repository.setCellAssignment(cell, packageNames)
    }
}
