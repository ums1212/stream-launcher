package org.comon.streamlauncher.data.slp

import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.Preset

/** SlpManifest ↔ 도메인 모델 변환 */

/**
 * 추출된 파일 경로 맵 + marketPresetId → 로컬 저장용 [Preset]
 *
 * @param extractedPaths  SlpUnpacker.unpack() 에서 반환된 "ZIP상대경로 → 절대경로" 맵
 * @param marketPresetId  연결할 마켓 프리셋 ID
 */
fun SlpManifest.toLocalPreset(
    extractedPaths: Map<String, String>,
    marketPresetId: String,
): Preset = Preset(
    name                  = name,
    hasTopLeftImage       = cellFlags.hasTopLeft,
    hasTopRightImage      = cellFlags.hasTopRight,
    hasBottomLeftImage    = cellFlags.hasBottomLeft,
    hasBottomRightImage   = cellFlags.hasBottomRight,
    topLeftIdleUri        = images.topLeftIdle?.let { extractedPaths[it] },
    topLeftExpandedUri    = images.topLeftExpanded?.let { extractedPaths[it] },
    topRightIdleUri       = images.topRightIdle?.let { extractedPaths[it] },
    topRightExpandedUri   = images.topRightExpanded?.let { extractedPaths[it] },
    bottomLeftIdleUri     = images.bottomLeftIdle?.let { extractedPaths[it] },
    bottomLeftExpandedUri = images.bottomLeftExpanded?.let { extractedPaths[it] },
    bottomRightIdleUri    = images.bottomRightIdle?.let { extractedPaths[it] },
    bottomRightExpandedUri= images.bottomRightExpanded?.let { extractedPaths[it] },
    hasFeedSettings       = feedSettings?.enabled ?: false,
    youtubeChannelId      = feedSettings?.youtubeChannelId ?: "",
    chzzkChannelId        = feedSettings?.chzzkChannelId ?: "",
    hasAppDrawerSettings  = appDrawerSettings?.enabled ?: false,
    appDrawerColumns      = appDrawerSettings?.columns ?: 4,
    appDrawerRows         = appDrawerSettings?.rows ?: 6,
    appDrawerIconSizeRatio= appDrawerSettings?.iconSizeRatio ?: 1.0f,
    hasWallpaperSettings  = wallpaperSettings?.enabled ?: false,
    wallpaperUri          = if (wallpaperSettings?.isLiveWallpaper == true) null
                            else images.wallpaper?.let { extractedPaths[it] },
    staticWallpaperLandscapeUri = if (wallpaperSettings?.isLiveWallpaperLandscape == true) null
                            else images.wallpaperLandscape?.let { extractedPaths[it] },
    hasThemeSettings      = themeSettings?.enabled ?: false,
    themeColorHex         = themeSettings?.colorHex,
    marketPresetId        = marketPresetId,
    isLiveWallpaper             = wallpaperSettings?.isLiveWallpaper ?: false,
    liveWallpaperUri            = if (wallpaperSettings?.isLiveWallpaper == true)
                                      images.wallpaper?.let { extractedPaths[it] }
                                  else null,
    isLiveWallpaperLandscape    = wallpaperSettings?.isLiveWallpaperLandscape ?: false,
    liveWallpaperLandscapeUri   = if (wallpaperSettings?.isLiveWallpaperLandscape == true)
                                      images.wallpaperLandscape?.let { extractedPaths[it] }
                                  else null,
)

/**
 * Firestore 문서용 [MarketPreset] 생성 (.slp 포맷, 개별 이미지 URL 없음)
 *
 * @param id            프리셋 ID
 * @param slpStorageUrl Firebase Storage .slp 파일 다운로드 URL
 * @param thumbnailUrl  썸네일 URL (별도 업로드)
 */
fun SlpManifest.toMarketPreset(
    id: String,
    slpStorageUrl: String,
    thumbnailUrl: String,
): MarketPreset = MarketPreset(
    id                    = id,
    authorUid             = authorUid,
    authorDisplayName     = authorDisplayName,
    name                  = name,
    description           = description,
    tags                  = tags,
    schemaVersion         = 2,
    slpStorageUrl         = slpStorageUrl,
    thumbnailUrl          = thumbnailUrl,
    previewImageUrls      = emptyList(),
    hasTopLeftImage       = cellFlags.hasTopLeft,
    hasTopRightImage      = cellFlags.hasTopRight,
    hasBottomLeftImage    = cellFlags.hasBottomLeft,
    hasBottomRightImage   = cellFlags.hasBottomRight,
    hasFeedSettings       = feedSettings?.enabled ?: false,
    hasAppDrawerSettings  = appDrawerSettings?.enabled ?: false,
    hasWallpaperSettings  = wallpaperSettings?.enabled ?: false,
    hasThemeSettings      = themeSettings?.enabled ?: false,
)
