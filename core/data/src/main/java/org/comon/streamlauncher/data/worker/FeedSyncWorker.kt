package org.comon.streamlauncher.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import org.comon.streamlauncher.domain.repository.FeedRepository
import org.comon.streamlauncher.domain.repository.SettingsRepository

private const val TAG = "FeedSyncWorker"

@HiltWorker
class FeedSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val feedRepository: FeedRepository,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.getSettings().first()
        if (settings.rssUrl.isEmpty() && settings.youtubeChannelId.isEmpty()) {
            Log.d(TAG, "No feed sources configured, skipping sync")
            return Result.success()
        }
        return try {
            feedRepository.getIntegratedFeed(settings.rssUrl, settings.youtubeChannelId)
                .first()
                .fold(
                    onSuccess = {
                        Log.d(TAG, "Feed sync completed: ${it.size} items")
                        Result.success()
                    },
                    onFailure = {
                        Log.w(TAG, "Feed sync failed", it)
                        Result.retry()
                    },
                )
        } catch (e: Exception) {
            Log.e(TAG, "Feed sync error", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
