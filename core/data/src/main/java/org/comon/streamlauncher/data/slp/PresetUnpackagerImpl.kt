package org.comon.streamlauncher.data.slp

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import org.comon.streamlauncher.domain.repository.PresetUnpackager
import org.comon.streamlauncher.domain.repository.UnpackedPresetResult
import java.io.File
import javax.inject.Inject

class PresetUnpackagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val marketRepository: MarketPresetRepository,
) : PresetUnpackager {

    override suspend fun downloadAndUnpack(marketPreset: MarketPreset): UnpackedPresetResult {
        val presetDir = File(context.filesDir, "market_presets/${marketPreset.id}")
        val cacheFile = File(context.cacheDir, "slp_download/${marketPreset.id}.slp")
        cacheFile.parentFile?.mkdirs()

        try {
            marketRepository.downloadSlpFile(marketPreset.slpStorageUrl!!, cacheFile.absolutePath).getOrThrow()
            val (manifest, extractedPaths) = SlpUnpacker.unpack(cacheFile, presetDir)
            val localPreset = manifest.toLocalPreset(extractedPaths, marketPreset.id)
            return UnpackedPresetResult(localPreset)
        } finally {
            cacheFile.delete()
        }
    }

    override suspend fun downloadLegacyImages(marketPreset: MarketPreset): UnpackedPresetResult {
        val presetDir = File(context.filesDir, "market_presets/${marketPreset.id}")
        presetDir.mkdirs()

        suspend fun downloadUrl(url: String?, filename: String): String? {
            url ?: return null
            val localPath = File(presetDir, filename).absolutePath
            return marketRepository.downloadImageToLocal(url, localPath).getOrNull()
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
            marketPresetId = marketPreset.id,
        )
        return UnpackedPresetResult(localPreset)
    }

    override fun cleanupPresetDir(marketPresetId: String) {
        File(context.filesDir, "market_presets/$marketPresetId").deleteRecursively()
    }
}
