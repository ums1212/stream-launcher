package org.comon.streamlauncher.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.comon.streamlauncher.domain.model.LiveWallpaper
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.ui.modifier.glassEffect
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewPresetScreen(
    liveWallpapers: List<LiveWallpaper> = emptyList(),
    onSave: (SettingsIntent.SavePreset) -> Unit,
    onBack: () -> Unit,
) {
    val defaultPresetNameFormat = stringResource(R.string.preset_default_name)
    var name by remember { mutableStateOf("") }
    var saveHome by remember { mutableStateOf(true) }
    var saveFeed by remember { mutableStateOf(true) }
    var saveDrawer by remember { mutableStateOf(true) }
    var saveWallpaper by remember { mutableStateOf(true) }
    var saveTheme by remember { mutableStateOf(true) }
    var selectedWallpaperUri by remember { mutableStateOf<String?>(null) }
    var useLiveWallpaper by remember { mutableStateOf(false) }
    var selectedLiveWallpaperId by remember { mutableStateOf<Int?>(null) }
    var showLiveWallpaperPicker by remember { mutableStateOf(false) }
    var pendingPreviousWallpaperUri by remember { mutableStateOf<String?>(null) }
    var pendingPreviousLiveWallpaperId by remember { mutableStateOf<Int?>(null) }
    var selectedLiveWallpaperLandscapeId by remember { mutableStateOf<Int?>(null) }
    var showLiveWallpaperLandscapePicker by remember { mutableStateOf(false) }
    var selectedStaticWallpaperLandscapeUri by remember { mutableStateOf<String?>(null) }

    val selectedLiveWallpaperUri = liveWallpapers.find { it.id == selectedLiveWallpaperId }?.fileUri
    val selectedLiveWallpaperLandscapeUri = liveWallpapers.find { it.id == selectedLiveWallpaperLandscapeId }?.fileUri

    val wallpaperPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedWallpaperUri = uri.toString()
            pendingPreviousLiveWallpaperId = null
        } else if (pendingPreviousLiveWallpaperId != null) {
            useLiveWallpaper = true
            selectedLiveWallpaperId = pendingPreviousLiveWallpaperId
            pendingPreviousLiveWallpaperId = null
        }
    }

    val staticLandscapePicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) selectedStaticWallpaperLandscapeUri = uri.toString()
    }

    val isConfirmEnabled = !saveWallpaper || when {
        useLiveWallpaper -> selectedLiveWallpaperId != null
        else -> selectedWallpaperUri != null
    }

    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val glassOnSurface = StreamLauncherTheme.colors.glassOnSurface
    val glassSurface = StreamLauncherTheme.colors.glassSurface

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_add_preset),
                        color = glassOnSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = accentPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
            ) {
                Button(
                    enabled = isConfirmEnabled,
                    onClick = {
                        onSave(
                            SettingsIntent.SavePreset(
                                name = name.ifEmpty {
                                    String.format(defaultPresetNameFormat, System.currentTimeMillis() % 1000)
                                },
                                saveHome = saveHome,
                                saveFeed = saveFeed,
                                saveDrawer = saveDrawer,
                                saveWallpaper = saveWallpaper,
                                saveTheme = saveTheme,
                                wallpaperUri = if (saveWallpaper) {
                                    if (useLiveWallpaper) selectedLiveWallpaperUri else selectedWallpaperUri
                                } else null,
                                isLiveWallpaper = saveWallpaper && useLiveWallpaper && selectedLiveWallpaperId != null,
                                wallpaperLandscapeUri = if (saveWallpaper) selectedLiveWallpaperLandscapeUri else null,
                                isLiveWallpaperLandscape = saveWallpaper && selectedLiveWallpaperLandscapeId != null,
                                staticWallpaperLandscapeUri = if (saveWallpaper && !useLiveWallpaper) selectedStaticWallpaperLandscapeUri else null,
                            )
                        )
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .glassEffect(overlayColor = glassSurface),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.preset_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
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

            // 정적 가로 배경화면 (세로 정적 배경화면이 선택된 경우)
            if (!useLiveWallpaper && selectedWallpaperUri != null && saveWallpaper) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.preset_static_wallpaper_landscape_optional),
                    style = MaterialTheme.typography.labelMedium,
                )
                RadioButtonRow(
                    label = stringResource(R.string.preset_wallpaper_select_image) +
                            if (selectedStaticWallpaperLandscapeUri != null) " ✓" else "",
                    selected = selectedStaticWallpaperLandscapeUri != null,
                    onClick = { staticLandscapePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                ) {
                    if (selectedStaticWallpaperLandscapeUri != null) {
                        AsyncImage(
                            model = selectedStaticWallpaperLandscapeUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            // 가로 라이브 배경화면 (라이브 배경화면이 선택된 경우)
            if (liveWallpapers.isNotEmpty() && useLiveWallpaper && saveWallpaper) {
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

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

    if (showLiveWallpaperPicker) {
        AlertDialog(
            onDismissRequest = {
                showLiveWallpaperPicker = false
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
                    pendingPreviousWallpaperUri = null
                }) {
                    Text(stringResource(R.string.preset_confirm))
                }
            },
        )
    }
}
