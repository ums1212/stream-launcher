package org.comon.streamlauncher.data.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import org.comon.streamlauncher.domain.repository.PresetRepository
import org.comon.streamlauncher.domain.usecase.SaveAppDrawerSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.comon.streamlauncher.domain.usecase.SaveFeedSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
import java.io.File
import javax.inject.Inject

class DownloadMarketPresetUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val marketRepository: MarketPresetRepository,
    private val presetRepository: PresetRepository,
    private val saveGridCellImageUseCase: SaveGridCellImageUseCase,
    private val saveFeedSettingsUseCase: SaveFeedSettingsUseCase,
    private val saveAppDrawerSettingsUseCase: SaveAppDrawerSettingsUseCase,
    private val saveColorPresetUseCase: SaveColorPresetUseCase,
) {
    suspend operator fun invoke(marketPreset: MarketPreset): Result<Unit> = runCatching {
        val presetDir = File(context.filesDir, "market_presets/${marketPreset.id}")
        presetDir.mkdirs()

        // Storage URL → 로컬 파일 다운로드
        suspend fun downloadUrl(url: String?, filename: String): String? {
            url ?: return null
            val localPath = File(presetDir, filename).absolutePath
            return marketRepository.downloadImageToLocal(url, localPath)
                .getOrNull()
        }

        val topLeftIdleLocal = downloadUrl(marketPreset.topLeftIdleUrl, "top_left_idle.webp")
        val topLeftExpandedLocal = downloadUrl(marketPreset.topLeftExpandedUrl, "top_left_expanded.webp")
        val topRightIdleLocal = downloadUrl(marketPreset.topRightIdleUrl, "top_right_idle.webp")
        val topRightExpandedLocal = downloadUrl(marketPreset.topRightExpandedUrl, "top_right_expanded.webp")
        val bottomLeftIdleLocal = downloadUrl(marketPreset.bottomLeftIdleUrl, "bottom_left_idle.webp")
        val bottomLeftExpandedLocal = downloadUrl(marketPreset.bottomLeftExpandedUrl, "bottom_left_expanded.webp")
        val bottomRightIdleLocal = downloadUrl(marketPreset.bottomRightIdleUrl, "bottom_right_idle.webp")
        val bottomRightExpandedLocal = downloadUrl(marketPreset.bottomRightExpandedUrl, "bottom_right_expanded.webp")
        val wallpaperLocal = downloadUrl(marketPreset.wallpaperUrl, "wallpaper.webp")

        // 로컬 Preset 객체 생성 후 저장
        val localPreset = Preset(
            name = marketPreset.name,
            hasTopLeftImage = marketPreset.hasTopLeftImage,
            hasTopRightImage = marketPreset.hasTopRightImage,
            hasBottomLeftImage = marketPreset.hasBottomLeftImage,
            hasBottomRightImage = marketPreset.hasBottomRightImage,
            topLeftIdleUri = topLeftIdleLocal,
            topLeftExpandedUri = topLeftExpandedLocal,
            topRightIdleUri = topRightIdleLocal,
            topRightExpandedUri = topRightExpandedLocal,
            bottomLeftIdleUri = bottomLeftIdleLocal,
            bottomLeftExpandedUri = bottomLeftExpandedLocal,
            bottomRightIdleUri = bottomRightIdleLocal,
            bottomRightExpandedUri = bottomRightExpandedLocal,
            hasFeedSettings = marketPreset.hasFeedSettings,
            useFeed = marketPreset.useFeed,
            youtubeChannelId = marketPreset.youtubeChannelId,
            chzzkChannelId = marketPreset.chzzkChannelId,
            hasAppDrawerSettings = marketPreset.hasAppDrawerSettings,
            appDrawerColumns = marketPreset.appDrawerColumns,
            appDrawerRows = marketPreset.appDrawerRows,
            appDrawerIconSizeRatio = marketPreset.appDrawerIconSizeRatio,
            hasWallpaperSettings = marketPreset.hasWallpaperSettings,
            wallpaperUri = wallpaperLocal,
            enableParallax = marketPreset.enableParallax,
            hasThemeSettings = marketPreset.hasThemeSettings,
            themeColorHex = marketPreset.themeColorHex,
        )
        presetRepository.savePreset(localPreset)

        // 런처 설정 즉시 적용
        if (marketPreset.hasTopLeftImage) {
            saveGridCellImageUseCase(GridCell.TOP_LEFT, topLeftIdleLocal, topLeftExpandedLocal)
        }
        if (marketPreset.hasTopRightImage) {
            saveGridCellImageUseCase(GridCell.TOP_RIGHT, topRightIdleLocal, topRightExpandedLocal)
        }
        if (marketPreset.hasBottomLeftImage) {
            saveGridCellImageUseCase(GridCell.BOTTOM_LEFT, bottomLeftIdleLocal, bottomLeftExpandedLocal)
        }
        if (marketPreset.hasBottomRightImage) {
            saveGridCellImageUseCase(GridCell.BOTTOM_RIGHT, bottomRightIdleLocal, bottomRightExpandedLocal)
        }
        if (marketPreset.hasFeedSettings) {
            saveFeedSettingsUseCase(marketPreset.chzzkChannelId, marketPreset.youtubeChannelId)
        }
        if (marketPreset.hasAppDrawerSettings) {
            saveAppDrawerSettingsUseCase(
                marketPreset.appDrawerColumns,
                marketPreset.appDrawerRows,
                marketPreset.appDrawerIconSizeRatio,
            )
        }
        if (marketPreset.hasThemeSettings) {
            marketPreset.themeColorHex?.toIntOrNull()?.let { saveColorPresetUseCase(it) }
        }

        // 다운로드 카운트 증가
        marketRepository.incrementDownloadCount(marketPreset.id)
    }
}
