package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import kotlin.math.roundToInt

@Composable
internal fun AppDrawerSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary

    var columns by remember(state.appDrawerGridColumns) { mutableIntStateOf(state.appDrawerGridColumns) }
    var rows by remember(state.appDrawerGridRows) { mutableIntStateOf(state.appDrawerGridRows) }
    var iconSizeRatio by remember(state.appDrawerIconSizeRatio) { mutableFloatStateOf(state.appDrawerIconSizeRatio) }

    val hasChanges by remember(columns, rows, iconSizeRatio, state.appDrawerGridColumns, state.appDrawerGridRows, state.appDrawerIconSizeRatio) {
        derivedStateOf {
            columns != state.appDrawerGridColumns ||
                    rows != state.appDrawerGridRows ||
                    iconSizeRatio != state.appDrawerIconSizeRatio
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.settings_columns), style = MaterialTheme.typography.bodyLarge)
                Text(text = "$columns", style = MaterialTheme.typography.bodyLarge, color = accentPrimary)
            }
            Slider(
                value = columns.toFloat(),
                onValueChange = { columns = it.roundToInt() },
                valueRange = 3f..6f,
                steps = 2,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.settings_rows), style = MaterialTheme.typography.bodyLarge)
                Text(text = "$rows", style = MaterialTheme.typography.bodyLarge, color = accentPrimary)
            }
            Slider(
                value = rows.toFloat(),
                onValueChange = { rows = it.roundToInt() },
                valueRange = 4f..8f,
                steps = 3,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.settings_icon_size_ratio), style = MaterialTheme.typography.bodyLarge)
                Text(text = "${(iconSizeRatio * 100).roundToInt()}%", style = MaterialTheme.typography.bodyLarge, color = accentPrimary)
            }
            Slider(
                value = iconSizeRatio,
                onValueChange = { iconSizeRatio = (it * 20).roundToInt() / 20f },
                valueRange = 0.5f..1.5f,
                steps = 19,
            )
            Text(
                text = stringResource(R.string.settings_icon_clip_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = {
                    columns = 4
                    rows = 6
                    iconSizeRatio = 1.0f
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.settings_reset))
            }

            Button(
                onClick = {
                    onIntent(SettingsIntent.SaveAppDrawerSettings(columns, rows, iconSizeRatio))
                },
                enabled = hasChanges,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
            ) {
                Text(text = stringResource(R.string.settings_save))
            }
        }
    }
}