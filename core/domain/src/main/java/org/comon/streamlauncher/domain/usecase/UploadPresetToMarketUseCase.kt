package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.UploadProgress
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import java.io.IOException
import java.net.UnknownHostException
import java.util.UUID
import javax.inject.Inject

class UploadPresetToMarketUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    suspend operator fun invoke(
        preset: MarketPreset,
        previewUris: List<String> = emptyList(),
    ): Result<String> = runCatching {
        val uid = repository.getCurrentUser()?.uid ?: error("로그인이 필요합니다")
        val presetId = preset.id.ifEmpty { UUID.randomUUID().toString() }
        val basePath = "preset_images/$uid/$presetId"

        suspend fun uploadIfLocal(uri: String?, filename: String): String? {
            if (uri == null) return null
            if (uri.startsWith("http")) return uri
            return repository.uploadImage(uri, "$basePath/$filename").getOrNull()
        }

        val topLeftIdleUrl      = uploadIfLocal(preset.topLeftIdleUrl,      "top_left_idle.webp")
        val topLeftExpandedUrl  = uploadIfLocal(preset.topLeftExpandedUrl,  "top_left_expanded.webp")
        val topRightIdleUrl     = uploadIfLocal(preset.topRightIdleUrl,     "top_right_idle.webp")
        val topRightExpandedUrl = uploadIfLocal(preset.topRightExpandedUrl, "top_right_expanded.webp")
        val bottomLeftIdleUrl      = uploadIfLocal(preset.bottomLeftIdleUrl,      "bottom_left_idle.webp")
        val bottomLeftExpandedUrl  = uploadIfLocal(preset.bottomLeftExpandedUrl,  "bottom_left_expanded.webp")
        val bottomRightIdleUrl     = uploadIfLocal(preset.bottomRightIdleUrl,     "bottom_right_idle.webp")
        val bottomRightExpandedUrl = uploadIfLocal(preset.bottomRightExpandedUrl, "bottom_right_expanded.webp")
        val wallpaperUrl = uploadIfLocal(preset.wallpaperUrl, "wallpaper.webp")

        val previewUrls = previewUris.mapIndexedNotNull { i, uri ->
            repository.uploadImage(uri, "$basePath/preview_$i.webp").getOrNull()
        }

        val thumbnailUrl = previewUrls.firstOrNull()
            ?: topLeftIdleUrl ?: topRightIdleUrl
            ?: bottomLeftIdleUrl ?: bottomRightIdleUrl
            ?: ""

        val finalPreset = preset.copy(
            id = presetId,
            topLeftIdleUrl      = topLeftIdleUrl,
            topLeftExpandedUrl  = topLeftExpandedUrl,
            topRightIdleUrl     = topRightIdleUrl,
            topRightExpandedUrl = topRightExpandedUrl,
            bottomLeftIdleUrl      = bottomLeftIdleUrl,
            bottomLeftExpandedUrl  = bottomLeftExpandedUrl,
            bottomRightIdleUrl     = bottomRightIdleUrl,
            bottomRightExpandedUrl = bottomRightExpandedUrl,
            wallpaperUrl        = wallpaperUrl,
            previewImageUrls    = previewUrls,
            thumbnailUrl        = thumbnailUrl,
        )

        repository.uploadPreset(finalPreset).getOrThrow()
    }

    fun uploadWithProgress(
        preset: MarketPreset,
        previewUris: List<String> = emptyList(),
    ): Flow<UploadProgress> = flow {
        val presetName = preset.name

        // 실제 업로드가 필요한 로컬 이미지 수 계산 (null이 아니고 http가 아닌 것)
        val localImageCount = listOf(
            preset.topLeftIdleUrl,
            preset.topLeftExpandedUrl,
            preset.topRightIdleUrl,
            preset.topRightExpandedUrl,
            preset.bottomLeftIdleUrl,
            preset.bottomLeftExpandedUrl,
            preset.bottomRightIdleUrl,
            preset.bottomRightExpandedUrl,
            preset.wallpaperUrl,
        ).count { uri -> uri != null && !uri.startsWith("http") }

        // totalSteps: 로컬 이미지 + 프리뷰 이미지 + 1 (Firestore)
        val totalSteps = localImageCount + previewUris.size + 1
        var completed = 0

        try {
            val uid = repository.getCurrentUser()?.uid ?: error("로그인이 필요합니다")
            val presetId = preset.id.ifEmpty { UUID.randomUUID().toString() }
            val basePath = "preset_images/$uid/$presetId"

            suspend fun uploadIfLocal(uri: String?, filename: String): String? {
                if (uri == null) return null
                if (uri.startsWith("http")) return uri
                val result = repository.uploadImage(uri, "$basePath/$filename").getOrNull()
                emit(UploadProgress(presetName, ++completed, totalSteps))
                return result
            }

            val topLeftIdleUrl      = uploadIfLocal(preset.topLeftIdleUrl,      "top_left_idle.webp")
            val topLeftExpandedUrl  = uploadIfLocal(preset.topLeftExpandedUrl,  "top_left_expanded.webp")
            val topRightIdleUrl     = uploadIfLocal(preset.topRightIdleUrl,     "top_right_idle.webp")
            val topRightExpandedUrl = uploadIfLocal(preset.topRightExpandedUrl, "top_right_expanded.webp")
            val bottomLeftIdleUrl      = uploadIfLocal(preset.bottomLeftIdleUrl,      "bottom_left_idle.webp")
            val bottomLeftExpandedUrl  = uploadIfLocal(preset.bottomLeftExpandedUrl,  "bottom_left_expanded.webp")
            val bottomRightIdleUrl     = uploadIfLocal(preset.bottomRightIdleUrl,     "bottom_right_idle.webp")
            val bottomRightExpandedUrl = uploadIfLocal(preset.bottomRightExpandedUrl, "bottom_right_expanded.webp")
            val wallpaperUrl = uploadIfLocal(preset.wallpaperUrl, "wallpaper.webp")

            val previewUrls = previewUris.mapIndexedNotNull { i, uri ->
                val result = repository.uploadImage(uri, "$basePath/preview_$i.webp").getOrNull()
                emit(UploadProgress(presetName, ++completed, totalSteps))
                result
            }

            val thumbnailUrl = previewUrls.firstOrNull()
                ?: topLeftIdleUrl ?: topRightIdleUrl
                ?: bottomLeftIdleUrl ?: bottomRightIdleUrl
                ?: ""

            val finalPreset = preset.copy(
                id = presetId,
                topLeftIdleUrl      = topLeftIdleUrl,
                topLeftExpandedUrl  = topLeftExpandedUrl,
                topRightIdleUrl     = topRightIdleUrl,
                topRightExpandedUrl = topRightExpandedUrl,
                bottomLeftIdleUrl      = bottomLeftIdleUrl,
                bottomLeftExpandedUrl  = bottomLeftExpandedUrl,
                bottomRightIdleUrl     = bottomRightIdleUrl,
                bottomRightExpandedUrl = bottomRightExpandedUrl,
                wallpaperUrl        = wallpaperUrl,
                previewImageUrls    = previewUrls,
                thumbnailUrl        = thumbnailUrl,
            )

            repository.uploadPreset(finalPreset).getOrThrow()
            emit(UploadProgress(presetName, totalSteps, totalSteps, isCompleted = true))
        } catch (e: UnknownHostException) {
            emit(UploadProgress(presetName, completed, totalSteps, error = "네트워크 연결을 확인하세요"))
        } catch (e: IOException) {
            emit(UploadProgress(presetName, completed, totalSteps, error = "네트워크 연결을 확인하세요"))
        } catch (e: Exception) {
            emit(UploadProgress(presetName, completed, totalSteps, error = e.message ?: "업로드 실패"))
        }
    }
}
