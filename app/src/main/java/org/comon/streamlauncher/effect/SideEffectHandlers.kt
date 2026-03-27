package org.comon.streamlauncher.effect

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.launcher.FeedSideEffect
import org.comon.streamlauncher.launcher.HomeSideEffect
import org.comon.streamlauncher.service.PresetUploadService
import org.comon.streamlauncher.service.VideoLiveWallpaperService
import org.comon.streamlauncher.settings.SettingsSideEffect
import org.comon.streamlauncher.settings.navigation.Launcher

@Composable
fun HomeSideEffectHandler(
    effectFlow: Flow<HomeSideEffect>,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        effectFlow.collect { effect ->
            when (effect) {
                is HomeSideEffect.NavigateToApp -> {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(effect.packageName)
                    if (launchIntent != null) {
                        try {
                            context.startActivity(launchIntent)
                        } catch (e: ActivityNotFoundException) {
                            Log.w("HomeSideEffectHandler", "앱 실행 실패: ${effect.packageName}", e)
                        }
                    } else {
                        Log.w("HomeSideEffectHandler", "앱 실행 실패: ${effect.packageName}")
                    }
                }
                is HomeSideEffect.ShowError -> {
                    Log.e("HomeSideEffectHandler", "Error: ${effect.message}")
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(effect.message)
                }
                is HomeSideEffect.SetDefaultHomeApp -> {
                    try {
                        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
                is HomeSideEffect.ShowNetworkError -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "네트워크 연결을 확인해주세요",
                        actionLabel = "설정",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}

@Composable
fun FeedSideEffectHandler(
    effectFlow: Flow<FeedSideEffect>,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        effectFlow.collect { effect ->
            when (effect) {
                is FeedSideEffect.OpenUrl -> {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                    } catch (e: ActivityNotFoundException) {
                        Log.w("FeedSideEffectHandler", "URL 열기 실패: ${effect.url}", e)
                    }
                }
                is FeedSideEffect.ShowError -> {
                    Log.e("FeedSideEffectHandler", "Feed Error: ${effect.message}")
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(effect.message)
                }
                is FeedSideEffect.ShowNetworkError -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "네트워크 연결을 확인해주세요",
                        actionLabel = "설정",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSideEffectHandler(
    effectFlow: Flow<SettingsSideEffect>,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onRequireSignIn: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        effectFlow.collect { effect ->
            when (effect) {
                is SettingsSideEffect.NavigateToMain ->
                    navController.popBackStack(Launcher, inclusive = false)
                is SettingsSideEffect.StartUploadService -> {
                    val serviceIntent = Intent(context, PresetUploadService::class.java)
                        .putExtra(PresetUploadService.EXTRA_PRESET_NAME, effect.presetName)
                    context.startForegroundService(serviceIntent)
                }
                is SettingsSideEffect.UploadStarted ->
                    snackbarHostState.showSnackbar("${effect.presetName}을 마켓에 업로드합니다")
                is SettingsSideEffect.UploadSuccess ->
                    snackbarHostState.showSnackbar("프리셋이 마켓에 업로드되었습니다!")
                is SettingsSideEffect.UploadError ->
                    snackbarHostState.showSnackbar("업로드 실패: ${effect.message}")
                is SettingsSideEffect.RequireSignIn ->
                    onRequireSignIn()
                is SettingsSideEffect.StopUploadService ->
                    context.stopService(Intent(context, PresetUploadService::class.java))
                is SettingsSideEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
                is SettingsSideEffect.ShowNetworkError -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "네트워크 연결을 확인해주세요",
                        actionLabel = "설정",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) {}
                    }
                }
                is SettingsSideEffect.LaunchLiveWallpaperPicker -> {
                    val component = ComponentName(context, VideoLiveWallpaperService::class.java)
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    val currentInfo = wallpaperManager.wallpaperInfo
                    if (currentInfo?.serviceName == VideoLiveWallpaperService::class.java.name) {
                        // 이미 활성화된 경우: URI가 DataStore에 저장됐으므로 서비스가 자동으로 갱신
                        snackbarHostState.showSnackbar("라이브 배경화면이 업데이트됩니다")
                    } else {
                        try {
                            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            try {
                                context.startActivity(
                                    Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {
                                snackbarHostState.showSnackbar("라이브 배경화면 설정 화면을 열 수 없습니다")
                            }
                        }
                    }
                }
            }
        }
    }
}
