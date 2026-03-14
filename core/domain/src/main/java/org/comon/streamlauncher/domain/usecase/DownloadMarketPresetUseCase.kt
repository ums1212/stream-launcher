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
) {
    suspend operator fun invoke(marketPreset: MarketPreset): Result<Unit> = runCatching {
        if (marketPreset.slpStorageUrl != null) {
            invokeV2(marketPreset)
        } else {
            invokeV1(marketPreset)
        }
    }

    private suspend fun invokeV2(marketPreset: MarketPreset) {
        val result = unpackager.downloadAndUnpack(marketPreset)
        val localPreset = result.localPreset
        presetRepository.savePreset(localPreset)
        applySettings(marketPreset, localPreset)
        marketRepository.incrementDownloadCount(marketPreset.id)
    }

    private suspend fun invokeV1(marketPreset: MarketPreset) {
        val result = unpackager.downloadLegacyImages(marketPreset)
        val localPreset = result.localPreset
        presetRepository.savePreset(localPreset)
        applySettings(marketPreset, localPreset)
        marketRepository.incrementDownloadCount(marketPreset.id)
    }

    /** 런처 설정 즉시 적용 */
    private suspend fun applySettings(marketPreset: MarketPreset, localPreset: Preset) {
        if (marketPreset.hasTopLeftImage) saveGridCellImageUseCase(GridCell.TOP_LEFT, localPreset.topLeftIdleUri, localPreset.topLeftExpandedUri)
        if (marketPreset.hasTopRightImage) saveGridCellImageUseCase(GridCell.TOP_RIGHT, localPreset.topRightIdleUri, localPreset.topRightExpandedUri)
        if (marketPreset.hasBottomLeftImage) saveGridCellImageUseCase(GridCell.BOTTOM_LEFT, localPreset.bottomLeftIdleUri, localPreset.bottomLeftExpandedUri)
        if (marketPreset.hasBottomRightImage) saveGridCellImageUseCase(GridCell.BOTTOM_RIGHT, localPreset.bottomRightIdleUri, localPreset.bottomRightExpandedUri)
        if (marketPreset.hasFeedSettings) saveFeedSettingsUseCase(localPreset.chzzkChannelId, localPreset.youtubeChannelId)
        if (marketPreset.hasAppDrawerSettings) {
            saveAppDrawerSettingsUseCase(localPreset.appDrawerColumns, localPreset.appDrawerRows, localPreset.appDrawerIconSizeRatio)
        }
        if (marketPreset.hasThemeSettings) localPreset.themeColorHex?.toIntOrNull()?.let { saveColorPresetUseCase(it) }
    }

    fun downloadWithProgress(marketPreset: MarketPreset): Flow<DownloadProgress> = flow {
        if (marketPreset.slpStorageUrl != null) {
            downloadWithProgressV2(marketPreset)
        } else {
            downloadWithProgressV1(marketPreset)
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<DownloadProgress>.downloadWithProgressV2(
        marketPreset: MarketPreset,
    ) {
        val presetName = marketPreset.name
        val totalSteps = 3

        try {
            // Step 1: .slp 다운로드 + 추출
            val result = unpackager.downloadAndUnpack(marketPreset)
            emit(DownloadProgress(presetName, 1, totalSteps))

            // Step 2: 저장 + 설정 적용
            presetRepository.savePreset(result.localPreset)
            applySettings(marketPreset, result.localPreset)
            emit(DownloadProgress(presetName, 2, totalSteps))

            // Step 3: 카운트 증가
            marketRepository.incrementDownloadCount(marketPreset.id)
            emit(DownloadProgress(presetName, totalSteps, totalSteps, isCompleted = true))
        } catch (e: UnknownHostException) {
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

    private suspend fun kotlinx.coroutines.flow.FlowCollector<DownloadProgress>.downloadWithProgressV1(
        marketPreset: MarketPreset,
    ) {
        val presetName = marketPreset.name
        val totalSteps = 3

        try {
            // Step 1: 이미지 다운로드
            val result = unpackager.downloadLegacyImages(marketPreset)
            emit(DownloadProgress(presetName, 1, totalSteps))

            // Step 2: 저장 + 설정 적용
            presetRepository.savePreset(result.localPreset)
            applySettings(marketPreset, result.localPreset)
            emit(DownloadProgress(presetName, 2, totalSteps))

            // Step 3: 카운트 증가
            marketRepository.incrementDownloadCount(marketPreset.id)
            emit(DownloadProgress(presetName, totalSteps, totalSteps, isCompleted = true))
        } catch (e: UnknownHostException) {
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
