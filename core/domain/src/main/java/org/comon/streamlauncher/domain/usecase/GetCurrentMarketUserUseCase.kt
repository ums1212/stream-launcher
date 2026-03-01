package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.preset.MarketUser
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Inject

class GetCurrentMarketUserUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    operator fun invoke(): MarketUser? = repository.getCurrentUser()
}
