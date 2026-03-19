package org.comon.streamlauncher.preset_market.download

import org.comon.streamlauncher.domain.model.preset.PresetOperationProgress
import org.comon.streamlauncher.ui.tracker.ProgressTracker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadProgressTracker @Inject constructor() : ProgressTracker<PresetOperationProgress>() {
    private var _cancellationRequested = false
    val cancellationRequested: Boolean get() = _cancellationRequested

    fun requestCancellation() {
        _cancellationRequested = true
        resume()  // 취소 시 pause 해제 → coroutine이 cancellation을 받을 수 있도록
    }

    override fun clear() {
        super.clear()
        _cancellationRequested = false
    }
}
