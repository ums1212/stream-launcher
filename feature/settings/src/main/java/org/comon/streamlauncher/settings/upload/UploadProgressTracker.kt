package org.comon.streamlauncher.settings.upload

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.comon.streamlauncher.domain.model.preset.UploadProgress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadProgressTracker @Inject constructor() {
    private val _progress = MutableStateFlow<UploadProgress?>(null)
    val progress: StateFlow<UploadProgress?> = _progress.asStateFlow()

    private val _isPaused = MutableStateFlow(false)

    fun update(progress: UploadProgress) {
        _progress.value = progress
    }

    fun pause() {
        _isPaused.value = true
    }

    fun resume() {
        _isPaused.value = false
    }

    /** 일시정지 상태일 경우 resume될 때까지 suspend */
    suspend fun awaitResume() {
        _isPaused.first { !it }
    }

    fun clear() {
        _progress.value = null
        _isPaused.value = false
    }
}
