package org.comon.streamlauncher.effect

import android.content.ActivityNotFoundException
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
) {
    LaunchedEffect(Unit) {
        effectFlow.collect { effect ->
            when (effect) {
                is SettingsSideEffect.NavigateToMain ->
                    navController.popBackStack(Launcher, inclusive = false)
                is SettingsSideEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
}
