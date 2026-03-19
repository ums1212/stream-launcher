package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState

@Composable
internal fun LandscapePresetSettingsScreen(
    state: SettingsState,
    isUploadInProgress: Boolean,
    onNavigateToMarket: () -> Unit,
    addPresetEvent: () -> Unit,
    presetToDeleteEvent: (Preset) -> Unit,
    presetItemCardOnclickEvent: (Preset) -> Unit,
    onShareEvent: ((Preset) -> Unit)?,
    onIntent: (SettingsIntent) -> Unit,
){
    Row(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 프리셋 마켓 진입 버튼
            GlassSettingsTile(
                label = stringResource(R.string.preset_market),
                icon = Icons.Default.Store,
                onClick = onNavigateToMarket,
            )

            // Add Preset Button
            GlassSettingsTile(
                label = stringResource(R.string.title_add_preset),
                icon = Icons.Default.Add,
                onClick = addPresetEvent
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${stringResource(R.string.title_presets)} (${state.presets.size}/10)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Preset List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.presets, key = { it.id }) { preset ->
                    PresetSwipeItem(
                        preset = preset,
                        state = state,
                        isUploadInProgress = isUploadInProgress,
                        onDelete = presetToDeleteEvent,
                        onClick = presetItemCardOnclickEvent,
                        onShare = onShareEvent,
                        onIntent = onIntent,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}