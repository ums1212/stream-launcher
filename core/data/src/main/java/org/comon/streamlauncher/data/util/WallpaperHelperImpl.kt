package org.comon.streamlauncher.data.util

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.comon.streamlauncher.domain.util.WallpaperHelper
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class WallpaperHelperImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WallpaperHelper {
    private val presetDir = File(context.filesDir, "preset_wallpapers").apply {
        if (!exists()) mkdirs()
    }

    // 사용자가 갤러리에서 선택한 이미지 URI를 앱 내부 저장소로 복사 후 경로 반환
    override suspend fun copyWallpaperFromUri(sourceUri: String, presetId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(presetDir, "wallpaper_$presetId.webp")
            val uri = sourceUri.toUri()
            val bytes = ImageCompressor.compressToWebP(context, uri, maxWidth = 2160, quality = 85)
            if (bytes.isEmpty()) return@withContext null
            destFile.writeBytes(bytes)
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun setWallpaperFromPreset(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                val wallpaperManager = WallpaperManager.getInstance(context)
                file.inputStream().use { stream ->
                    wallpaperManager.setStream(stream)
                }
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    override suspend fun deletePresetWallpaper(filePath: String): Unit = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
