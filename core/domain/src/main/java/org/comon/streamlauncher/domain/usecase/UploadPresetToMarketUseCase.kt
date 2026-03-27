package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.model.preset.PresetOperationProgress
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import org.comon.streamlauncher.domain.repository.PresetPackager
import java.io.IOException
import java.net.UnknownHostException
import java.util.UUID
import javax.inject.Inject

/**
 * 프리셋을 단일 .slp 파일로 패킹하여 Firebase Storage 업로드 → Firestore 문서 생성
 *
 * schemaVersion = 2, 개별 이미지 URL 없음, slpStorageUrl 만 사용
 */
class UploadPresetToMarketUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
    private val packager: PresetPackager,
) {
    suspend operator fun invoke(
        localPreset: Preset,
        previewUris: List<String> = emptyList(),
        description: String = "",
        tags: List<String> = emptyList(),
    ): Result<String> = runCatching {
        val user = repository.getCurrentUser() ?: error("로그인이 필요합니다")
        val uid = user.uid
        val presetId = UUID.randomUUID().toString()

        // 1. .slp 패킹
        val packed = packager.packPreset(localPreset, previewUris, presetId, description, tags, uid, user.displayName)

        try {
            // 2. .slp Storage 업로드
            val storagePath = "presets/$uid/$presetId.slp"
            val slpStorageUrl = repository.uploadSlpFile(packed.slpFilePath, storagePath).getOrThrow()

            // 3. 썸네일 업로드 (목록 표시용)
            val thumbnailUrl = previewUris.firstOrNull()?.let {
                repository.uploadImage(it, "presets/$uid/$presetId/thumbnail.webp", maxWidth = 480, quality = 70).getOrNull()
            } ?: ""

            // 4. 프리뷰 이미지 개별 업로드 (애니메이션 GIF/WebP는 원본 유지)
            val previewImageUrls = previewUris.mapIndexedNotNull { index, uri ->
                repository.uploadPreviewImage(uri, "presets/$uid/$presetId/preview_$index.webp", maxWidth = 720, quality = 70).getOrNull()
            }

            // 5. Firestore 문서 생성
            val finalPreset = packed.presetTemplate.copy(
                slpStorageUrl = slpStorageUrl,
                thumbnailUrl = thumbnailUrl,
                previewImageUrls = previewImageUrls,
            )
            repository.uploadPreset(finalPreset).getOrThrow()
        } finally {
            packager.deleteTempFile(packed.slpFilePath)
        }
    }

    fun uploadWithProgress(
        localPreset: Preset,
        previewUris: List<String> = emptyList(),
        description: String = "",
        tags: List<String> = emptyList(),
    ): Flow<PresetOperationProgress> = flow {
        val presetName = localPreset.name
        val totalSteps = 3
        var completed = 0

        try {
            val user = repository.getCurrentUser() ?: error("로그인이 필요합니다")
            val uid = user.uid
            val presetId = UUID.randomUUID().toString()

            // Step 1: 패킹
            val packed = packager.packPreset(localPreset, previewUris, presetId, description, tags, uid, user.displayName)
            emit(PresetOperationProgress(presetName, ++completed, totalSteps))

            try {
                // Step 2: .slp 업로드
                val storagePath = "presets/$uid/$presetId.slp"
                val slpStorageUrl = repository.uploadSlpFile(packed.slpFilePath, storagePath).getOrThrow()
                emit(PresetOperationProgress(presetName, ++completed, totalSteps))

                // Step 3: 썸네일 업로드 + Firestore 문서
                val thumbnailUrl = previewUris.firstOrNull()?.let {
                    repository.uploadImage(it, "presets/$uid/$presetId/thumbnail.webp", maxWidth = 480, quality = 70).getOrNull()
                } ?: ""

                // 프리뷰 이미지 개별 업로드 (애니메이션 GIF/WebP는 원본 유지)
                val previewImageUrls = previewUris.mapIndexedNotNull { index, uri ->
                    repository.uploadPreviewImage(uri, "presets/$uid/$presetId/preview_$index.webp", maxWidth = 720, quality = 70).getOrNull()
                }

                val finalPreset = packed.presetTemplate.copy(
                    slpStorageUrl = slpStorageUrl,
                    thumbnailUrl = thumbnailUrl,
                    previewImageUrls = previewImageUrls,
                )
                repository.uploadPreset(finalPreset).getOrThrow()

                emit(PresetOperationProgress(presetName, totalSteps, totalSteps, isCompleted = true, marketPresetId = presetId))
            } finally {
                packager.deleteTempFile(packed.slpFilePath)
            }
        } catch (e: UnknownHostException) {
            emit(PresetOperationProgress(presetName, completed, totalSteps, error = e))
        } catch (e: IOException) {
            emit(PresetOperationProgress(presetName, completed, totalSteps, error = e))
        } catch (e: Exception) {
            emit(PresetOperationProgress(presetName, completed, totalSteps, error = e))
        }
    }
}
