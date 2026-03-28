package org.comon.streamlauncher.domain.repository

interface SuggestionRepository {
    suspend fun submitSuggestion(
        email: String,
        body: String,
        imageUrl: String? = null,
        appVersion: String,
        deviceInfo: String,
    ): Result<Unit>

    suspend fun uploadSuggestionImage(localUri: String, storagePath: String): Result<String>
}
