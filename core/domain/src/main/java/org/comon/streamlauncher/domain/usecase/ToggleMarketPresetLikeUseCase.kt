package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Inject

class ToggleMarketPresetLikeUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    suspend operator fun invoke(presetId: String): Result<Boolean> =
        repository.toggleLike(presetId)
}
