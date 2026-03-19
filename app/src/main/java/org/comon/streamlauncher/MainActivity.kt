package org.comon.streamlauncher

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

    private var resetTrigger by mutableIntStateOf(0)

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
            val settingsSnackbarHostState = remember { SnackbarHostState() }
            val settingsScope = rememberCoroutineScope()

            wallpaperManager.ObserveDestinationChanges(navController)

            StreamLauncherTheme(
                accentPrimaryOverride = Color(preset.accentPrimaryArgb),
                accentSecondaryOverride = Color(preset.accentSecondaryArgb),
            ) {
                CompositionLocalProvider(LocalDragDropState provides dragDropState) {
                    HomeSideEffectHandler(effectFlow = viewModel.effect)
                    FeedSideEffectHandler(effectFlow = feedViewModel.effect)

                    val onRequireSignIn = GoogleSignInFlow(
                        onSignInSuccess = { idToken ->
                            settingsViewModel.handleIntent(SettingsIntent.SignInWithGoogle(idToken))
                        },
                        onSignInFailure = { message ->
                            settingsScope.launch { settingsSnackbarHostState.showSnackbar(message) }
                        },
                    )

                    SettingsSideEffectHandler(
                        effectFlow = settingsViewModel.effect,
                        navController = navController,
                        snackbarHostState = settingsSnackbarHostState,
                        onRequireSignIn = onRequireSignIn,
                    )

                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val appDrawerColumns = if (isLandscape) uiState.appDrawerGridRows else uiState.appDrawerGridColumns
                    val appDrawerRows = if (isLandscape) uiState.appDrawerGridColumns else uiState.appDrawerGridRows

                    MainNavHost(
                        navController = navController,
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
                        settingsState = settingsState,
                        onSettingsIntent = settingsViewModel::handleIntent,
                        onShowSnackbar = { message ->
                            settingsScope.launch { settingsSnackbarHostState.showSnackbar(message) }
                        },
                        onRequireSignIn = onRequireSignIn,
                        onStartDownloadService = {
                            startForegroundService(Intent(this@MainActivity, PresetDownloadService::class.java))
                        },
                        onStopDownloadService = {
                            stopService(Intent(this@MainActivity, PresetDownloadService::class.java))
                        },
                    )

                    if (settingsState.showNoticeDialog) {
                        NoticeDialog(
                            noticeText = androidx.compose.ui.res.stringResource(org.comon.streamlauncher.settings.R.string.notice_body),
                            version = BuildConfig.VERSION_NAME,
                            onDismiss = { settingsViewModel.handleIntent(SettingsIntent.DismissNotice) },
                        )
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
