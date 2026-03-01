package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Inject

class GetTopDownloadPresetsUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    suspend operator fun invoke(limit: Int = 10): Result<List<MarketPreset>> =
        repository.getTopByDownloads(limit)
}
