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
    suspend fun uploadImage(localUri: String, storagePath: String, maxWidth: Int = 1080, quality: Int = 80): Result<String>
    /** getDownloadUrl 없이 gs:// 경로만 반환 (신고 이미지 등 앱 표시 불필요한 경우) */
    suspend fun uploadImageGetPath(localUri: String, storagePath: String, maxWidth: Int = 1080, quality: Int = 80): Result<String>
    suspend fun uploadPreviewImage(
        localUri: String,
        storagePath: String,
        maxWidth: Int = 720,
        quality: Int = 70,
        maxSizeBytes: Long = 20L * 1024 * 1024,
    ): Result<String>
    suspend fun uploadSlpFile(localPath: String, storagePath: String): Result<String>
    suspend fun downloadImageToLocal(storageUrl: String, localPath: String): Result<String>
    suspend fun downloadSlpFile(storageUrl: String, localPath: String): Result<String>
    suspend fun toggleLike(presetId: String): Result<Boolean>
    suspend fun incrementDownloadCount(presetId: String): Result<Unit>
    suspend fun getPresetsByAuthor(uid: String): Result<List<MarketPreset>>
    suspend fun deletePreset(presetId: String): Result<Unit>
    suspend fun reportPreset(
        reporterUid: String,
        reporterDisplayName: String,
        presetId: String,
        presetName: String,
        presetAuthorUid: String,
        presetAuthorDisplayName: String,
        reason: String,
        imageUrl: String? = null,
    ): Result<Unit>
}
