package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.SuggestionRepository
import javax.inject.Inject

class SubmitSuggestionUseCase @Inject constructor(
    private val repository: SuggestionRepository,
) {
    suspend operator fun invoke(
        email: String,
        body: String,
        imageUrl: String?,
        appVersion: String,
        deviceInfo: String,
    ): Result<Unit> = repository.submitSuggestion(
        email = email,
        body = body,
        imageUrl = imageUrl,
        appVersion = appVersion,
        deviceInfo = deviceInfo,
    )
}
