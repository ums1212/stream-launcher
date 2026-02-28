package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.repository.PresetRepository
import javax.inject.Inject

class GetAllPresetsUseCase @Inject constructor(
    private val presetRepository: PresetRepository
) {
    operator fun invoke(): Flow<List<Preset>> {
        return presetRepository.getAllPresets()
    }
}
