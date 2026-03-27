package org.comon.streamlauncher.data.util

import android.content.Context
import android.net.Uri

/**
 * 콘텐츠 URI가 애니메이션 이미지(GIF 또는 애니메이션 WebP)인지 감지한다.
 *
 * - GIF: MIME type이 `image/gif`이면 항상 애니메이션으로 간주.
 * - WebP: RIFF 컨테이너 내 `ANIM` 청크 존재 여부를 바이트 검색으로 판단.
 *   API 레벨과 무관하게 동작.
 */
internal object AnimationDetector {

    fun isAnimated(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri) ?: return false
        return when (mimeType) {
            "image/gif" -> true
            "image/webp" -> hasAnimChunk(context, uri)
            else -> false
        }
    }

    /**
     * WebP 파일에서 ANIM 청크 식별자(0x41 0x4E 0x49 0x4D)를 검색한다.
     * 애니메이션 WebP는 RIFF 헤더 뒤 WEBP 식별자와 함께 반드시 ANIM 청크를 포함한다.
     */
    private fun hasAnimChunk(context: Context, uri: Uri): Boolean {
        val animSignature = byteArrayOf(0x41, 0x4E, 0x49, 0x4D) // "ANIM"
        val bufferSize = 4096
        val buffer = ByteArray(bufferSize)
        var found = false

        context.contentResolver.openInputStream(uri)?.use { stream ->
            var matchCount = 0
            var bytesRead: Int
            outer@ while (stream.read(buffer).also { bytesRead = it } != -1) {
                for (i in 0 until bytesRead) {
                    if (buffer[i] == animSignature[matchCount]) {
                        matchCount++
                        if (matchCount == animSignature.size) {
                            found = true
                            break@outer
                        }
                    } else {
                        matchCount = if (buffer[i] == animSignature[0]) 1 else 0
                    }
                }
            }
        }
        return found
    }
}
