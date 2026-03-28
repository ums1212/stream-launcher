package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.domain.model.preset.PresetOperationProgress
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import org.comon.streamlauncher.domain.repository.PresetRepository
import org.comon.streamlauncher.domain.repository.PresetUnpackager
import org.comon.streamlauncher.domain.util.WallpaperHelper
import java.io.IOException
import java.net.UnknownHostException
import javax.inject.Inject

class DownloadMarketPresetUseCase @Inject constructor(
    private val unpackager: PresetUnpackager,
    private val presetRepository: PresetRepository,
    private val marketRepository: MarketPresetRepository,
    private val saveGridCellImageUseCase: SaveGridCellImageUseCase,
    private val saveFeedSettingsUseCase: SaveFeedSettingsUseCase,
    private val saveAppDrawerSettingsUseCase: SaveAppDrawerSettingsUseCase,
    private val saveColorPresetUseCase: SaveColorPresetUseCase,
    private val wallpaperHelper: WallpaperHelper,
    private val setLiveWallpaperUseCase: SetLiveWallpaperUseCase,
) {
    suspend operator fun invoke(marketPreset: MarketPreset): Result<Unit> = runCatching {
        val result = unpackager.downloadAndUnpack(marketPreset)
        val localPreset = result.localPreset
        presetRepository.savePreset(localPreset)
        applySettings(localPreset)
        marketRepository.incrementDownloadCount(marketPreset.id)
    }

    /**
     * 런처 설정 즉시 적용.
     * @return 라이브 배경화면이 적용된 경우 해당 URI, 아니면 null
     */
    private suspend fun applySettings(localPreset: Preset): String? {
        if (localPreset.hasTopLeftImage) saveGridCellImageUseCase(GridCell.TOP_LEFT, localPreset.topLeftIdleUri, localPreset.topLeftExpandedUri)
        if (localPreset.hasTopRightImage) saveGridCellImageUseCase(GridCell.TOP_RIGHT, localPreset.topRightIdleUri, localPreset.topRightExpandedUri)
        if (localPreset.hasBottomLeftImage) saveGridCellImageUseCase(GridCell.BOTTOM_LEFT, localPreset.bottomLeftIdleUri, localPreset.bottomLeftExpandedUri)
        if (localPreset.hasBottomRightImage) saveGridCellImageUseCase(GridCell.BOTTOM_RIGHT, localPreset.bottomRightIdleUri, localPreset.bottomRightExpandedUri)
        if (localPreset.hasFeedSettings) saveFeedSettingsUseCase(localPreset.chzzkChannelId, localPreset.youtubeChannelId)
        if (localPreset.hasAppDrawerSettings) {
            saveAppDrawerSettingsUseCase(localPreset.appDrawerColumns, localPreset.appDrawerRows, localPreset.appDrawerIconSizeRatio)
        }
        if (localPreset.hasThemeSettings) localPreset.themeColorHex?.toIntOrNull()?.let { saveColorPresetUseCase(it) }
        if (localPreset.hasWallpaperSettings) {
            if (localPreset.isLiveWallpaper && localPreset.liveWallpaperUri != null) {
                setLiveWallpaperUseCase(null, localPreset.liveWallpaperUri, WallpaperOrientation.PORTRAIT)
            } else {
                localPreset.wallpaperUri?.let { wallpaperHelper.setWallpaperFromPreset(it) }
            }
            if (localPreset.isLiveWallpaperLandscape && localPreset.liveWallpaperLandscapeUri != null) {
                setLiveWallpaperUseCase(null, localPreset.liveWallpaperLandscapeUri, WallpaperOrientation.LANDSCAPE)
            }
            if (localPreset.isLiveWallpaper && localPreset.liveWallpaperUri != null) {
                return localPreset.liveWallpaperUri
            }
        }
        return null
    }

    fun downloadWithProgress(marketPreset: MarketPreset): Flow<PresetOperationProgress> = flow {
        val presetName = marketPreset.name
        val totalSteps = 3

        try {
            // Step 1: .slp 다운로드 + 추출
            val result = unpackager.downloadAndUnpack(marketPreset)
            emit(PresetOperationProgress(presetName, 1, totalSteps))

            // Step 2: 저장 + 설정 적용
            presetRepository.savePreset(result.localPreset)
            val appliedLiveWallpaperUri = applySettings(result.localPreset)
            emit(PresetOperationProgress(presetName, 2, totalSteps))

            // Step 3: 카운트 증가
            marketRepository.incrementDownloadCount(marketPreset.id)
            emit(PresetOperationProgress(presetName, totalSteps, totalSteps, isCompleted = true, liveWallpaperUri = appliedLiveWallpaperUri))
        } catch (e: UnknownHostException) {
            unpackager.cleanupPresetDir(marketPreset.id)
            emit(PresetOperationProgress(presetName, 0, totalSteps, error = e))
        } catch (e: IOException) {
            unpackager.cleanupPresetDir(marketPreset.id)
            emit(PresetOperationProgress(presetName, 0, totalSteps, error = e))
        } catch (e: Exception) {
            unpackager.cleanupPresetDir(marketPreset.id)
            emit(PresetOperationProgress(presetName, 0, totalSteps, error = e))
        }
    }
}
