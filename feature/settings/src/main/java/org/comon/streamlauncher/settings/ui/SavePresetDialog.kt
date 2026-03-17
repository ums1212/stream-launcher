package org.comon.streamlauncher.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.settings.R
import kotlin.text.ifEmpty

@Composable
internal fun SavePresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String,
                saveHome: Boolean,
                saveFeed: Boolean,
                saveDrawer: Boolean,
                saveWallpaper: Boolean,
                saveTheme: Boolean,
                wallpaperUri: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val defaultPresetNameFormat = stringResource(R.string.preset_default_name)
    var saveHome by remember { mutableStateOf(true) }
    var saveFeed by remember { mutableStateOf(true) }
    var saveDrawer by remember { mutableStateOf(true) }
    var saveWallpaper by remember { mutableStateOf(true) }
    var saveTheme by remember { mutableStateOf(true) }
    var selectedWallpaperUri by remember { mutableStateOf<String?>(null) }

    val wallpaperPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        selectedWallpaperUri = uri?.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_add_preset)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.preset_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.preset_items_to_save), style = MaterialTheme.typography.titleSmall)
                CheckboxRow(stringResource(R.string.preset_item_home), saveHome) { saveHome = it }
                CheckboxRow(stringResource(R.string.preset_item_feed), saveFeed) { saveFeed = it }
                CheckboxRow(stringResource(R.string.preset_item_drawer), saveDrawer) { saveDrawer = it }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = saveWallpaper, onCheckedChange = {
                        saveWallpaper = it
                        if (!it) selectedWallpaperUri = null
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.preset_item_wallpaper), modifier = Modifier.weight(1f))
                    if (saveWallpaper) {
                        TextButton(
                            onClick = { wallpaperPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
                        ) {
                            Text(if (selectedWallpaperUri != null) stringResource(R.string.preset_wallpaper_selected) else stringResource(R.string.preset_wallpaper_select_image))
                        }
                    }
                }
                CheckboxRow(stringResource(R.string.preset_item_theme), saveTheme) { saveTheme = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saveWallpaper || selectedWallpaperUri != null,
                onClick = {
                    onConfirm(
                        name.ifEmpty { String.format(defaultPresetNameFormat, System.currentTimeMillis() % 1000) },
                        saveHome, saveFeed, saveDrawer, saveWallpaper, saveTheme,
                        if (saveWallpaper) selectedWallpaperUri else null,
                    )
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}