package org.comon.streamlauncher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import org.comon.streamlauncher.data.worker.FeedSyncWorker
import org.comon.streamlauncher.ui.component.AppIconFetcher
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class StreamLauncherApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        createNotificationChannels()
        scheduleFeedSync()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .components {
                add(AppIconFetcher.Factory())
            }
            .crossfade(true)
            .build()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "preset_upload",
            "프리셋 업로드",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "프리셋 마켓 업로드 진행 상황"
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun scheduleFeedSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<FeedSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "feed_sync_initial",
            ExistingWorkPolicy.KEEP,
            oneTimeRequest,
        )

        val periodicRequest = PeriodicWorkRequestBuilder<FeedSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "feed_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
    }
}
