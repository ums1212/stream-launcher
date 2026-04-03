package org.comon.streamlauncher.settings.ui

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.staticwallpaper.StaticWallpaperSettingsIntent
import org.comon.streamlauncher.settings.staticwallpaper.StaticWallpaperSettingsViewModel
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

@Composable
internal fun StaticWallpaperSettingsContent(
    modifier: Modifier = Modifier,
    viewModel: StaticWallpaperSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isLandscapeTab = state.selectedStaticWallpaperTab == WallpaperOrientation.LANDSCAPE
    val currentUri = if (isLandscapeTab) state.staticWallpaperLandscapeUri else state.staticWallpaperPortraitUri

    val configuration = LocalConfiguration.current
    val isCurrentLandscape = remember(configuration.orientation) {
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val orientation = if (isLandscapeTab) WallpaperOrientation.LANDSCAPE else WallpaperOrientation.PORTRAIT
            viewModel.handleIntent(StaticWallpaperSettingsIntent.SetStaticWallpaper(uri.toString(), orientation, isCurrentLandscape))
        }
    }

    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val glassOnSurface = StreamLauncherTheme.colors.glassOnSurface

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PrimaryTabRow(
            selectedTabIndex = if (isLandscapeTab) 1 else 0,
            containerColor = Color.Transparent,
            contentColor = accentPrimary,
        ) {
            Tab(
                selected = !isLandscapeTab,
                onClick = { viewModel.handleIntent(StaticWallpaperSettingsIntent.SwitchStaticWallpaperTab(WallpaperOrientation.PORTRAIT)) },
                text = { Text(stringResource(R.string.wallpaper_orientation_portrait)) },
            )
            Tab(
                selected = isLandscapeTab,
                onClick = { viewModel.handleIntent(StaticWallpaperSettingsIntent.SwitchStaticWallpaperTab(WallpaperOrientation.LANDSCAPE)) },
                text = { Text(stringResource(R.string.wallpaper_orientation_landscape)) },
            )
        }

        if (isLandscapeTab) {
            Text(
                text = stringResource(R.string.wallpaper_landscape_fallback_hint),
                style = MaterialTheme.typography.bodySmall,
                color = glassOnSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        if (currentUri != null) {
            AsyncImage(
                model = currentUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }

        Button(
            onClick = {
                pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
        ) {
            Text(
                text = if (currentUri != null) {
                    stringResource(R.string.static_wallpaper_change)
                } else {
                    stringResource(R.string.static_wallpaper_select)
                },
                color = glassOnSurface,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
