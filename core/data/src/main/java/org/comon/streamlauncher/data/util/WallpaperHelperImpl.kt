package org.comon.streamlauncher.data.util

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.comon.streamlauncher.domain.util.WallpaperHelper
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperHelperImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WallpaperHelper {
    private val presetDir = File(context.filesDir, "preset_wallpapers").apply {
        if (!exists()) mkdirs()
    }

    @SuppressLint("MissingPermission")
    override suspend fun saveCurrentWallpaperForPreset(presetId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val drawable = wallpaperManager.drawable
            if (drawable is BitmapDrawable) {
                val file = File(presetDir, "wallpaper_$presetId.png")
                FileOutputStream(file).use { out ->
                    drawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                return@withContext file.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    @SuppressLint("MissingPermission")
    override suspend fun setWallpaperFromPreset(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                val wallpaperManager = WallpaperManager.getInstance(context)
                // Use input stream directly
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
