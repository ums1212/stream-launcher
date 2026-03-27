package org.comon.streamlauncher.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.comon.streamlauncher.MainActivity
import org.comon.streamlauncher.R
import org.comon.streamlauncher.settings.upload.UploadDataHolder
import org.comon.streamlauncher.settings.upload.UploadProgressTracker
import org.comon.streamlauncher.domain.usecase.UploadPresetToMarketUseCase
import org.comon.streamlauncher.network.error.getErrorMessage
import javax.inject.Inject

@AndroidEntryPoint
class PresetUploadService : Service() {

    @Inject lateinit var uploadPresetToMarketUseCase: UploadPresetToMarketUseCase
    @Inject lateinit var progressTracker: UploadProgressTracker
    @Inject lateinit var uploadDataHolder: UploadDataHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val preset = uploadDataHolder.pendingPreset
        val previewUris = uploadDataHolder.pendingPreviewUris
        val description = uploadDataHolder.pendingDescription
        val tags = uploadDataHolder.pendingTags

        if (preset == null) {
            val appCtx = applicationContext
            val mgr = appCtx.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            showErrorNotification(appCtx, mgr, "업로드 실패", "업로드 데이터를 찾을 수 없습니다")
            stopSelf()
            return START_NOT_STICKY
        }

        val appContext = applicationContext
        val presetNameStr = preset.name
        val manager = appContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val useCase = uploadPresetToMarketUseCase
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

        scope.launch {
            useCase.uploadWithProgress(preset, previewUris, description, tags)
                .collect { progress ->
                    tracker.awaitResume()
                    tracker.update(progress)
                    val errorMsg = progress.error
                    when {
                        progress.isCompleted -> {
                            showCompletedNotification(appContext, manager, presetNameStr)
                            delay(1000L)
                            tracker.clear()
                            appContext.stopService(Intent(appContext, PresetUploadService::class.java))
                        }
                        errorMsg != null -> {
                            showErrorNotification(appContext, manager, presetNameStr, errorMsg.getErrorMessage("업로드"))
                            delay(1000L)
                            tracker.clear()
                            appContext.stopService(Intent(appContext, PresetUploadService::class.java))
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
        scope.cancel()
        progressTracker.clear()
        uploadDataHolder.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "preset_upload"
        const val NOTIFICATION_ID = 9001
        const val NOTIFICATION_RESULT_ID = 9002
        const val EXTRA_PRESET_NAME = "extra_preset_name"

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
                .setContentTitle("'$presetName' 업로드 중")
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
                .setContentTitle("업로드 완료")
                .setContentText("'$presetName'이 마켓에 업로드되었습니다")
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
                .setContentTitle("업로드 실패")
                .setContentText("'$presetName': $message")
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .build()
            manager.cancel(NOTIFICATION_ID)
            manager.notify(NOTIFICATION_RESULT_ID, notification)
        }
    }
}


