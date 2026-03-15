package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.domain.model.ColorPreset
import org.comon.streamlauncher.domain.model.ColorPresets
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import org.comon.streamlauncher.ui.util.calculateIsCompactHeight

@Composable
internal fun ColorSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val isCompactLandscape = calculateIsCompactHeight()

    if(isCompactLandscape){
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ColorPresets.defaults) { preset ->
                ColorSettingsGridItem(state, preset, onIntent)
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ColorPresets.defaults) { preset ->
                ColorSettingsGridItem(state, preset, onIntent)
            }
        }
    }

}

@Composable
internal fun ColorSettingsGridItem(
    state: SettingsState,
    preset: ColorPreset,
    onIntent: (SettingsIntent) -> Unit,
){
    val shape = RoundedCornerShape(12.dp)
    val isSelected = state.colorPresetIndex == preset.index
    val primary = Color(preset.accentPrimaryArgb)
    val secondary = Color(preset.accentSecondaryArgb)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .drawBehind {
                val half = size.width / 2f
                drawRect(color = primary,
                    topLeft = Offset.Zero,
                    size = size.copy(width = half)
                )
                drawRect(color = secondary,
                    topLeft = Offset(half, 0f),
                    size = size.copy(width = half)
                )
            }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = StreamLauncherTheme.colors.accentPrimary,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable {
                onIntent(SettingsIntent.ChangeAccentColor(preset.index))
            },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.settings_color_selected),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = preset.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}