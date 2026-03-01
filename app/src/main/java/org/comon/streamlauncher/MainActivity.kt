package org.comon.streamlauncher

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.comon.streamlauncher.apps_drawer.ui.AppDrawerScreen
import org.comon.streamlauncher.domain.model.ColorPresets
import org.comon.streamlauncher.launcher.FeedSideEffect
import org.comon.streamlauncher.launcher.FeedViewModel
import org.comon.streamlauncher.launcher.HomeIntent
import org.comon.streamlauncher.launcher.HomeSideEffect
import org.comon.streamlauncher.launcher.HomeViewModel
import org.comon.streamlauncher.launcher.ui.FeedScreen
import org.comon.streamlauncher.launcher.ui.HomeScreen
import org.comon.streamlauncher.navigation.CrossPagerNavigation
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsSideEffect
import org.comon.streamlauncher.settings.SettingsViewModel
import org.comon.streamlauncher.settings.navigation.SettingsMenu
import org.comon.streamlauncher.settings.navigation.SettingsRoute
import org.comon.streamlauncher.settings.ui.NoticeDialog
import org.comon.streamlauncher.settings.ui.SettingsDetailScreen
import org.comon.streamlauncher.settings.ui.SettingsScreen
import org.comon.streamlauncher.ui.dragdrop.DragDropState
import org.comon.streamlauncher.ui.dragdrop.LocalDragDropState
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import org.comon.streamlauncher.widget.WidgetViewModel
import org.comon.streamlauncher.widget.ui.WidgetScreen
import androidx.core.net.toUri

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private val widgetViewModel: WidgetViewModel by viewModels()
    private val feedViewModel: FeedViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var resetTrigger by mutableIntStateOf(0)

    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager

    // 위젯 선택 진행 중 임시 보관
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingSlot: Int = -1

    // 월페이퍼 밝기 상태 (true = 어두운 월페이퍼 → 시스템바 아이콘 밝게)
    private var isWallpaperDark = mutableStateOf(false)

    private val wallpaperColorsChangedListener =
        WallpaperManager.OnColorsChangedListener { colors, _ ->
            val dark = isDarkFromColors(colors)
            isWallpaperDark.value = dark
            updateSystemBarStyle(dark)
        }

    companion object {
        private const val HOST_ID = 1
    }

    /**
     * WallpaperColors에서 배경이 "어두운지" 판별합니다.
     *
     * - API 31+: HINT_SUPPORTS_DARK_TEXT 비트 플래그 (공식 방법)
     * - API 28-30: primaryColor의 luminance로 직접 판별
     *   (HINT_SUPPORTS_DARK_TEXT 상수 자체가 API 31에서 추가되었으므로
     *    그 이전 버전에서 colorHints를 비교하면 항상 0이 반환됨)
     *
     * @return true = 어두운 배경 → 시스템바 아이콘 흰색
     *         false = 밝은 배경 → 시스템바 아이콘 검은색
     */
    private fun isDarkFromColors(colors: WallpaperColors?): Boolean {
        if (colors == null) return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: HINT_SUPPORTS_DARK_TEXT(=1) 플래그가 없으면 어두운 배경
            (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) == 0
        } else {
            // API 28-30: 주 색상의 상대적 휘도로 판별 (0.0=검정, 1.0=흰색)
            val luminance = ColorUtils.calculateLuminance(colors.primaryColor.toArgb())
            luminance < 0.5
        }
    }

    /**
     * 월페이퍼 밝기에 따라 시스템바 아이콘 색상을 동적으로 전환합니다.
     * - isDark=true  (어두운 월페이퍼) → SystemBarStyle.dark  → 아이콘 흰색
     * - isDark=false (밝은 월페이퍼)  → SystemBarStyle.light → 아이콘 검은색
     */
    private fun updateSystemBarStyle(isDark: Boolean) {
        val transparent = android.graphics.Color.TRANSPARENT
        if (isDark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(transparent),
                navigationBarStyle = SystemBarStyle.dark(transparent),
            )
        } else {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(transparent, transparent),
                navigationBarStyle = SystemBarStyle.light(transparent, transparent),
            )
        }
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

        // 초기 월페이퍼 색상 조회 → 시스템바 스타일 결정
        val wallpaperManager = WallpaperManager.getInstance(this)
        val initialColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        val initialDark = isDarkFromColors(initialColors)
        isWallpaperDark.value = initialDark
        updateSystemBarStyle(initialDark)

        // 월페이퍼 변경 리스너 등록
        wallpaperManager.addOnColorsChangedListener(
            wallpaperColorsChangedListener,
            Handler(Looper.getMainLooper()),
        )

        appWidgetHost = AppWidgetHost(this, HOST_ID)
        appWidgetManager = AppWidgetManager.getInstance(this)

        viewModel.handleIntent(HomeIntent.CheckFirstLaunch)
        settingsViewModel.checkNotice(BuildConfig.VERSION_NAME)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val preset = ColorPresets.getByIndex(settingsState.colorPresetIndex)
            val dragDropState = remember { DragDropState() }
            val navController = rememberNavController()

            StreamLauncherTheme(
                accentPrimaryOverride = Color(preset.accentPrimaryArgb),
                accentSecondaryOverride = Color(preset.accentSecondaryArgb),
            ) {
            CompositionLocalProvider(LocalDragDropState provides dragDropState) {
                val widgetSlots by widgetViewModel.widgetSlots.collectAsStateWithLifecycle()
                val isWidgetEditMode by widgetViewModel.isEditMode.collectAsStateWithLifecycle()

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
                            is HomeSideEffect.SetDefaultHomeApp -> {
                                try {
                                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                    if (intent.resolveActivity(packageManager) != null) {
                                        startActivity(intent)
                                    } else {
                                        startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                } catch (_: ActivityNotFoundException) {
                                    startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    feedViewModel.effect.collect { effect ->
                        when (effect) {
                            is FeedSideEffect.OpenUrl -> {
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                                } catch (e: ActivityNotFoundException) {
                                    Log.w("MainActivity", "URL 열기 실패: ${effect.url}", e)
                                }
                            }
                            is FeedSideEffect.ShowError ->
                                Log.e("MainActivity", "Feed Error: ${effect.message}")
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    settingsViewModel.effect.collect { effect ->
                        when (effect) {
                            is SettingsSideEffect.NavigateToMain ->
                                navController.popBackStack(SettingsRoute.LAUNCHER, false)
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = SettingsRoute.LAUNCHER,
                ) {
                    composable(route = SettingsRoute.LAUNCHER) {
                        CrossPagerNavigation(
                            resetTrigger = resetTrigger,
                            feedContent = { isVisible ->
                                FeedScreen(
                                    state = feedState,
                                    isVisible = isVisible,
                                    onIntent = feedViewModel::handleIntent,
                                )
                            },
                            settingsContent = {
                                SettingsScreen(
                                    onIntent = settingsViewModel::handleIntent,
                                    onNavigate = { navController.navigate(it) },
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
                                    columns = uiState.appDrawerGridColumns,
                                    rows = uiState.appDrawerGridRows,
                                    iconSizeRatio = uiState.appDrawerIconSizeRatio,
                                )
                            },
                            isWidgetEditMode = isWidgetEditMode,
                            widgetContent = {
                                WidgetScreen(
                                    widgetSlots = widgetSlots,
                                    appWidgetHost = appWidgetHost,
                                    onAddWidgetClick = ::launchWidgetPicker,
                                    onDeleteWidgetClick = ::deleteWidget,
                                    isEditMode = isWidgetEditMode,
                                    onEditModeChange = { widgetViewModel.setEditMode(it) }
                                )
                            },
                            isHomeEditMode = uiState.editingCell != null,
                        ) {
                            HomeScreen(state = uiState, onIntent = viewModel::handleIntent)
                        }
                    }
                    composable(
                        route = SettingsRoute.DETAIL,
                        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                    ) { backStackEntry ->
                        val menuStr = backStackEntry.arguments?.getString("menu")
                            ?: return@composable
                        val menu = runCatching { SettingsMenu.valueOf(menuStr) }.getOrNull()
                            ?: return@composable
                        SettingsDetailScreen(
                            menu = menu,
                            state = settingsState,
                            onIntent = settingsViewModel::handleIntent,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }

                if (settingsState.showNoticeDialog) {
                    NoticeDialog(
                        noticeText = androidx.compose.ui.res.stringResource(org.comon.streamlauncher.settings.R.string.notice_body),
                        version = BuildConfig.VERSION_NAME,
                        onDismiss = { settingsViewModel.handleIntent(SettingsIntent.DismissNotice) },
                    )
                }
            } // CompositionLocalProvider
            } // StreamLauncherTheme
        } // setContent
    } // onCreate

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onResume() {
        super.onResume()
        // 백그라운드에서 월페이퍼가 변경된 경우를 대비해 포그라운드 복귀 시 재조회
        val colors = WallpaperManager.getInstance(this)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        val isDark = isDarkFromColors(colors)
        if (isDark != isWallpaperDark.value) {
            isWallpaperDark.value = isDark
            updateSystemBarStyle(isDark)
        }
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        WallpaperManager.getInstance(this)
            .removeOnColorsChangedListener(wallpaperColorsChangedListener)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        ) {
            resetTrigger++
            viewModel.handleIntent(HomeIntent.ResetHome)
            settingsViewModel.handleIntent(SettingsIntent.ResetTab)
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
