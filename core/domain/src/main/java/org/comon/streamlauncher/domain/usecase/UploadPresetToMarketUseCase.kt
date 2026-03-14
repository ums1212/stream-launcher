package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.UploadProgress
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
        preset: MarketPreset,
        previewUris: List<String> = emptyList(),
    ): Result<String> = runCatching {
        val uid = repository.getCurrentUser()?.uid ?: error("로그인이 필요합니다")
        val presetId = preset.id.ifEmpty { UUID.randomUUID().toString() }

        // 1. .slp 패킹
        val packed = packager.packPreset(preset, previewUris, presetId)

        try {
            // 2. .slp Storage 업로드
            val storagePath = "presets/$uid/$presetId.slp"
            val slpStorageUrl = repository.uploadSlpFile(packed.slpFilePath, storagePath).getOrThrow()

            // 3. 썸네일 업로드 (목록 표시용)
            val thumbnailUri = previewUris.firstOrNull()
                ?: listOfNotNull(
                    preset.topLeftIdleUrl, preset.topRightIdleUrl,
                    preset.bottomLeftIdleUrl, preset.bottomRightIdleUrl,
                ).firstOrNull { !it.startsWith("http") }
            val thumbnailUrl = thumbnailUri?.let {
                repository.uploadImage(it, "presets/$uid/$presetId/thumbnail.webp").getOrNull()
            } ?: ""

            // 4. 프리뷰 이미지 개별 업로드
            val previewImageUrls = previewUris.mapIndexedNotNull { index, uri ->
                repository.uploadImage(uri, "presets/$uid/$presetId/preview_$index.webp").getOrNull()
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
        preset: MarketPreset,
        previewUris: List<String> = emptyList(),
    ): Flow<UploadProgress> = flow {
        val presetName = preset.name
        val totalSteps = 3
        var completed = 0

        try {
            val uid = repository.getCurrentUser()?.uid ?: error("로그인이 필요합니다")
            val presetId = preset.id.ifEmpty { UUID.randomUUID().toString() }

            // Step 1: 패킹
            val packed = packager.packPreset(preset, previewUris, presetId)
            emit(UploadProgress(presetName, ++completed, totalSteps))

            try {
                // Step 2: .slp 업로드
                val storagePath = "presets/$uid/$presetId.slp"
                val slpStorageUrl = repository.uploadSlpFile(packed.slpFilePath, storagePath).getOrThrow()
                emit(UploadProgress(presetName, ++completed, totalSteps))

                // Step 3: 썸네일 업로드 + Firestore 문서
                val thumbnailUri = previewUris.firstOrNull()
                    ?: listOfNotNull(
                        preset.topLeftIdleUrl, preset.topRightIdleUrl,
                        preset.bottomLeftIdleUrl, preset.bottomRightIdleUrl,
                    ).firstOrNull { !it.startsWith("http") }
                val thumbnailUrl = thumbnailUri?.let {
                    repository.uploadImage(it, "presets/$uid/$presetId/thumbnail.webp").getOrNull()
                } ?: ""

                // 프리뷰 이미지 개별 업로드
                val previewImageUrls = previewUris.mapIndexedNotNull { index, uri ->
                    repository.uploadImage(uri, "presets/$uid/$presetId/preview_$index.webp").getOrNull()
                }

                val finalPreset = packed.presetTemplate.copy(
                    slpStorageUrl = slpStorageUrl,
                    thumbnailUrl = thumbnailUrl,
                    previewImageUrls = previewImageUrls,
                )
                repository.uploadPreset(finalPreset).getOrThrow()

                emit(UploadProgress(presetName, totalSteps, totalSteps, isCompleted = true))
            } finally {
                packager.deleteTempFile(packed.slpFilePath)
            }
        } catch (e: UnknownHostException) {
            emit(UploadProgress(presetName, completed, totalSteps, error = "네트워크 연결을 확인하세요"))
        } catch (e: IOException) {
            emit(UploadProgress(presetName, completed, totalSteps, error = "네트워크 연결을 확인하세요"))
        } catch (e: Exception) {
            emit(UploadProgress(presetName, completed, totalSteps, error = e.message ?: "업로드 실패"))
        }
    }
}
