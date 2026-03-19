package org.comon.streamlauncher.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class AppWidgetHostManager(
    private val activity: ComponentActivity,
    private val widgetViewModel: WidgetViewModel,
) {
    val appWidgetHost: AppWidgetHost = AppWidgetHost(activity.applicationContext, HOST_ID)

    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(activity)
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    // registerForActivityResult는 onCreate 이전(또는 onCreate 내, onStart 이전)에 호출 필수
    private val configureWidgetLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK &&
                pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
            ) {
                addPendingWidget(pendingWidgetId)
            } else {
                if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    appWidgetHost.deleteAppWidgetId(pendingWidgetId)
                }
            }
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }

    private val pickWidgetLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val widgetId = data.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                )
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return@registerForActivityResult

                pendingWidgetId = widgetId
                val widgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)

                if (widgetInfo?.configure != null) {
                    val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = widgetInfo.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    try {
                        configureWidgetLauncher.launch(configIntent)
                    } catch (e: ActivityNotFoundException) {
                        Log.w("AppWidgetHostManager", "위젯 구성 액티비티 없음, 바로 저장", e)
                        addPendingWidget(widgetId)
                        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                    }
                } else {
                    addPendingWidget(widgetId)
                    pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                }
            }
        }

    fun onStart() {
        appWidgetHost.startListening()
    }

    fun onStop() {
        try {
            appWidgetHost.stopListening()
        } catch (e: Exception) {
            // Android 시스템 버그: AppWidgetServiceImpl.Provider.id가 null인 상태에서
            // stopListening() 호출 시 RemoteException 발생 가능. 무시해도 무방함.
        }
    }

    fun onDestroy() {
        try {
            appWidgetHost.stopListening()
        } catch (_: Exception) {
            // onStop에서 이미 정리되더라도 회전 종료 시 한 번 더 방어적으로 해제
        }
        if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(pendingWidgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    fun launchWidgetPicker() {
        val newWidgetId = appWidgetHost.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newWidgetId)
        }
        pickWidgetLauncher.launch(pickIntent)
    }

    fun deleteWidget(appWidgetId: Int) {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        }
        widgetViewModel.handleIntent(WidgetIntent.RemoveWidget(appWidgetId))
    }

    private fun addPendingWidget(widgetId: Int) {
        val info = appWidgetManager.getAppWidgetInfo(widgetId)
        val density = activity.resources.displayMetrics.density
        val cellDp = 70f
        val minCols = if (info != null) ((info.minWidth / density + cellDp - 1) / cellDp).toInt().coerceAtLeast(1) else 2
        val minRows = if (info != null) ((info.minHeight / density + cellDp - 1) / cellDp).toInt().coerceAtLeast(1) else 2
        widgetViewModel.handleIntent(WidgetIntent.AddWidget(widgetId, minCols, minRows))
    }

    companion object {
        private const val HOST_ID = 1
    }
}
