package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
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

        // 로컬 URI(content:// 또는 file path)를 Storage에 업로드하고 download URL 반환.
        // 이미 https:// URL이면 그대로 사용 (재업로드 방지).
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

        // 프리뷰 이미지 업로드
        val previewUrls = previewUris.mapIndexedNotNull { i, uri ->
            repository.uploadImage(uri, "$basePath/preview_$i.webp").getOrNull()
        }

        // 썸네일: 첫 번째 프리뷰 → 없으면 첫 번째 홈 이미지
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
}
