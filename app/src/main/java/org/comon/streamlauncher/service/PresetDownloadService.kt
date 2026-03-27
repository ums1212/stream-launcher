package org.comon.streamlauncher.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.comon.streamlauncher.MainActivity
import org.comon.streamlauncher.R
import org.comon.streamlauncher.domain.usecase.DownloadMarketPresetUseCase
import org.comon.streamlauncher.preset_market.download.DownloadDataHolder
import org.comon.streamlauncher.preset_market.download.DownloadProgressTracker
import org.comon.streamlauncher.network.error.getErrorMessage
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PresetDownloadService : Service() {

    @Inject lateinit var downloadMarketPresetUseCase: DownloadMarketPresetUseCase
    @Inject lateinit var progressTracker: DownloadProgressTracker
    @Inject lateinit var downloadDataHolder: DownloadDataHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 이미 다운로드 진행 중이면 무시
        if (downloadJob?.isActive == true) return START_NOT_STICKY

        val preset = downloadDataHolder.pendingPreset
        if (preset == null) {
            val appCtx = applicationContext
            val mgr = appCtx.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            showErrorNotification(appCtx, mgr, "다운로드 실패", "다운로드 데이터를 찾을 수 없습니다")
            stopSelf()
            return START_NOT_STICKY
        }

        val appContext = applicationContext
        val presetNameStr = preset.name
        val manager = appContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val useCase = downloadMarketPresetUseCase
        val tracker = progressTracker
        val progressNotification = buildProgressNotification(appContext, presetNameStr, 0f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                progressNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, progressNotification)
        }

        downloadJob = scope.launch {
            useCase.downloadWithProgress(preset)
                .collect { progress ->
                    tracker.awaitResume()
                    tracker.update(progress)
                    val errorMsg = progress.error
                    when {
                        progress.isCompleted -> {
                            maybeActivateLiveWallpaper(appContext, progress.liveWallpaperUri)
                            showCompletedNotification(appContext, manager, presetNameStr)
                            delay(1000L)
                            tracker.clear()
                            appContext.stopService(Intent(appContext, PresetDownloadService::class.java))
                        }
                        errorMsg != null -> {
                            showErrorNotification(appContext, manager, presetNameStr, errorMsg.getErrorMessage("다운로드"))
                            delay(1000L)
                            tracker.clear()
                            appContext.stopService(Intent(appContext, PresetDownloadService::class.java))
                        }
                        else -> {
                            updateProgressNotification(appContext, manager, presetNameStr, progress.percentage)
                        }
                    }
                }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // 사용자가 취소한 경우 → 알림 제거 + 로컬 고아 파일 삭제
        if (progressTracker.cancellationRequested) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_ID)
            val preset = downloadDataHolder.pendingPreset
            if (preset != null) {
                File(filesDir, "market_presets/${preset.id}").deleteRecursively()
            }
            progressTracker.clear()
        }
        scope.cancel()
        downloadDataHolder.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "preset_download"
        const val NOTIFICATION_ID = 9003
        const val NOTIFICATION_RESULT_ID = 9004

        private fun maybeActivateLiveWallpaper(context: android.content.Context, liveWallpaperUri: String?) {
            if (liveWallpaperUri == null) return
            val wallpaperManager = context.getSystemService(WallpaperManager::class.java)
            val isAlreadyActive = wallpaperManager.wallpaperInfo?.serviceName == VideoLiveWallpaperService::class.java.name
            if (isAlreadyActive) return  // DataStore 갱신만으로 서비스가 자동 업데이트됨
            try {
                val component = ComponentName(context, VideoLiveWallpaperService::class.java)
                context.startActivity(
                    Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                        .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: ActivityNotFoundException) {
                try {
                    context.startActivity(
                        Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } catch (_: Exception) { /* 무시 */ }
            }
        }

        private fun buildProgressNotification(context: android.content.Context, presetName: String, progress: Float): Notification {
            val openIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val progressInt = (progress * 100).toInt()
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("'$presetName' 다운로드 중")
                .setContentText("$progressInt% 완료")
                .setProgress(100, progressInt, progressInt == 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openIntent)
                .build()
        }

        private fun updateProgressNotification(context: android.content.Context, manager: NotificationManager, presetName: String, progress: Float) {
            val notification = buildProgressNotification(context, presetName, progress)
            manager.notify(NOTIFICATION_ID, notification)
        }

        private fun showCompletedNotification(context: android.content.Context, manager: NotificationManager, presetName: String) {
            val openIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("다운로드 완료")
                .setContentText("'$presetName'이 다운로드되었습니다")
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .build()
            manager.cancel(NOTIFICATION_ID)
            manager.notify(NOTIFICATION_RESULT_ID, notification)
        }

        private fun showErrorNotification(context: android.content.Context, manager: NotificationManager, presetName: String, message: String) {
            val openIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("다운로드 실패")
                .setContentText("'$presetName': $message")
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .build()
            manager.cancel(NOTIFICATION_ID)
            manager.notify(NOTIFICATION_RESULT_ID, notification)
        }
    }
}
