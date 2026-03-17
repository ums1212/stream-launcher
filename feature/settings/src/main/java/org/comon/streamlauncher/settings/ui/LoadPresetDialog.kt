package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.comon.streamlauncher.settings.R

@Composable
internal fun LoadPresetDialog(
    presetName: String,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var loadHome by remember { mutableStateOf(true) }
    var loadFeed by remember { mutableStateOf(true) }
    var loadDrawer by remember { mutableStateOf(true) }
    var loadWallpaper by remember { mutableStateOf(true) }
    var loadTheme by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.desc_load_preset) + " '$presetName'") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.preset_items_to_load), style = MaterialTheme.typography.titleSmall)
                CheckboxRow(stringResource(R.string.preset_item_home), loadHome) { loadHome = it }
                CheckboxRow(stringResource(R.string.preset_item_feed), loadFeed) { loadFeed = it }
                CheckboxRow(stringResource(R.string.preset_item_drawer), loadDrawer) { loadDrawer = it }
                CheckboxRow(stringResource(R.string.preset_item_wallpaper), loadWallpaper) { loadWallpaper = it }
                CheckboxRow(stringResource(R.string.preset_item_theme), loadTheme) { loadTheme = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(loadHome, loadFeed, loadDrawer, loadWallpaper, loadTheme) }
            ) {
                Text(stringResource(R.string.preset_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}