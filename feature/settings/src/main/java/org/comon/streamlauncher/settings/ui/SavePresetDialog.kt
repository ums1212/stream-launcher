package org.comon.streamlauncher.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
                isLiveWallpaper: Boolean,
                wallpaperLandscapeUri: String?,
                isLiveWallpaperLandscape: Boolean) -> Unit,
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
    // 전환 취소 시 복원을 위한 임시 저장
    var pendingPreviousWallpaperUri by remember { mutableStateOf<String?>(null) }
    var pendingPreviousLiveWallpaperId by remember { mutableStateOf<Int?>(null) }
    // 가로 라이브 배경화면
    var selectedLiveWallpaperLandscapeId by remember { mutableStateOf<Int?>(null) }
    var showLiveWallpaperLandscapePicker by remember { mutableStateOf(false) }

    val selectedLiveWallpaperUri = liveWallpapers.find { it.id == selectedLiveWallpaperId }?.fileUri
    val selectedLiveWallpaperLandscapeUri = liveWallpapers.find { it.id == selectedLiveWallpaperLandscapeId }?.fileUri

    val wallpaperPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedWallpaperUri = uri.toString()
            pendingPreviousLiveWallpaperId = null
        } else if (pendingPreviousLiveWallpaperId != null) {
            // 이미지 피커 취소 → 라이브 배경화면 선택으로 복원
            useLiveWallpaper = true
            selectedLiveWallpaperId = pendingPreviousLiveWallpaperId
            pendingPreviousLiveWallpaperId = null
        }
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
                CheckboxRow(stringResource(R.string.preset_item_theme), saveTheme, onCheckedChange = { saveTheme = it })

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
                        label = stringResource(R.string.preset_wallpaper_select_image) +
                                if (selectedWallpaperUri != null) " ✓" else "",
                        selected = !useLiveWallpaper,
                        onClick = {
                            if (useLiveWallpaper) {
                                pendingPreviousLiveWallpaperId = selectedLiveWallpaperId
                                selectedLiveWallpaperId = null
                            }
                            useLiveWallpaper = false
                            wallpaperPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        },
                    ) {
                        if (selectedWallpaperUri != null) {
                            AsyncImage(
                                model = selectedWallpaperUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    if (liveWallpapers.isNotEmpty()) {
                        RadioButtonRow(
                            label = stringResource(R.string.live_wallpaper_select_for_preset) +
                                    if (selectedLiveWallpaperId != null) " ✓" else "",
                            selected = useLiveWallpaper,
                            onClick = {
                                if (!useLiveWallpaper) {
                                    pendingPreviousWallpaperUri = selectedWallpaperUri
                                    selectedWallpaperUri = null
                                }
                                useLiveWallpaper = true
                                showLiveWallpaperPicker = true
                            },
                        ) {
                            val selectedLw = liveWallpapers.find { it.id == selectedLiveWallpaperId }
                            val thumbUri = selectedLw?.thumbnailUri ?: selectedLw?.fileUri
                            if (thumbUri != null) {
                                AsyncImage(
                                    model = thumbUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }

                // 가로 라이브 배경화면 (선택사항)
                if (liveWallpapers.isNotEmpty() && saveWallpaper) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.preset_wallpaper_landscape_optional),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    RadioButtonRow(
                        label = stringResource(R.string.live_wallpaper_select_for_preset) +
                                if (selectedLiveWallpaperLandscapeId != null) " ✓" else "",
                        selected = selectedLiveWallpaperLandscapeId != null,
                        onClick = { showLiveWallpaperLandscapePicker = true },
                    ) {
                        val selectedLw = liveWallpapers.find { it.id == selectedLiveWallpaperLandscapeId }
                        val thumbUri = selectedLw?.thumbnailUri ?: selectedLw?.fileUri
                        if (thumbUri != null) {
                            AsyncImage(
                                model = thumbUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
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
                        if (saveWallpaper) selectedLiveWallpaperLandscapeUri else null,
                        saveWallpaper && selectedLiveWallpaperLandscapeId != null,
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

    // 가로 라이브 배경화면 선택 다이얼로그
    if (showLiveWallpaperLandscapePicker) {
        AlertDialog(
            onDismissRequest = { showLiveWallpaperLandscapePicker = false },
            title = { Text(stringResource(R.string.live_wallpaper_select_for_preset)) },
            text = {
                Column {
                    RadioButtonRow(
                        label = stringResource(R.string.preset_wallpaper_landscape_none),
                        selected = selectedLiveWallpaperLandscapeId == null,
                        onClick = { selectedLiveWallpaperLandscapeId = null },
                    )
                    liveWallpapers.forEach { lw ->
                        RadioButtonRow(
                            label = lw.name,
                            selected = selectedLiveWallpaperLandscapeId == lw.id,
                            onClick = { selectedLiveWallpaperLandscapeId = lw.id },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLiveWallpaperLandscapePicker = false }) {
                    Text(stringResource(R.string.preset_confirm))
                }
            },
        )
    }

    // 라이브 배경화면 선택 다이얼로그
    if (showLiveWallpaperPicker) {
        AlertDialog(
            onDismissRequest = {
                showLiveWallpaperPicker = false
                // 취소 시: 이전 이미지 선택 상태로 복원
                if (pendingPreviousWallpaperUri != null) {
                    useLiveWallpaper = false
                    selectedWallpaperUri = pendingPreviousWallpaperUri
                    selectedLiveWallpaperId = null
                    pendingPreviousWallpaperUri = null
                }
            },
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
                TextButton(onClick = {
                    showLiveWallpaperPicker = false
                    pendingPreviousWallpaperUri = null  // 확인 시 복원 불필요
                }) {
                    Text(stringResource(R.string.preset_confirm))
                }
            },
        )
    }
}
