package org.comon.streamlauncher.preset_market.download

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.comon.streamlauncher.domain.model.preset.DownloadProgress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadProgressTracker @Inject constructor() {
    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    fun update(progress: DownloadProgress) {
        _progress.value = progress
    }

    fun clear() {
        _progress.value = null
    }
}
