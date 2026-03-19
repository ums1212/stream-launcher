package org.comon.streamlauncher.settings.upload

import org.comon.streamlauncher.domain.model.preset.UploadProgress
import org.comon.streamlauncher.ui.tracker.ProgressTracker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadProgressTracker @Inject constructor() : ProgressTracker<UploadProgress>()
