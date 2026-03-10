package org.comon.streamlauncher.preset_market.download

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.comon.streamlauncher.domain.model.preset.DownloadProgress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadProgressTracker @Inject constructor() {
    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    private val _isPaused = MutableStateFlow(false)

    private var _cancellationRequested = false
    val cancellationRequested: Boolean get() = _cancellationRequested

    fun update(progress: DownloadProgress) {
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

    fun requestCancellation() {
        _cancellationRequested = true
        _isPaused.value = false  // 취소 시 pause 해제 → coroutine이 cancellation을 받을 수 있도록
    }

    fun clear() {
        _progress.value = null
        _isPaused.value = false
        _cancellationRequested = false
    }
}
