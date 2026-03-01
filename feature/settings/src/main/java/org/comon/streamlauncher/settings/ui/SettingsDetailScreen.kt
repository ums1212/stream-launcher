package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState
import org.comon.streamlauncher.settings.navigation.SettingsMenu
import org.comon.streamlauncher.ui.modifier.glassEffect
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    menu: SettingsMenu,
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    val title = stringResource(
        when (menu) {
            SettingsMenu.COLOR -> R.string.settings_theme_color
            SettingsMenu.IMAGE -> R.string.settings_home_image
            SettingsMenu.FEED -> R.string.settings_feed
            SettingsMenu.APP_DRAWER -> R.string.settings_app_drawer
            SettingsMenu.PRESET -> R.string.settings_preset
        }
    )
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val glassOnSurface = StreamLauncherTheme.colors.glassOnSurface
    val glassSurface = StreamLauncherTheme.colors.glassSurface

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = glassOnSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = accentPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .glassEffect(overlayColor = glassSurface),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            when (menu) {
                SettingsMenu.COLOR -> ColorSettingsContent(state = state, onIntent = onIntent)
                SettingsMenu.IMAGE -> ImageSettingsContent(state = state, onIntent = onIntent)
                SettingsMenu.FEED -> FeedSettingsContent(state = state, onIntent = onIntent)
                SettingsMenu.APP_DRAWER -> AppDrawerSettingsContent(state = state, onIntent = onIntent)
                SettingsMenu.PRESET -> PresetSettingsContent(state = state, onIntent = onIntent)
            }
        }
    }
}
