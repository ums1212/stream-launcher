package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.preset.MarketUser
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    suspend operator fun invoke(idToken: String): Result<MarketUser> =
        repository.signInWithGoogle(idToken)
}
