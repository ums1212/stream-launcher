package org.comon.streamlauncher.data.util

import java.text.SimpleDateFormat
import java.util.Locale

object DateParser {

    private val RFC_822_FORMATS = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "dd MMM yyyy HH:mm:ss Z",
        "EEE, d MMM yyyy HH:mm:ss Z",
    )

    private val ISO_8601_FORMATS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
    )

    fun parseRfc822(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        for (pattern in RFC_822_FORMATS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                return sdf.parse(dateStr)?.time ?: continue
            } catch (_: Exception) {
                continue
            }
        }
        return 0L
    }

    fun parseIso8601(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        for (pattern in ISO_8601_FORMATS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                return sdf.parse(dateStr)?.time ?: continue
            } catch (_: Exception) {
                continue
            }
        }
        return 0L
    }
}
