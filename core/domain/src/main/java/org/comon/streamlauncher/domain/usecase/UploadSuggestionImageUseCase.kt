package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.SuggestionRepository
import javax.inject.Inject

class UploadSuggestionImageUseCase @Inject constructor(
    private val repository: SuggestionRepository,
) {
    suspend operator fun invoke(localUri: String): Result<String> {
        val storagePath = "suggestion-images/${System.currentTimeMillis()}"
        return repository.uploadSuggestionImage(localUri, storagePath)
    }
}
