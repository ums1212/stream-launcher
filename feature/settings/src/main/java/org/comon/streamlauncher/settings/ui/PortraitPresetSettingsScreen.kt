package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState

@Composable
internal fun PortraitPresetSettingsScreen(
    state: SettingsState,
    isUploadInProgress: Boolean,
    onNavigateToMarket: () -> Unit,
    addPresetEvent: () -> Unit,
    presetToDeleteEvent: (Preset) -> Unit,
    presetItemCardOnclickEvent: (Preset) -> Unit,
    onShareEvent: ((Preset) -> Unit)?,
    onIntent: (SettingsIntent) -> Unit,
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                val dismissState = rememberSwipeToDismissBoxState()

                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        presetToDeleteEvent(preset)
                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                    }
                }

                // 현재 이 preset이 업로드 중인지 확인
                val isPendingForThisPreset = state.pendingUploadPresetName == preset.name && state.uploadProgress == null
                val thisPresetProgress = state.uploadProgress?.takeIf { it.presetName == preset.name }

                SwipeToDismissBox(
                    state = dismissState,
                    modifier = Modifier.animateItem(),
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = !isUploadInProgress,
                    backgroundContent = {
                        val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color, RoundedCornerShape(16.dp))
                                .padding(end = 24.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    },
                    content = {
                        PresetItemCard(
                            preset = preset,
                            onClick = {
                                presetItemCardOnclickEvent(preset)
                            },
                            onShare = {
                                onShareEvent?.invoke(preset)
                            },
                            isPending = isPendingForThisPreset,
                            uploadProgress = thisPresetProgress,
                            onPauseUpload = { onIntent(SettingsIntent.PauseUpload) },
                            onResumeUpload = { onIntent(SettingsIntent.ResumeUpload) },
                            onCancelUpload = { onIntent(SettingsIntent.CancelUpload) },
                        )
                    }
                )
            }
        }
    }
}