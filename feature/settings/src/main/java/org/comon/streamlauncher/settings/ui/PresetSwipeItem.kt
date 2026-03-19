package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState

@Composable
internal fun PresetSwipeItem(
    preset: Preset,
    state: SettingsState,
    isUploadInProgress: Boolean,
    onDelete: (Preset) -> Unit,
    onClick: (Preset) -> Unit,
    onShare: ((Preset) -> Unit)?,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete(preset)
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    val isPendingForThisPreset = state.pendingUploadPresetName == preset.name && state.uploadProgress == null
    val thisPresetProgress = state.uploadProgress?.takeIf { it.presetName == preset.name }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
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
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
        content = {
            PresetItemCard(
                preset = preset,
                onClick = { onClick(preset) },
                onShare = { onShare?.invoke(preset) },
                isPending = isPendingForThisPreset,
                uploadProgress = thisPresetProgress,
                onPauseUpload = { onIntent(SettingsIntent.PauseUpload) },
                onResumeUpload = { onIntent(SettingsIntent.ResumeUpload) },
                onCancelUpload = { onIntent(SettingsIntent.CancelUpload) },
            )
        },
    )
}
