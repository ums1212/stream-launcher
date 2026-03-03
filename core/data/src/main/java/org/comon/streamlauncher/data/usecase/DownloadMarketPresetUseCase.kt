package org.comon.streamlauncher.data.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.preset.DownloadProgress
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import org.comon.streamlauncher.domain.repository.PresetRepository
import org.comon.streamlauncher.domain.usecase.SaveAppDrawerSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveColorPresetUseCase
import org.comon.streamlauncher.domain.usecase.SaveFeedSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveGridCellImageUseCase
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
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

    fun downloadWithProgress(marketPreset: MarketPreset): Flow<DownloadProgress> = flow {
        val presetDir = File(context.filesDir, "market_presets/${marketPreset.id}")
        presetDir.mkdirs()

        val imageEntries = listOfNotNull(
            marketPreset.topLeftIdleUrl?.let { it to "top_left_idle.webp" },
            marketPreset.topLeftExpandedUrl?.let { it to "top_left_expanded.webp" },
            marketPreset.topRightIdleUrl?.let { it to "top_right_idle.webp" },
            marketPreset.topRightExpandedUrl?.let { it to "top_right_expanded.webp" },
            marketPreset.bottomLeftIdleUrl?.let { it to "bottom_left_idle.webp" },
            marketPreset.bottomLeftExpandedUrl?.let { it to "bottom_left_expanded.webp" },
            marketPreset.bottomRightIdleUrl?.let { it to "bottom_right_idle.webp" },
            marketPreset.bottomRightExpandedUrl?.let { it to "bottom_right_expanded.webp" },
            marketPreset.wallpaperUrl?.let { it to "wallpaper.webp" },
        )
        // 이미지 수 + 설정 적용(1) + Firestore increment(1)
        val totalSteps = imageEntries.size + 2
        val presetName = marketPreset.name

        try {
            val localPaths = mutableMapOf<String, String?>()
            imageEntries.forEachIndexed { index, (url, filename) ->
                val localPath = File(presetDir, filename).absolutePath
                val result = marketRepository.downloadImageToLocal(url, localPath)
                if (result.isFailure) {
                    presetDir.deleteRecursively()
                    emit(
                        DownloadProgress(
                            presetName = presetName,
                            currentStep = index + 1,
                            totalSteps = totalSteps,
                            error = result.exceptionOrNull()?.message ?: "이미지 다운로드 실패",
                        ),
                    )
                    return@flow
                }
                localPaths[filename] = result.getOrNull()
                emit(
                    DownloadProgress(
                        presetName = presetName,
                        currentStep = index + 1,
                        totalSteps = totalSteps,
                    ),
                )
            }

            // 로컬 Preset 저장 + 설정 적용
            val localPreset = Preset(
                name = presetName,
                hasTopLeftImage = marketPreset.hasTopLeftImage,
                hasTopRightImage = marketPreset.hasTopRightImage,
                hasBottomLeftImage = marketPreset.hasBottomLeftImage,
                hasBottomRightImage = marketPreset.hasBottomRightImage,
                topLeftIdleUri = localPaths["top_left_idle.webp"],
                topLeftExpandedUri = localPaths["top_left_expanded.webp"],
                topRightIdleUri = localPaths["top_right_idle.webp"],
                topRightExpandedUri = localPaths["top_right_expanded.webp"],
                bottomLeftIdleUri = localPaths["bottom_left_idle.webp"],
                bottomLeftExpandedUri = localPaths["bottom_left_expanded.webp"],
                bottomRightIdleUri = localPaths["bottom_right_idle.webp"],
                bottomRightExpandedUri = localPaths["bottom_right_expanded.webp"],
                hasFeedSettings = marketPreset.hasFeedSettings,
                useFeed = marketPreset.useFeed,
                youtubeChannelId = marketPreset.youtubeChannelId,
                chzzkChannelId = marketPreset.chzzkChannelId,
                hasAppDrawerSettings = marketPreset.hasAppDrawerSettings,
                appDrawerColumns = marketPreset.appDrawerColumns,
                appDrawerRows = marketPreset.appDrawerRows,
                appDrawerIconSizeRatio = marketPreset.appDrawerIconSizeRatio,
                hasWallpaperSettings = marketPreset.hasWallpaperSettings,
                wallpaperUri = localPaths["wallpaper.webp"],
                enableParallax = marketPreset.enableParallax,
                hasThemeSettings = marketPreset.hasThemeSettings,
                themeColorHex = marketPreset.themeColorHex,
            )
            presetRepository.savePreset(localPreset)

            if (marketPreset.hasTopLeftImage) {
                saveGridCellImageUseCase(GridCell.TOP_LEFT, localPaths["top_left_idle.webp"], localPaths["top_left_expanded.webp"])
            }
            if (marketPreset.hasTopRightImage) {
                saveGridCellImageUseCase(GridCell.TOP_RIGHT, localPaths["top_right_idle.webp"], localPaths["top_right_expanded.webp"])
            }
            if (marketPreset.hasBottomLeftImage) {
                saveGridCellImageUseCase(GridCell.BOTTOM_LEFT, localPaths["bottom_left_idle.webp"], localPaths["bottom_left_expanded.webp"])
            }
            if (marketPreset.hasBottomRightImage) {
                saveGridCellImageUseCase(GridCell.BOTTOM_RIGHT, localPaths["bottom_right_idle.webp"], localPaths["bottom_right_expanded.webp"])
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

            emit(
                DownloadProgress(
                    presetName = presetName,
                    currentStep = totalSteps - 1,
                    totalSteps = totalSteps,
                ),
            )

            // 다운로드 카운트 증가
            marketRepository.incrementDownloadCount(marketPreset.id)

            emit(
                DownloadProgress(
                    presetName = presetName,
                    currentStep = totalSteps,
                    totalSteps = totalSteps,
                    isCompleted = true,
                ),
            )
        } catch (e: UnknownHostException) {
            presetDir.deleteRecursively()
            emit(DownloadProgress(presetName = presetName, currentStep = 0, totalSteps = totalSteps, error = "네트워크 연결 없음"))
        } catch (e: IOException) {
            presetDir.deleteRecursively()
            emit(DownloadProgress(presetName = presetName, currentStep = 0, totalSteps = totalSteps, error = e.message ?: "IO 오류"))
        } catch (e: Exception) {
            presetDir.deleteRecursively()
            emit(DownloadProgress(presetName = presetName, currentStep = 0, totalSteps = totalSteps, error = e.message ?: "알 수 없는 오류"))
        }
    }
}
