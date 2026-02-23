package org.comon.streamlauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.graphics.Color
import org.comon.streamlauncher.apps_drawer.ui.AppDrawerScreen
import org.comon.streamlauncher.domain.model.ColorPresets
import org.comon.streamlauncher.launcher.FeedSideEffect
import org.comon.streamlauncher.launcher.FeedViewModel
import org.comon.streamlauncher.launcher.HomeIntent
import org.comon.streamlauncher.launcher.HomeSideEffect
import org.comon.streamlauncher.launcher.HomeViewModel
import org.comon.streamlauncher.launcher.ui.FeedScreen
import org.comon.streamlauncher.launcher.ui.HomeScreen
import org.comon.streamlauncher.launcher.ui.SettingsScreen
import org.comon.streamlauncher.navigation.CrossPagerNavigation
import org.comon.streamlauncher.ui.dragdrop.DragDropState
import org.comon.streamlauncher.ui.dragdrop.LocalDragDropState
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import org.comon.streamlauncher.widget.WidgetViewModel
import org.comon.streamlauncher.widget.ui.WidgetScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private val widgetViewModel: WidgetViewModel by viewModels()
    private val feedViewModel: FeedViewModel by viewModels()
    private var resetTrigger by mutableIntStateOf(0)

    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager

    // 위젯 선택 진행 중 임시 보관
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingSlot: Int = -1

    companion object {
        private const val HOST_ID = 1
    }

    // 위젯 구성 액티비티 결과 처리
    private val configureWidgetLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                widgetViewModel.setWidgetAtSlot(pendingSlot, pendingWidgetId)
            } else {
                if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    appWidgetHost.deleteAppWidgetId(pendingWidgetId)
                }
            }
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            pendingSlot = -1
        }

    // 위젯 선택 다이얼로그 결과 처리
    private val pickWidgetLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
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
                        Log.w("MainActivity", "위젯 구성 액티비티 없음, 바로 저장", e)
                        widgetViewModel.setWidgetAtSlot(pendingSlot, widgetId)
                        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                        pendingSlot = -1
                    }
                } else {
                    widgetViewModel.setWidgetAtSlot(pendingSlot, widgetId)
                    pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                    pendingSlot = -1
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetHost = AppWidgetHost(this, HOST_ID)
        appWidgetManager = AppWidgetManager.getInstance(this)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()
            val preset = ColorPresets.getByIndex(uiState.colorPresetIndex)
            val dragDropState = remember { DragDropState() }
            StreamLauncherTheme(
                accentPrimaryOverride = Color(preset.accentPrimaryArgb),
                accentSecondaryOverride = Color(preset.accentSecondaryArgb),
            ) {
            CompositionLocalProvider(LocalDragDropState provides dragDropState) {
                val widgetSlots by widgetViewModel.widgetSlots.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is HomeSideEffect.NavigateToApp -> {
                                val launchIntent = packageManager.getLaunchIntentForPackage(effect.packageName)
                                if (launchIntent != null) {
                                    try {
                                        startActivity(launchIntent)
                                    } catch (e: ActivityNotFoundException) {
                                        Log.w("MainActivity", "앱 실행 실패: ${effect.packageName}", e)
                                    }
                                } else {
                                    Log.w("MainActivity", "앱 실행 실패: ${effect.packageName}")
                                }
                            }
                            is HomeSideEffect.ShowError ->
                                Log.e("MainActivity", "Error: ${effect.message}")
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    feedViewModel.effect.collect { effect ->
                        when (effect) {
                            is FeedSideEffect.OpenUrl -> {
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)))
                                } catch (e: ActivityNotFoundException) {
                                    Log.w("MainActivity", "URL 열기 실패: ${effect.url}", e)
                                }
                            }
                            is FeedSideEffect.ShowError ->
                                Log.e("MainActivity", "Feed Error: ${effect.message}")
                        }
                    }
                }

                CrossPagerNavigation(
                    resetTrigger = resetTrigger,
                    wallpaperImage = uiState.wallpaperImage,
                    feedContent = {
                        FeedScreen(
                            state = feedState,
                            onIntent = feedViewModel::handleIntent,
                        )
                    },
                    settingsContent = {
                        SettingsScreen(
                            state = uiState,
                            onIntent = viewModel::handleIntent,
                        )
                    },
                    appDrawerContent = {
                        AppDrawerScreen(
                            searchQuery = uiState.searchQuery,
                            filteredApps = uiState.filteredApps,
                            onSearch = { viewModel.handleIntent(HomeIntent.Search(it)) },
                            onAppClick = { viewModel.handleIntent(HomeIntent.ClickApp(it)) },
                            onAppAssigned = { app, cell ->
                                viewModel.handleIntent(HomeIntent.AssignAppToCell(app, cell))
                            },
                        )
                    },
                    widgetContent = {
                        WidgetScreen(
                            widgetSlots = widgetSlots,
                            appWidgetHost = appWidgetHost,
                            onAddWidgetClick = ::launchWidgetPicker,
                            onDeleteWidgetClick = ::deleteWidget,
                        )
                    },
                ) {
                    HomeScreen(state = uiState, onIntent = viewModel::handleIntent)
                }
            } // CompositionLocalProvider
            } // StreamLauncherTheme
        } // setContent
    } // onCreate

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        ) {
            resetTrigger++
            viewModel.handleIntent(HomeIntent.ResetHome)
        }
    }

    private fun launchWidgetPicker(slotIndex: Int) {
        pendingSlot = slotIndex
        val newWidgetId = appWidgetHost.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newWidgetId)
        }
        pickWidgetLauncher.launch(pickIntent)
    }

    private fun deleteWidget(slotIndex: Int) {
        val widgetId = widgetViewModel.widgetSlots.value.getOrNull(slotIndex)
        if (widgetId != null && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(widgetId)
        }
        widgetViewModel.clearSlot(slotIndex)
    }
}
