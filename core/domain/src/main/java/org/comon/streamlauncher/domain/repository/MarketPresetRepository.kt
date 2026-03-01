package org.comon.streamlauncher.domain.repository

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.MarketUser

interface MarketPresetRepository {
    fun getCurrentUser(): MarketUser?
    fun authStateChanges(): Flow<MarketUser?>
    suspend fun signInWithGoogle(idToken: String): Result<MarketUser>
    suspend fun signOut()
    suspend fun getTopByDownloads(limit: Int): Result<List<MarketPreset>>
    suspend fun getTopByLikes(limit: Int): Result<List<MarketPreset>>
    suspend fun getPresetDetail(presetId: String): Result<MarketPreset>
    suspend fun isLikedByCurrentUser(presetId: String): Result<Boolean>
    suspend fun searchPresets(
        query: String,
        pageSize: Int,
        lastDocId: String?,
    ): Result<List<MarketPreset>>
    suspend fun uploadPreset(preset: MarketPreset): Result<String>
    suspend fun uploadImage(localUri: String, storagePath: String): Result<String>
    suspend fun downloadImageToLocal(storageUrl: String, localPath: String): Result<String>
    suspend fun toggleLike(presetId: String): Result<Boolean>
    suspend fun incrementDownloadCount(presetId: String): Result<Unit>
}
