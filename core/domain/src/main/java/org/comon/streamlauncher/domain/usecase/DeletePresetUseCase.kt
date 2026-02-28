package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.repository.PresetRepository
import javax.inject.Inject

class DeletePresetUseCase @Inject constructor(
    private val presetRepository: PresetRepository
) {
    suspend operator fun invoke(preset: Preset) {
        presetRepository.deletePreset(preset)
    }
}
