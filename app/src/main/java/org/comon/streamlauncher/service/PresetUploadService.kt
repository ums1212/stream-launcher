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

        if (preset == null) {
            showErrorNotification("업로드 실패", "업로드 데이터를 찾을 수 없습니다")
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

        scope.launch {
            uploadPresetToMarketUseCase.uploadWithProgress(preset, previewUris)
                .collect { progress ->
                    progressTracker.awaitResume()
                    progressTracker.update(progress)
                    val errorMsg = progress.error
                    when {
                        progress.isCompleted -> {
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
        scope.cancel()
        progressTracker.clear()
        uploadDataHolder.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
            .setContentTitle("'$presetName' 업로드 중")
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
            .setContentTitle("업로드 완료")
            .setContentText("'$presetName'이 마켓에 업로드되었습니다")
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
            .setContentTitle("업로드 실패")
            .setContentText("'$presetName': $message")
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
        manager.notify(NOTIFICATION_RESULT_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "preset_upload"
        const val NOTIFICATION_ID = 9001
        const val NOTIFICATION_RESULT_ID = 9002
        const val EXTRA_PRESET_NAME = "extra_preset_name"
    }
}
