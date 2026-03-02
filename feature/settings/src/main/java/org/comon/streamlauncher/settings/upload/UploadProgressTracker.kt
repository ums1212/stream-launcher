package org.comon.streamlauncher.settings.upload

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.comon.streamlauncher.domain.model.preset.UploadProgress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadProgressTracker @Inject constructor() {
    private val _progress = MutableStateFlow<UploadProgress?>(null)
    val progress: StateFlow<UploadProgress?> = _progress.asStateFlow()

    fun update(progress: UploadProgress) {
        _progress.value = progress
    }

    fun clear() {
        _progress.value = null
    }
}
