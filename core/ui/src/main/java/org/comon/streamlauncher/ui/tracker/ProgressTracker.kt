package org.comon.streamlauncher.ui.tracker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

abstract class ProgressTracker<T> {
    protected val _progress = MutableStateFlow<T?>(null)
    val progress: StateFlow<T?> = _progress.asStateFlow()

    private val _isPaused = MutableStateFlow(false)

    fun update(progress: T) {
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

    open fun clear() {
        _progress.value = null
        _isPaused.value = false
    }
}
