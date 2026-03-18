package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Inject

class GetUserPresetsUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    suspend operator fun invoke(uid: String): Result<List<MarketPreset>> =
        repository.getPresetsByAuthor(uid)
}
