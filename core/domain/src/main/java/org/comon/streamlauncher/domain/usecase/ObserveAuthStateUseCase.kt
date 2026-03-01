package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.preset.MarketUser
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    operator fun invoke(): Flow<MarketUser?> = repository.authStateChanges()
}
