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
            showErrorNotification("다운로드 실패", "다운로드 데이터를 찾을 수 없습니다")
            stopSelf()
            return START_NOT_STICKY
        }

        val presetName = preset.name
        val progressNotification = buildProgressNotification(presetName, 0f)

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
            downloadMarketPresetUseCase.downloadWithProgress(preset)
                .collect { progress ->
                    progressTracker.awaitResume()
                    progressTracker.update(progress)
                    val errorMsg = progress.error
                    when {
                        progress.isCompleted -> {
                            maybeActivateLiveWallpaper(progress.liveWallpaperUri)
                            showCompletedNotification(presetName)
                            delay(1000L)
                            progressTracker.clear()
                            stopSelf()
                        }
                        errorMsg != null -> {
                            showErrorNotification(presetName, errorMsg)
                            delay(1000L)
                            progressTracker.clear()
                            stopSelf()
                        }
                        else -> {
                            updateProgressNotification(presetName, progress.percentage)
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

    private fun maybeActivateLiveWallpaper(liveWallpaperUri: String?) {
        if (liveWallpaperUri == null) return
        val wallpaperManager = getSystemService(WallpaperManager::class.java)
        val isAlreadyActive = wallpaperManager.wallpaperInfo?.serviceName == VideoLiveWallpaperService::class.java.name
        if (isAlreadyActive) return  // DataStore 갱신만으로 서비스가 자동 업데이트됨
        try {
            val component = ComponentName(this, VideoLiveWallpaperService::class.java)
            startActivity(
                Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(
                    Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: Exception) { /* 무시 */ }
        }
    }

    private fun buildProgressNotification(presetName: String, progress: Float): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val progressInt = (progress * 100).toInt()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("'$presetName' 다운로드 중")
            .setContentText("$progressInt% 완료")
            .setProgress(100, progressInt, progressInt == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun updateProgressNotification(presetName: String, progress: Float) {
        val notification = buildProgressNotification(presetName, progress)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletedNotification(presetName: String) {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("다운로드 완료")
            .setContentText("'$presetName'이 다운로드되었습니다")
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
        manager.notify(NOTIFICATION_RESULT_ID, notification)
    }

    private fun showErrorNotification(presetName: String, message: String) {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("다운로드 실패")
            .setContentText("'$presetName': $message")
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
        manager.notify(NOTIFICATION_RESULT_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "preset_download"
        const val NOTIFICATION_ID = 9003
        const val NOTIFICATION_RESULT_ID = 9004
    }
}
