package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.PresetRepository
import javax.inject.Inject

class IsDownloadedByMarketIdUseCase @Inject constructor(
    private val repository: PresetRepository,
) {
    suspend operator fun invoke(marketPresetId: String): Boolean =
        repository.isDownloadedByMarketId(marketPresetId)
}
