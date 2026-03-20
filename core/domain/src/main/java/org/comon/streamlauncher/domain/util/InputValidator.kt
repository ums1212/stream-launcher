package org.comon.streamlauncher.domain.util

object InputValidator {
    private val CHZZK_CHANNEL_REGEX = Regex("^[a-f0-9]{32}$")
    private val YT_CHANNEL_REGEX = Regex("^(UC[a-zA-Z0-9_-]{22}|@[a-zA-Z0-9._-]+)$")

    fun isValidChzzkChannelId(id: String): Boolean = CHZZK_CHANNEL_REGEX.matches(id)
    fun isValidYoutubeChannelId(id: String): Boolean = YT_CHANNEL_REGEX.matches(id)
}
