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

    /**
     * content:// URI → WebP 바이트 배열 (최대 크기 보장)
     *
     * 초기 압축 후 [maxSizeBytes]를 초과하면 quality를 단계적으로 낮춰 재압축한다.
     * quality 최소값([minQuality])까지 낮춰도 초과하면 maxWidth를 절반으로 줄여 한 번 더 시도한다.
     */
    fun compressToWebPWithMaxSize(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1080,
        quality: Int = 80,
        maxSizeBytes: Long = 10L * 1024 * 1024,
        minQuality: Int = 30,
    ): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return ByteArray(0)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        // quality를 10씩 낮추며 시도
        val qualities = (quality downTo minQuality step 10).toList()
            .let { if (it.last() > minQuality) it + minQuality else it }

        for (q in qualities) {
            val scaled = scaleBitmap(original, maxWidth)
            val output = ByteArrayOutputStream()
            scaled.compress(format, q, output)
            if (scaled !== original) scaled.recycle()
            val bytes = output.toByteArray()
            if (bytes.size <= maxSizeBytes) {
                original.recycle()
                return bytes
            }
        }

        // quality 최소로도 초과 → maxWidth 절반으로 한 번 더 시도
        val halfWidth = maxWidth / 2
        val scaled = scaleBitmap(original, halfWidth)
        val output = ByteArrayOutputStream()
        scaled.compress(format, minQuality, output)
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
