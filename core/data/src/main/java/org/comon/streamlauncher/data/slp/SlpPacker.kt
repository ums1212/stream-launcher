package org.comon.streamlauncher.data.slp

import android.content.Context
import androidx.core.net.toUri
import kotlinx.serialization.json.Json
import org.comon.streamlauncher.data.util.ImageCompressor
import org.comon.streamlauncher.domain.model.preset.Preset
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Preset(로컬 URI 포함) + 프리뷰 URI 목록 → 단일 .slp 파일 생성
 *
 * .slp 구조:
 *   manifest.json  (DEFLATED)
 *   images/        (각 이미지 STORED - WebP는 이미 압축됨)
 *   previews/
 */
object SlpPacker {

    /**
     * @param context  ContentResolver 접근용
     * @param preset   로컬 URI/경로가 담긴 Preset
     * @param previewUris  프리뷰 이미지 URI 목록
     * @param outDir   .slp 파일을 저장할 디렉토리 (보통 cacheDir/slp_temp)
     * @param presetId 파일명용
     * @param description  마켓 설명
     * @param tags         마켓 태그
     * @param authorUid    업로더 UID
     * @param authorDisplayName  업로더 표시 이름
     * @return 생성된 .slp File
     */
    fun pack(
        context: Context,
        preset: Preset,
        previewUris: List<String>,
        outDir: File,
        presetId: String,
        description: String = "",
        tags: List<String> = emptyList(),
        authorUid: String = "",
        authorDisplayName: String = "",
    ): File {
        outDir.mkdirs()
        val outFile = File(outDir, "$presetId.slp")

        val imageEntries = buildImageEntries(preset)
        val liveWallpaperEntries = buildLiveWallpaperEntries(preset)
        val previewEntries = previewUris.mapIndexed { i, uri -> "previews/preview_$i.webp" to uri }

        val manifest = buildManifest(preset, previewUris, description, tags, authorUid, authorDisplayName)

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

            // 라이브 배경화면(동영상/GIF) — 원본 바이트 그대로 STORED
            liveWallpaperEntries.forEach { (entryPath, localUri) ->
                val bytes = readRawFile(localUri)
                if (bytes.isNotEmpty()) {
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
        }

        return outFile
    }

    /**
     * [pack] 에서 내부적으로 사용하는 manifest 빌드 로직을 외부에서도 쓸 수 있도록 공개
     * (PresetPackagerImpl 에서 toMarketPreset 변환용으로 사용)
     */
    fun buildManifest(
        preset: Preset,
        previewUris: List<String>,
        description: String = "",
        tags: List<String> = emptyList(),
        authorUid: String = "",
        authorDisplayName: String = "",
    ): SlpManifest {
        val previewPaths = previewUris.mapIndexed { i, _ -> "previews/preview_$i.webp" }
        return SlpManifest(
            name              = preset.name,
            description       = description,
            tags              = tags,
            authorUid         = authorUid,
            authorDisplayName = authorDisplayName,
            images = SlpImagePaths(
                topLeftIdle         = preset.topLeftIdleUri?.takeIf { it.isLocalUri() }?.let { "images/top_left_idle.webp" },
                topLeftExpanded     = preset.topLeftExpandedUri?.takeIf { it.isLocalUri() }?.let { "images/top_left_expanded.webp" },
                topRightIdle        = preset.topRightIdleUri?.takeIf { it.isLocalUri() }?.let { "images/top_right_idle.webp" },
                topRightExpanded    = preset.topRightExpandedUri?.takeIf { it.isLocalUri() }?.let { "images/top_right_expanded.webp" },
                bottomLeftIdle      = preset.bottomLeftIdleUri?.takeIf { it.isLocalUri() }?.let { "images/bottom_left_idle.webp" },
                bottomLeftExpanded  = preset.bottomLeftExpandedUri?.takeIf { it.isLocalUri() }?.let { "images/bottom_left_expanded.webp" },
                bottomRightIdle     = preset.bottomRightIdleUri?.takeIf { it.isLocalUri() }?.let { "images/bottom_right_idle.webp" },
                bottomRightExpanded = preset.bottomRightExpandedUri?.takeIf { it.isLocalUri() }?.let { "images/bottom_right_expanded.webp" },
                wallpaper = when {
                    preset.isLiveWallpaper -> {
                        val lwUri = preset.liveWallpaperUri
                        if (lwUri?.isLocalUri() == true) {
                            "images/wallpaper.${lwUri.substringAfterLast('.', "mp4")}"
                        } else null
                    }
                    preset.wallpaperUri?.isLocalUri() == true -> "images/wallpaper.webp"
                    else -> null
                },
                wallpaperLandscape = if (preset.isLiveWallpaperLandscape) {
                    val lwUri = preset.liveWallpaperLandscapeUri
                    if (lwUri?.isLocalUri() == true) {
                        "images/wallpaper_landscape.${lwUri.substringAfterLast('.', "mp4")}"
                    } else null
                } else null,
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
                enabled                  = true,
                enableParallax           = preset.enableParallax,
                isLiveWallpaper          = preset.isLiveWallpaper,
                isLiveWallpaperLandscape = preset.isLiveWallpaperLandscape,
            ) else null,
            themeSettings = if (preset.hasThemeSettings) SlpThemeSettings(
                enabled  = true,
                colorHex = preset.themeColorHex,
            ) else null,
        )
    }

    // ---------- internal ----------

    private fun buildImageEntries(preset: Preset): List<Pair<String, String>> =
        listOfNotNull(
            preset.topLeftIdleUri?.takeIf { it.isLocalUri() }?.let { "images/top_left_idle.webp" to it },
            preset.topLeftExpandedUri?.takeIf { it.isLocalUri() }?.let { "images/top_left_expanded.webp" to it },
            preset.topRightIdleUri?.takeIf { it.isLocalUri() }?.let { "images/top_right_idle.webp" to it },
            preset.topRightExpandedUri?.takeIf { it.isLocalUri() }?.let { "images/top_right_expanded.webp" to it },
            preset.bottomLeftIdleUri?.takeIf { it.isLocalUri() }?.let { "images/bottom_left_idle.webp" to it },
            preset.bottomLeftExpandedUri?.takeIf { it.isLocalUri() }?.let { "images/bottom_left_expanded.webp" to it },
            preset.bottomRightIdleUri?.takeIf { it.isLocalUri() }?.let { "images/bottom_right_idle.webp" to it },
            preset.bottomRightExpandedUri?.takeIf { it.isLocalUri() }?.let { "images/bottom_right_expanded.webp" to it },
            // 라이브 배경화면이 아닐 때만 wallpaperUri 포함 (라이브는 buildLiveWallpaperEntry로 별도 처리)
            if (!preset.isLiveWallpaper) preset.wallpaperUri?.takeIf { it.isLocalUri() }?.let { "images/wallpaper.webp" to it } else null,
        )

    private fun buildLiveWallpaperEntries(preset: Preset): List<Pair<String, String>> =
        listOfNotNull(
            if (preset.isLiveWallpaper) {
                val lwUri = preset.liveWallpaperUri
                if (lwUri?.isLocalUri() == true) {
                    "images/wallpaper.${lwUri.substringAfterLast('.', "mp4")}" to lwUri
                } else null
            } else null,
            if (preset.isLiveWallpaperLandscape) {
                val lwUri = preset.liveWallpaperLandscapeUri
                if (lwUri?.isLocalUri() == true) {
                    "images/wallpaper_landscape.${lwUri.substringAfterLast('.', "mp4")}" to lwUri
                } else null
            } else null,
        )

    private fun readRawFile(uri: String): ByteArray =
        try { File(uri).readBytes() } catch (_: Exception) { ByteArray(0) }

    private fun compressUri(context: Context, uri: String): ByteArray =
        if (uri.startsWith("/")) {
            ImageCompressor.compressToWebP(File(uri))
        } else {
            ImageCompressor.compressToWebP(context, uri.toUri())
        }

    /** null 또는 http(s):// 이면 로컬 아님 */
    private fun String?.isLocalUri(): Boolean = this != null && !this.startsWith("http")
}
