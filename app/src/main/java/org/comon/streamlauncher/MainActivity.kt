package org.comon.streamlauncher

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.ColorPresets
import org.comon.streamlauncher.effect.FeedSideEffectHandler
import org.comon.streamlauncher.effect.HomeSideEffectHandler
import org.comon.streamlauncher.effect.SettingsSideEffectHandler
import org.comon.streamlauncher.launcher.FeedViewModel
import org.comon.streamlauncher.launcher.HomeIntent
import org.comon.streamlauncher.launcher.HomeViewModel
import org.comon.streamlauncher.launcher.ui.FeedScreen
import org.comon.streamlauncher.launcher.ui.HomeScreen
import org.comon.streamlauncher.navigation.CrossPagerNavigation
import org.comon.streamlauncher.navigation.MainNavHost
import org.comon.streamlauncher.apps_drawer.ui.AppDrawerScreen
import org.comon.streamlauncher.service.PresetDownloadService
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsViewModel
import org.comon.streamlauncher.settings.ui.NoticeDialog
import org.comon.streamlauncher.settings.ui.SettingsScreen
import org.comon.streamlauncher.ui.GoogleSignInFlow
import org.comon.streamlauncher.ui.dragdrop.DragDropState
import org.comon.streamlauncher.ui.dragdrop.LocalDragDropState
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import org.comon.streamlauncher.wallpaper.WallpaperSystemBarManager
import org.comon.streamlauncher.widget.AppWidgetHostManager
import org.comon.streamlauncher.widget.WidgetViewModel
import org.comon.streamlauncher.widget.ui.WidgetScreen
import androidx.compose.ui.graphics.Color

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private val widgetViewModel: WidgetViewModel by viewModels()
    private val feedViewModel: FeedViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val liveWallpaperSettingsViewModel: org.comon.streamlauncher.settings.livewallpaper.LiveWallpaperSettingsViewModel by viewModels()

    private var resetTrigger by mutableIntStateOf(0)

    // ── 가로 배경화면 피커 상태 ──────────────────────────────────────────────────
    /** 가로 배경화면 피커 플로우 진행 중 여부 */
    private var isInLandscapePickerFlow = false
    /** 피커에서 확정할 새 가로 배경화면 ID */
    private var pendingLandscapeId: Int? = null
    /** 피커에서 확정할 새 가로 배경화면 URI */
    private var pendingLandscapeUri: String? = null
    /** 세로 URI 파일을 미리보기용으로 임시 교체한 경우 true */
    private var needsPortraitUriRestore = false
    /** 임시 교체 전 원본 세로 URI (null 이면 파일이 없었음) */
    private var originalPortraitUri: String? = null

    private val wallpaperManager = WallpaperSystemBarManager(this)
    private lateinit var widgetHostManager: AppWidgetHostManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // registerForActivityResult 타이밍 요건: onCreate 내, onStart 이전
        widgetHostManager = AppWidgetHostManager(this, widgetViewModel)

        wallpaperManager.initialize()
        viewModel.handleIntent(HomeIntent.CheckFirstLaunch)
        settingsViewModel.checkNotice(BuildConfig.VERSION_NAME)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val feedState by feedViewModel.uiState.collectAsStateWithLifecycle()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val widgetState by widgetViewModel.uiState.collectAsStateWithLifecycle()
            val preset = ColorPresets.getByIndex(settingsState.colorPresetIndex)
            val dragDropState = remember { DragDropState() }
            val navController = rememberNavController()
            val snackbarHostState = remember { SnackbarHostState() }
            val settingsScope = rememberCoroutineScope()

            wallpaperManager.ObserveDestinationChanges(navController)

            StreamLauncherTheme(
                accentPrimaryOverride = Color(preset.accentPrimaryArgb),
                accentSecondaryOverride = Color(preset.accentSecondaryArgb),
            ) {
                CompositionLocalProvider(LocalDragDropState provides dragDropState) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HomeSideEffectHandler(
                            effectFlow = viewModel.effect,
                            snackbarHostState = snackbarHostState,
                        )
                        FeedSideEffectHandler(
                            effectFlow = feedViewModel.effect,
                            snackbarHostState = snackbarHostState,
                        )

                        val onRequireSignIn = GoogleSignInFlow(
                            onSignInSuccess = { idToken ->
                                settingsViewModel.handleIntent(SettingsIntent.SignInWithGoogle(idToken))
                            },
                            onSignInFailure = { message ->
                                settingsScope.launch { snackbarHostState.showSnackbar(message) }
                            },
                        )

                        SettingsSideEffectHandler(
                            effectFlow = settingsViewModel.effect,
                            navController = navController,
                            snackbarHostState = snackbarHostState,
                        )

                        val configuration = LocalConfiguration.current
                        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                        val appDrawerColumns = if (isLandscape) uiState.appDrawerGridRows else uiState.appDrawerGridColumns
                        val appDrawerRows = if (isLandscape) uiState.appDrawerGridColumns else uiState.appDrawerGridRows

                        MainNavHost(
                            navController = navController,
                            onLaunchLiveWallpaperPicker = { landscapeNewId, landscapeNewUri ->
                                if (landscapeNewUri != null) {
                                    // 가로 배경화면 플로우: DataStore 기록은 onResume에서 확정
                                    isInLandscapePickerFlow = true
                                    pendingLandscapeId = landscapeNewId
                                    pendingLandscapeUri = landscapeNewUri
                                    // 피커 세로 미리보기에 가로 콘텐츠가 보이도록 portrait 파일 임시 교체
                                    val portraitFile = java.io.File(filesDir, "live_wallpaper_uri.txt")
                                    originalPortraitUri = portraitFile.takeIf { it.exists() }
                                        ?.readText()?.trim()?.takeIf { it.isNotBlank() }
                                    portraitFile.writeText(landscapeNewUri)
                                    needsPortraitUriRestore = true
                                    sendBroadcast(
                                        Intent(org.comon.streamlauncher.service.VideoLiveWallpaperService.ACTION_RELOAD_URI)
                                            .apply { `package` = packageName }
                                    )
                                }
                                val component = android.content.ComponentName(this@MainActivity, org.comon.streamlauncher.service.VideoLiveWallpaperService::class.java)
                                try {
                                    startActivity(
                                        Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                                            .putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: android.content.ActivityNotFoundException) {
                                    try {
                                        startActivity(
                                            Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } catch (_: Exception) {}
                                }
                            },
                            onReloadWallpaper = {
                                runCatching { android.app.WallpaperManager.getInstance(this@MainActivity).clear() }
                            },
                            onRequireSignIn = onRequireSignIn,
                            launcherContent = {
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
                                            onShowAppInfo = { viewModel.handleIntent(HomeIntent.ShowAppInfo(it.packageName)) },
                                            onRequestUninstall = { viewModel.handleIntent(HomeIntent.RequestUninstall(it.packageName)) },
                                            columns = appDrawerColumns,
                                            rows = appDrawerRows,
                                            iconSizeRatio = uiState.appDrawerIconSizeRatio,
                                        )
                                    },
                                    isWidgetEditMode = widgetState.isEditMode,
                                    isWidgetDragging = widgetState.draggingWidgetId != null,
                                    widgetContent = {
                                        WidgetScreen(
                                            state = widgetState,
                                            appWidgetHost = widgetHostManager.appWidgetHost,
                                            onAddWidgetClick = widgetHostManager::launchWidgetPicker,
                                            onDeleteWidgetClick = widgetHostManager::deleteWidget,
                                            onIntent = widgetViewModel::handleIntent,
                                        )
                                    },
                                    isHomeEditMode = uiState.editingCell != null,
                                ) {
                                    HomeScreen(state = uiState, onIntent = viewModel::handleIntent)
                                }
                            },
                            onStartDownloadService = {
                                startForegroundService(Intent(this@MainActivity, PresetDownloadService::class.java))
                            },
                            onStopDownloadService = {
                                stopService(Intent(this@MainActivity, PresetDownloadService::class.java))
                            },
                            onStartUploadService = { presetName ->
                                val serviceIntent = Intent(this@MainActivity, org.comon.streamlauncher.service.PresetUploadService::class.java)
                                    .putExtra(org.comon.streamlauncher.service.PresetUploadService.EXTRA_PRESET_NAME, presetName)
                                startForegroundService(serviceIntent)
                            },
                            onStopUploadService = {
                                stopService(Intent(this@MainActivity, org.comon.streamlauncher.service.PresetUploadService::class.java))
                            },
                        )

                        if (settingsState.showNoticeDialog) {
                            NoticeDialog(
                                noticeText = androidx.compose.ui.res.stringResource(org.comon.streamlauncher.settings.R.string.notice_body),
                                version = BuildConfig.VERSION_NAME,
                                onDismiss = { settingsViewModel.handleIntent(SettingsIntent.DismissNotice) },
                            )
                        }

                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        ) { snackbarData ->
                            val dismissState = rememberSwipeToDismissBoxState()
                            LaunchedEffect(dismissState.currentValue) {
                                if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                    snackbarData.dismiss()
                                }
                            }
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {},
                            ) {
                                Snackbar(snackbarData)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHostManager.onStart()
    }

    override fun onResume() {
        super.onResume()
        wallpaperManager.onResume()

        if (isInLandscapePickerFlow) {
            isInLandscapePickerFlow = false
            val newId = pendingLandscapeId
            val newUri = pendingLandscapeUri
            pendingLandscapeId = null
            pendingLandscapeUri = null

            // 가로 배경화면을 DataStore에 확정 저장
            liveWallpaperSettingsViewModel.handleIntent(
                org.comon.streamlauncher.settings.livewallpaper.LiveWallpaperSettingsIntent.ConfirmLandscapeWallpaper(
                    id = newId,
                    uri = newUri,
                )
            )
            // 가로 URI 파일 직접 기록 (서비스는 파일을 읽으므로 DataStore 코루틴보다 먼저 반영)
            val landscapeFile = java.io.File(filesDir, "live_wallpaper_uri_landscape.txt")
            if (newUri != null) landscapeFile.writeText(newUri) else landscapeFile.delete()
        }

        // portrait 파일 임시 교체를 원상 복원하고 서비스에 reload 브로드캐스트 전송
        // (가로 플로우에서는 landscape 파일 기록 후 이 블록이 실행되므로 올바른 순서로 처리됨)
        if (needsPortraitUriRestore) {
            needsPortraitUriRestore = false
            val portraitFile = java.io.File(filesDir, "live_wallpaper_uri.txt")
            val orig = originalPortraitUri
            originalPortraitUri = null
            if (orig != null) portraitFile.writeText(orig) else portraitFile.delete()
            sendBroadcast(
                Intent(org.comon.streamlauncher.service.VideoLiveWallpaperService.ACTION_RELOAD_URI)
                    .apply { `package` = packageName }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        widgetHostManager.onStop()
    }

    override fun onDestroy() {
        widgetHostManager.onDestroy()
        super.onDestroy()
        wallpaperManager.destroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        wallpaperManager.onConfigurationChanged()
        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        settingsViewModel.handleIntent(SettingsIntent.ApplyStaticWallpaperForOrientation(isLandscape))
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
}
