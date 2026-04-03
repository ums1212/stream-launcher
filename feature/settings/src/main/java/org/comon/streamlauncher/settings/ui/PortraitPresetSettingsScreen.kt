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
import org.comon.streamlauncher.domain.model.preset.PresetOperationProgress
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.preset.PresetSettingsIntent

@Composable
internal fun PortraitPresetSettingsScreen(
    presets: List<Preset>,
    pendingUploadPresetName: String?,
    uploadProgress: PresetOperationProgress?,
    isUploadInProgress: Boolean,
    onNavigateToMarket: () -> Unit,
    addPresetEvent: () -> Unit,
    presetToDeleteEvent: (Preset) -> Unit,
    presetItemCardOnclickEvent: (Preset) -> Unit,
    onShareEvent: ((Preset) -> Unit)?,
    onIntent: (PresetSettingsIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassSettingsTile(
                label = stringResource(R.string.preset_market),
                icon = Icons.Default.Store,
                onClick = onNavigateToMarket,
            )
            GlassSettingsTile(
                label = stringResource(R.string.title_add_preset),
                icon = Icons.Default.Add,
                onClick = addPresetEvent,
            )
        }

        Text(
            text = "${stringResource(R.string.title_presets)} (${presets.size}/10)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(presets, key = { it.id }) { preset ->
                PresetSwipeItem(
                    preset = preset,
                    pendingUploadPresetName = pendingUploadPresetName,
                    uploadProgress = uploadProgress,
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
