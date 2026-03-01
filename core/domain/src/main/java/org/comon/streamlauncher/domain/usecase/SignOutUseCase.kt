package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    suspend operator fun invoke() = repository.signOut()
}
