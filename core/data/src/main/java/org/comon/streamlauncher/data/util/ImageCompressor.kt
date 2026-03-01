package org.comon.streamlauncher.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.core.graphics.scale

object ImageCompressor {

    /** content:// URI → WebP 바이트 배열 */
    fun compressToWebP(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1080,
        quality: Int = 80,
    ): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return ByteArray(0)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return encodeToWebP(original, maxWidth, quality)
    }

    /** 절대 파일 경로 → WebP 바이트 배열 (ContentResolver 없이 직접 읽기) */
    fun compressToWebP(
        file: File,
        maxWidth: Int = 1080,
        quality: Int = 80,
    ): ByteArray {
        val original = BitmapFactory.decodeFile(file.absolutePath)
            ?: return ByteArray(0)
        return encodeToWebP(original, maxWidth, quality)
    }

    private fun encodeToWebP(original: Bitmap, maxWidth: Int, quality: Int): ByteArray {
        val scaled = scaleBitmap(original, maxWidth)
        val output = ByteArrayOutputStream()
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        scaled.compress(format, quality, output)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        return output.toByteArray()
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt()
        return bitmap.scale(maxWidth, newHeight)
    }
}
