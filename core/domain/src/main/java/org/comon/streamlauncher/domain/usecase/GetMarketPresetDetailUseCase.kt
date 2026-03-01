package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Inject

class GetMarketPresetDetailUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    suspend operator fun invoke(presetId: String): Result<MarketPreset> =
        repository.getPresetDetail(presetId)
}
