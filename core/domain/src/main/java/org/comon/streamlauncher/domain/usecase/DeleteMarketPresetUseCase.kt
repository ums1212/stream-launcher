package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Inject

class DeleteMarketPresetUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    suspend operator fun invoke(presetId: String): Result<Unit> = repository.deletePreset(presetId)
}
