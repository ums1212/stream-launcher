package org.comon.streamlauncher.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.domain.model.LiveWallpaper
import org.comon.streamlauncher.settings.R

@Composable
internal fun SavePresetDialog(
    liveWallpapers: List<LiveWallpaper> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (name: String,
                saveHome: Boolean,
                saveFeed: Boolean,
                saveDrawer: Boolean,
                saveWallpaper: Boolean,
                saveTheme: Boolean,
                wallpaperUri: String?,
                isLiveWallpaper: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val defaultPresetNameFormat = stringResource(R.string.preset_default_name)
    var saveHome by remember { mutableStateOf(true) }
    var saveFeed by remember { mutableStateOf(true) }
    var saveDrawer by remember { mutableStateOf(true) }
    var saveWallpaper by remember { mutableStateOf(true) }
    var saveTheme by remember { mutableStateOf(true) }
    var selectedWallpaperUri by remember { mutableStateOf<String?>(null) }
    var useLiveWallpaper by remember { mutableStateOf(false) }
    var selectedLiveWallpaperId by remember { mutableStateOf<Int?>(null) }
    var showLiveWallpaperPicker by remember { mutableStateOf(false) }

    val selectedLiveWallpaperUri = liveWallpapers.find { it.id == selectedLiveWallpaperId }?.fileUri

    val wallpaperPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        selectedWallpaperUri = uri?.toString()
    }

    val isConfirmEnabled = !saveWallpaper || when {
        useLiveWallpaper -> selectedLiveWallpaperId != null
        else -> selectedWallpaperUri != null
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
                CheckboxRow(stringResource(R.string.preset_item_home), saveHome, onCheckedChange = { saveHome = it })
                CheckboxRow(stringResource(R.string.preset_item_feed), saveFeed, onCheckedChange = { saveFeed = it })
                CheckboxRow(stringResource(R.string.preset_item_drawer), saveDrawer, onCheckedChange = { saveDrawer = it })

                CheckboxRow(
                    label = stringResource(R.string.preset_item_wallpaper),
                    checked = saveWallpaper,
                    onCheckedChange = {
                        saveWallpaper = it
                        if (!it) {
                            selectedWallpaperUri = null
                            selectedLiveWallpaperId = null
                        }
                    },
                ) {
                    RadioButtonRow(
                        label = if (selectedWallpaperUri != null) stringResource(R.string.preset_wallpaper_selected)
                                else stringResource(R.string.preset_wallpaper_select_image),
                        selected = !useLiveWallpaper,
                        onClick = {
                            useLiveWallpaper = false
                            wallpaperPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        },
                    )
                    if (liveWallpapers.isNotEmpty()) {
                        RadioButtonRow(
                            label = if (selectedLiveWallpaperId != null)
                                        liveWallpapers.find { it.id == selectedLiveWallpaperId }?.name
                                            ?: stringResource(R.string.live_wallpaper_select_for_preset)
                                    else stringResource(R.string.live_wallpaper_select_for_preset),
                            selected = useLiveWallpaper,
                            onClick = {
                                useLiveWallpaper = true
                                showLiveWallpaperPicker = true
                            },
                        )
                    }
                }

                CheckboxRow(stringResource(R.string.preset_item_theme), saveTheme, onCheckedChange = { saveTheme = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = isConfirmEnabled,
                onClick = {
                    onConfirm(
                        name.ifEmpty { String.format(defaultPresetNameFormat, System.currentTimeMillis() % 1000) },
                        saveHome, saveFeed, saveDrawer, saveWallpaper, saveTheme,
                        if (saveWallpaper) {
                            if (useLiveWallpaper) selectedLiveWallpaperUri else selectedWallpaperUri
                        } else null,
                        saveWallpaper && useLiveWallpaper && selectedLiveWallpaperId != null,
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

    // 라이브 배경화면 선택 다이얼로그
    if (showLiveWallpaperPicker) {
        AlertDialog(
            onDismissRequest = { showLiveWallpaperPicker = false },
            title = { Text(stringResource(R.string.live_wallpaper_select_for_preset)) },
            text = {
                Column {
                    liveWallpapers.forEach { lw ->
                        RadioButtonRow(
                            label = lw.name,
                            selected = selectedLiveWallpaperId == lw.id,
                            onClick = { selectedLiveWallpaperId = lw.id },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLiveWallpaperPicker = false }) {
                    Text(stringResource(R.string.preset_confirm))
                }
            },
        )
    }
}
