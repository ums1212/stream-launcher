package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.PresetRepository
import javax.inject.Inject

class UpdateMarketPresetIdUseCase @Inject constructor(
    private val repository: PresetRepository,
) {
    suspend operator fun invoke(presetId: Int, marketPresetId: String): Int =
        repository.updateMarketPresetId(presetId, marketPresetId)
}
