package org.comon.streamlauncher.settings.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.model.SettingsActionType
import org.comon.streamlauncher.settings.model.settingMenuList
import org.comon.streamlauncher.settings.navigation.SettingsMenu
import org.comon.streamlauncher.settings.navigation.SettingsRoute
import org.comon.streamlauncher.ui.util.calculateIsCompactHeight

@Composable
fun SettingsScreen(
    onIntent: (SettingsIntent) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        MainSettingsContent(
            onNavigate = onNavigate,
            onIntent = onIntent
        )
    }
}

@Composable
private fun MainSettingsContent(
    onNavigate: (String) -> Unit,
    onIntent: (SettingsIntent) -> Unit,
) {
    val context = LocalContext.current
    val settingsWallpaperErrorMessage = stringResource(R.string.settings_wallpaper_error)
    val isCompactLandscape = calculateIsCompactHeight()
    val settingMenuList = remember { settingMenuList }
    val handleItemClick: (SettingsActionType) -> Unit = { actionType ->
        when (actionType) {
            SettingsActionType.COLOR -> onNavigate(SettingsRoute.detail(SettingsMenu.COLOR.name))
            SettingsActionType.IMAGE -> onNavigate(SettingsRoute.detail(SettingsMenu.IMAGE.name))
            SettingsActionType.APP_DRAWER -> onNavigate(SettingsRoute.detail(SettingsMenu.APP_DRAWER.name))
            SettingsActionType.FEED -> onNavigate(SettingsRoute.detail(SettingsMenu.FEED.name))
            SettingsActionType.NOTICE -> onIntent(SettingsIntent.ShowNotice)
            SettingsActionType.WALLPAPER -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
                } catch (_: Exception) {
                    Toast.makeText(context, settingsWallpaperErrorMessage, Toast.LENGTH_SHORT).show()
                }
            }
            SettingsActionType.DEFAULT_HOME -> {
                val homeIntent = Intent(Settings.ACTION_HOME_SETTINGS)
                val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                try {
                    if (homeIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(homeIntent)
                    } else {
                        context.startActivity(settingsIntent)
                    }
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(settingsIntent)
                }
            }
            SettingsActionType.PRESET -> onNavigate(SettingsRoute.detail(SettingsMenu.PRESET.name))
        }
    }

    if(isCompactLandscape){
        // 스마트폰 가로화면인 경우에만 좁은 height 대응 화면
        LandScapeSettingsScreen(settingMenuList, handleItemClick)
    } else {
        // 그외 나머지 화면일 경우
        PortraitSettingsScreen(settingMenuList, handleItemClick)
    }
}
