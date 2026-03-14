package org.comon.streamlauncher.data.slp

import android.content.Context
import androidx.core.net.toUri
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.comon.streamlauncher.data.util.ImageCompressor
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * MarketPreset(로컬 URI 포함) + 프리뷰 URI 목록 → 단일 .slp 파일 생성
 *
 * .slp 구조:
 *   manifest.json  (DEFLATED)
 *   images/        (각 이미지 STORED - WebP는 이미 압축됨)
 *   previews/
 */
object SlpPacker {

    /**
     * @param context  ContentResolver 접근용
     * @param preset   로컬 URI/경로가 담긴 MarketPreset
     * @param previewUris  프리뷰 이미지 URI 목록
     * @param outDir   .slp 파일을 저장할 디렉토리 (보통 cacheDir/slp_temp)
     * @param presetId 파일명 및 manifest authorUid 용
     * @return 생성된 .slp File
     */
    fun pack(
        context: Context,
        preset: MarketPreset,
        previewUris: List<String>,
        outDir: File,
        presetId: String,
    ): File {
        outDir.mkdirs()
        val outFile = File(outDir, "$presetId.slp")

        val imageEntries = buildImageEntries(preset)
        val previewEntries = previewUris.mapIndexed { i, uri -> "previews/preview_$i.webp" to uri }

        val manifest = buildManifest(preset, previewUris)

        ZipOutputStream(outFile.outputStream().buffered()).use { zos ->
            // manifest.json — DEFLATED
            val manifestBytes = Json.encodeToString(manifest).toByteArray(Charsets.UTF_8)
            zos.putNextEntry(ZipEntry("manifest.json").apply { method = ZipEntry.DEFLATED })
            zos.write(manifestBytes)
            zos.closeEntry()

            // 이미지 — STORED (WebP 재압축 방지)
            (imageEntries + previewEntries).forEach { (entryPath, localUri) ->
                val bytes = compressUri(context, localUri)
                if (bytes.isEmpty()) return@forEach
                val crc = CRC32().also { it.update(bytes) }
                val entry = ZipEntry(entryPath).apply {
                    method = ZipEntry.STORED
                    size = bytes.size.toLong()
                    compressedSize = bytes.size.toLong()
                    this.crc = crc.value
                }
                zos.putNextEntry(entry)
                zos.write(bytes)
                zos.closeEntry()
            }
        }

        return outFile
    }

    /**
     * [pack] 에서 내부적으로 사용하는 manifest 빌드 로직을 외부에서도 쓸 수 있도록 공개
     * (PresetPackagerImpl 에서 toMarketPreset 변환용으로 사용)
     */
    fun buildManifest(preset: MarketPreset, previewUris: List<String>): SlpManifest {
        val previewPaths = previewUris.mapIndexed { i, _ -> "previews/preview_$i.webp" }
        return SlpManifest(
            name              = preset.name,
            description       = preset.description,
            tags              = preset.tags,
            authorUid         = preset.authorUid,
            authorDisplayName = preset.authorDisplayName,
            images = SlpImagePaths(
                topLeftIdle         = if (preset.topLeftIdleUrl.isLocalUri()) "images/top_left_idle.webp" else null,
                topLeftExpanded     = if (preset.topLeftExpandedUrl.isLocalUri()) "images/top_left_expanded.webp" else null,
                topRightIdle        = if (preset.topRightIdleUrl.isLocalUri()) "images/top_right_idle.webp" else null,
                topRightExpanded    = if (preset.topRightExpandedUrl.isLocalUri()) "images/top_right_expanded.webp" else null,
                bottomLeftIdle      = if (preset.bottomLeftIdleUrl.isLocalUri()) "images/bottom_left_idle.webp" else null,
                bottomLeftExpanded  = if (preset.bottomLeftExpandedUrl.isLocalUri()) "images/bottom_left_expanded.webp" else null,
                bottomRightIdle     = if (preset.bottomRightIdleUrl.isLocalUri()) "images/bottom_right_idle.webp" else null,
                bottomRightExpanded = if (preset.bottomRightExpandedUrl.isLocalUri()) "images/bottom_right_expanded.webp" else null,
                wallpaper           = if (preset.wallpaperUrl.isLocalUri()) "images/wallpaper.webp" else null,
            ),
            previews  = previewPaths,
            cellFlags = SlpCellFlags(
                hasTopLeft     = preset.hasTopLeftImage,
                hasTopRight    = preset.hasTopRightImage,
                hasBottomLeft  = preset.hasBottomLeftImage,
                hasBottomRight = preset.hasBottomRightImage,
            ),
            feedSettings = if (preset.hasFeedSettings) SlpFeedSettings(
                enabled          = true,
                useFeed          = preset.useFeed,
                youtubeChannelId = preset.youtubeChannelId,
                chzzkChannelId   = preset.chzzkChannelId,
            ) else null,
            appDrawerSettings = if (preset.hasAppDrawerSettings) SlpAppDrawerSettings(
                enabled       = true,
                columns       = preset.appDrawerColumns,
                rows          = preset.appDrawerRows,
                iconSizeRatio = preset.appDrawerIconSizeRatio,
            ) else null,
            wallpaperSettings = if (preset.hasWallpaperSettings) SlpWallpaperSettings(
                enabled        = true,
                enableParallax = preset.enableParallax,
            ) else null,
            themeSettings = if (preset.hasThemeSettings) SlpThemeSettings(
                enabled  = true,
                colorHex = preset.themeColorHex,
            ) else null,
        )
    }

    // ---------- internal ----------

    private fun buildImageEntries(preset: MarketPreset): List<Pair<String, String>> =
        listOfNotNull(
            preset.topLeftIdleUrl?.takeIf { it.isLocalUri() }?.let { "images/top_left_idle.webp" to it },
            preset.topLeftExpandedUrl?.takeIf { it.isLocalUri() }?.let { "images/top_left_expanded.webp" to it },
            preset.topRightIdleUrl?.takeIf { it.isLocalUri() }?.let { "images/top_right_idle.webp" to it },
            preset.topRightExpandedUrl?.takeIf { it.isLocalUri() }?.let { "images/top_right_expanded.webp" to it },
            preset.bottomLeftIdleUrl?.takeIf { it.isLocalUri() }?.let { "images/bottom_left_idle.webp" to it },
            preset.bottomLeftExpandedUrl?.takeIf { it.isLocalUri() }?.let { "images/bottom_left_expanded.webp" to it },
            preset.bottomRightIdleUrl?.takeIf { it.isLocalUri() }?.let { "images/bottom_right_idle.webp" to it },
            preset.bottomRightExpandedUrl?.takeIf { it.isLocalUri() }?.let { "images/bottom_right_expanded.webp" to it },
            preset.wallpaperUrl?.takeIf { it.isLocalUri() }?.let { "images/wallpaper.webp" to it },
        )

    private fun compressUri(context: Context, uri: String): ByteArray =
        if (uri.startsWith("/")) {
            ImageCompressor.compressToWebP(File(uri))
        } else {
            ImageCompressor.compressToWebP(context, uri.toUri())
        }

    /** null 또는 http(s):// 이면 로컬 아님 */
    private fun String?.isLocalUri(): Boolean = this != null && !this.startsWith("http")
}
