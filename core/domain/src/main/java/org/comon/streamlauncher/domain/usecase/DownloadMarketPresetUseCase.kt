package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.preset.DownloadProgress
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
) {
    suspend operator fun invoke(marketPreset: MarketPreset): Result<Unit> = runCatching {
        val result = unpackager.downloadAndUnpack(marketPreset)
        val localPreset = result.localPreset
        presetRepository.savePreset(localPreset)
        applySettings(localPreset)
        marketRepository.incrementDownloadCount(marketPreset.id)
    }

    /** 런처 설정 즉시 적용 */
    private suspend fun applySettings(localPreset: Preset) {
        if (localPreset.hasTopLeftImage) saveGridCellImageUseCase(GridCell.TOP_LEFT, localPreset.topLeftIdleUri, localPreset.topLeftExpandedUri)
        if (localPreset.hasTopRightImage) saveGridCellImageUseCase(GridCell.TOP_RIGHT, localPreset.topRightIdleUri, localPreset.topRightExpandedUri)
        if (localPreset.hasBottomLeftImage) saveGridCellImageUseCase(GridCell.BOTTOM_LEFT, localPreset.bottomLeftIdleUri, localPreset.bottomLeftExpandedUri)
        if (localPreset.hasBottomRightImage) saveGridCellImageUseCase(GridCell.BOTTOM_RIGHT, localPreset.bottomRightIdleUri, localPreset.bottomRightExpandedUri)
        if (localPreset.hasFeedSettings) saveFeedSettingsUseCase(localPreset.chzzkChannelId, localPreset.youtubeChannelId)
        if (localPreset.hasAppDrawerSettings) {
            saveAppDrawerSettingsUseCase(localPreset.appDrawerColumns, localPreset.appDrawerRows, localPreset.appDrawerIconSizeRatio)
        }
        if (localPreset.hasThemeSettings) localPreset.themeColorHex?.toIntOrNull()?.let { saveColorPresetUseCase(it) }
        if (localPreset.hasWallpaperSettings) localPreset.wallpaperUri?.let { wallpaperHelper.setWallpaperFromPreset(it) }
    }

    fun downloadWithProgress(marketPreset: MarketPreset): Flow<DownloadProgress> = flow {
        val presetName = marketPreset.name
        val totalSteps = 3

        try {
            // Step 1: .slp 다운로드 + 추출
            val result = unpackager.downloadAndUnpack(marketPreset)
            emit(DownloadProgress(presetName, 1, totalSteps))

            // Step 2: 저장 + 설정 적용
            presetRepository.savePreset(result.localPreset)
            applySettings(result.localPreset)
            emit(DownloadProgress(presetName, 2, totalSteps))

            // Step 3: 카운트 증가
            marketRepository.incrementDownloadCount(marketPreset.id)
            emit(DownloadProgress(presetName, totalSteps, totalSteps, isCompleted = true))
        } catch (_: UnknownHostException) {
            unpackager.cleanupPresetDir(marketPreset.id)
            emit(DownloadProgress(presetName, 0, totalSteps, error = "네트워크 연결 없음"))
        } catch (e: IOException) {
            unpackager.cleanupPresetDir(marketPreset.id)
            emit(DownloadProgress(presetName, 0, totalSteps, error = e.message ?: "IO 오류"))
        } catch (e: Exception) {
            unpackager.cleanupPresetDir(marketPreset.id)
            emit(DownloadProgress(presetName, 0, totalSteps, error = e.message ?: "알 수 없는 오류"))
        }
    }
}
