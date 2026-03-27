package org.comon.streamlauncher.data.repository

import android.content.Context
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.data.datasource.MarketAuthDataSource
import org.comon.streamlauncher.data.datasource.MarketPresetRemoteDataSource
import org.comon.streamlauncher.data.datasource.MarketStorageDataSource
import org.comon.streamlauncher.data.util.AnimationDetector
import org.comon.streamlauncher.data.util.ImageCompressor
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.MarketUser
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import org.comon.streamlauncher.paging.RecentPresetsPagerProvider
import org.comon.streamlauncher.paging.SearchPresetsPagerProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketPresetRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authDataSource: MarketAuthDataSource,
    private val presetRemoteDataSource: MarketPresetRemoteDataSource,
    private val storageDataSource: MarketStorageDataSource,
) : MarketPresetRepository, RecentPresetsPagerProvider, SearchPresetsPagerProvider {

    override fun authStateChanges(): Flow<MarketUser?> = authDataSource.authStateChanges()

    override fun getCurrentUser(): MarketUser? = authDataSource.getCurrentUser()

    override suspend fun signInWithGoogle(idToken: String): Result<MarketUser> = runCatching {
        authDataSource.signInWithGoogle(idToken)
    }

    override suspend fun signOut() {
        authDataSource.signOut()
    }

    override suspend fun getTopByDownloads(limit: Int): Result<List<MarketPreset>> = runCatching {
        presetRemoteDataSource.getPresetsOrderedBy("downloadCount", limit)
    }

    override suspend fun getTopByLikes(limit: Int): Result<List<MarketPreset>> = runCatching {
        presetRemoteDataSource.getPresetsOrderedBy("likeCount", limit)
    }

    override suspend fun getPresetDetail(presetId: String): Result<MarketPreset> = runCatching {
        presetRemoteDataSource.getPresetById(presetId)
            ?: error("프리셋을 찾을 수 없습니다: $presetId")
    }

    override suspend fun isLikedByCurrentUser(presetId: String): Result<Boolean> = runCatching {
        val uid = authDataSource.getCurrentUserId() ?: return@runCatching false
        presetRemoteDataSource.isLikedByUser(presetId, uid)
    }

    override suspend fun searchPresets(
        query: String,
        pageSize: Int,
        lastDocId: String?,
    ): Result<List<MarketPreset>> = runCatching {
        // 단순 검색은 PagingSource 기반으로 대체되어 있으나 인터페이스 호환성 유지
        val searchTerm = query.lowercase().trim().replace(" ", "")
        presetRemoteDataSource.getPresetsOrderedBy("createdAt", pageSize)
            .filter { preset ->
                val keywords = buildList {
                    add(preset.name.lowercase().replace(" ", ""))
                    addAll(preset.tags.map { it.lowercase().replace(" ", "") })
                }
                keywords.any { it.contains(searchTerm) }
            }
    }

    override suspend fun uploadPreset(preset: MarketPreset): Result<String> = runCatching {
        presetRemoteDataSource.savePreset(preset)
    }

    override suspend fun uploadImage(localUri: String, storagePath: String, maxWidth: Int, quality: Int): Result<String> =
        runCatching {
            val bytes = if (localUri.startsWith("/")) {
                ImageCompressor.compressToWebP(File(localUri), maxWidth, quality)
            } else {
                ImageCompressor.compressToWebP(context, localUri.toUri(), maxWidth, quality)
            }
            storageDataSource.uploadBytes(bytes, storagePath)
        }

    override suspend fun uploadImageGetPath(localUri: String, storagePath: String, maxWidth: Int, quality: Int): Result<String> =
        runCatching {
            val bytes = if (localUri.startsWith("/")) {
                ImageCompressor.compressToWebP(File(localUri), maxWidth, quality)
            } else {
                ImageCompressor.compressToWebP(context, localUri.toUri(), maxWidth, quality)
            }
            storageDataSource.uploadBytesGetPath(bytes, storagePath)
        }

    override suspend fun uploadPreviewImage(
        localUri: String,
        storagePath: String,
        maxWidth: Int,
        quality: Int,
        maxSizeBytes: Long,
    ): Result<String> = runCatching {
        val uri = localUri.toUri()
        if (AnimationDetector.isAnimated(context, uri)) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("이미지를 열 수 없습니다: $localUri")
            require(bytes.size <= maxSizeBytes) {
                "이미지 크기가 ${maxSizeBytes / (1024 * 1024)}MB를 초과합니다"
            }
            val mimeType = context.contentResolver.getType(uri) ?: "image/webp"
            val contentType = if (mimeType == "image/gif") "image/gif" else "image/webp"
            val finalPath = if (contentType == "image/gif") storagePath.replace(".webp", ".gif") else storagePath
            storageDataSource.uploadBytes(bytes, finalPath, contentType)
        } else {
            val bytes = if (localUri.startsWith("/")) {
                ImageCompressor.compressToWebP(File(localUri), maxWidth, quality)
            } else {
                ImageCompressor.compressToWebP(context, uri, maxWidth, quality)
            }
            storageDataSource.uploadBytes(bytes, storagePath)
        }
    }

    override suspend fun uploadSlpFile(localPath: String, storagePath: String): Result<String> =
        runCatching {
            storageDataSource.uploadFile(localPath, storagePath)
        }

    override suspend fun downloadImageToLocal(
        storageUrl: String,
        localPath: String,
    ): Result<String> = runCatching {
        storageDataSource.downloadToFile(storageUrl, localPath)
    }

    override suspend fun downloadSlpFile(storageUrl: String, localPath: String): Result<String> =
        runCatching {
            storageDataSource.downloadToFile(storageUrl, localPath)
        }

    override suspend fun toggleLike(presetId: String): Result<Boolean> = runCatching {
        val uid = authDataSource.getCurrentUserId() ?: error("로그인이 필요합니다")
        presetRemoteDataSource.toggleLike(presetId, uid)
    }

    override suspend fun incrementDownloadCount(presetId: String): Result<Unit> = runCatching {
        presetRemoteDataSource.incrementDownloadCount(presetId)
    }

    override suspend fun getPresetsByAuthor(uid: String): Result<List<MarketPreset>> = runCatching {
        presetRemoteDataSource.getPresetsByAuthor(uid)
    }

    override suspend fun deletePreset(presetId: String): Result<Unit> = runCatching {
        presetRemoteDataSource.softDeletePreset(presetId)
    }

    override suspend fun reportPreset(
        reporterUid: String,
        reporterDisplayName: String,
        presetId: String,
        presetName: String,
        presetAuthorUid: String,
        presetAuthorDisplayName: String,
        reason: String,
        imageUrl: String?,
    ): Result<Unit> = runCatching {
        val data = buildMap<String, Any> {
            put("reporterUid", reporterUid)
            put("reporterDisplayName", reporterDisplayName)
            put("presetId", presetId)
            put("presetName", presetName)
            put("presetAuthorUid", presetAuthorUid)
            put("presetAuthorDisplayName", presetAuthorDisplayName)
            put("reason", reason)
            put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
            put("status", "pending")
            imageUrl?.let { put("imageUrl", it) }
        }
        presetRemoteDataSource.reportPreset(data)
    }

    // RecentPresetsPagerProvider
    override fun provide(): Flow<PagingData<MarketPreset>> =
        Pager(
            config = PagingConfig(pageSize = 10, enablePlaceholders = false),
            pagingSourceFactory = { presetRemoteDataSource.createRecentPresetsPagingSource() },
        ).flow

    // SearchPresetsPagerProvider
    override fun provide(query: String): Flow<PagingData<MarketPreset>> =
        Pager(
            config = PagingConfig(pageSize = 10, enablePlaceholders = false),
            pagingSourceFactory = { presetRemoteDataSource.createSearchPresetsPagingSource(query) },
        ).flow
}
