package org.comon.streamlauncher.settings.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import coil.compose.AsyncImage
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.domain.model.preset.UploadProgress
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState
import org.comon.streamlauncher.settings.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PresetSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToMarket: () -> Unit = {},
    onShowSnackbar: (String) -> Unit = {},
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var presetToLoad by remember { mutableStateOf<Preset?>(null) }
    var presetToDelete by remember { mutableStateOf<Preset?>(null) }
    var presetToUpload by remember { mutableStateOf<Preset?>(null) }

    val isUploadInProgress = state.uploadProgress != null || state.pendingUploadPresetName != null

    // Android 13+ POST_NOTIFICATIONS 권한 요청
    val uploadNotificationDeniedMsg = stringResource(R.string.upload_notification_denied)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (!granted) {
            onShowSnackbar(uploadNotificationDeniedMsg)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row {
            // 프리셋 마켓 진입 버튼
            GlassSettingsTile(
                label = stringResource(R.string.preset_market),
                icon = Icons.Default.Store,
                lerpFraction = 0f,
                onClick = onNavigateToMarket,
            )

            // Add Preset Button
            GlassSettingsTile(
                label = stringResource(R.string.title_add_preset),
                icon = Icons.Default.Add,
                lerpFraction = 0f,
                onClick = {
                    if (state.presets.size >= 10) {
                        showLimitDialog = true
                    } else {
                        showSaveDialog = true
                    }
                }
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
                        presetToDelete = preset
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
                            onClick = { if (!isUploadInProgress) presetToLoad = preset },
                            onShare = if (!isUploadInProgress) {
                                { presetToUpload = preset }
                            } else null,
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

    // Preset Limit Dialog
    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text(stringResource(R.string.preset_limit_title)) },
            text = { Text(stringResource(R.string.preset_limit_message)) },
            confirmButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text(stringResource(R.string.preset_confirm))
                }
            }
        )
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, saveHome, saveFeed, saveDrawer, saveWallpaper, saveTheme, wallpaperUri ->
                onIntent(SettingsIntent.SavePreset(name, saveHome, saveFeed, saveDrawer, saveWallpaper, saveTheme, wallpaperUri))
                showSaveDialog = false
            }
        )
    }

    // Load Preset Dialog
    presetToLoad?.let { preset ->
        LoadPresetDialog(
            presetName = preset.name,
            onDismiss = { presetToLoad = null },
            onConfirm = { loadHome, loadFeed, loadDrawer, loadWallpaper, loadTheme ->
                onIntent(SettingsIntent.LoadPreset(preset, loadHome, loadFeed, loadDrawer, loadWallpaper, loadTheme))
                presetToLoad = null
            }
        )
    }

    // Upload Preset Dialog
    presetToUpload?.let { preset ->
        UploadToMarketDialog(
            presetName = preset.name,
            onDismiss = { presetToUpload = null },
            onUpload = { description, tags, previewUris ->
                // Android 13+ 알림 권한 요청
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                onIntent(SettingsIntent.UploadPreset(preset, description, tags, previewUris))
                presetToUpload = null
            },
        )
    }

    // Delete Confirmation
    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text(stringResource(R.string.delete) + "?") },
            text = { Text(stringResource(R.string.preset_delete_confirm, preset.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(SettingsIntent.DeletePreset(preset))
                    presetToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun PresetItemCard(
    preset: Preset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: (() -> Unit)? = null,
    isPending: Boolean = false,
    uploadProgress: UploadProgress? = null,
    onPauseUpload: () -> Unit = {},
    onResumeUpload: () -> Unit = {},
    onCancelUpload: () -> Unit = {},
) {
    var showCancelUploadDialog by remember { mutableStateOf(false) }

    val dateString = remember(preset.createdAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(preset.createdAt))
    }

    val isUploading = uploadProgress != null || isPending

    if (showCancelUploadDialog) {
        LaunchedEffect(Unit) {
            onPauseUpload()
        }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.upload_cancel_title)) },
            text = { Text(stringResource(R.string.upload_cancel_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showCancelUploadDialog = false
                    onCancelUpload()
                }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onResumeUpload()
                    showCancelUploadDialog = false
                }) {
                    Text(stringResource(R.string.upload_cancel_resume))
                }
            },
        )
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUploading) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (preset.hasTopLeftImage || preset.hasTopRightImage || preset.hasBottomLeftImage || preset.hasBottomRightImage) PresetTag("Home")
                        if (preset.hasFeedSettings) PresetTag("Feed")
                        if (preset.hasAppDrawerSettings) PresetTag("Drawer")
                        if (preset.hasThemeSettings) PresetTag("Theme")
                        if (preset.hasWallpaperSettings) PresetTag("Wallpaper")
                    }
                }
                if (!isUploading && onShare != null) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.preset_share_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 업로드 진행 표시
            if (isUploading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (uploadProgress != null) {
                            LinearProgressIndicator(
                                progress = { uploadProgress.percentage },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = stringResource(R.string.preset_upload_progress, (uploadProgress.percentage * 100).toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = stringResource(R.string.preset_upload_preparing),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = { showCancelUploadDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.preset_upload_cancel_desc),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetTag(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SavePresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, saveHome: Boolean, saveFeed: Boolean, saveDrawer: Boolean, saveWallpaper: Boolean, saveTheme: Boolean, wallpaperUri: String?) -> Unit,
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

@Composable
fun LoadPresetDialog(
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

@Composable
fun UploadToMarketDialog(
    presetName: String,
    onDismiss: () -> Unit,
    onUpload: (description: String, tags: List<String>, previewUris: List<String>) -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewUris by remember { mutableStateOf<List<String>>(emptyList()) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = PickMultipleVisualMedia(maxItems = 4),
    ) { uris ->
        previewUris = uris.map { it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.preset_upload_to_market, presetName)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.preset_upload_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { input ->
                        val separated = input.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                        if (separated.size > 1) {
                            tags = (tags + separated.dropLast(1)).distinct().take(5)
                            tagInput = separated.last()
                        } else {
                            tagInput = input
                        }
                    },
                    label = { Text(stringResource(R.string.preset_upload_tag_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        tags.forEach { tag ->
                            SuggestionChip(onClick = { tags = tags - tag }, label = { Text(tag) })
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.preset_upload_preview_button, previewUris.size))
                }
                if (previewUris.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        previewUris.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onUpload(description, tags, previewUris) }) {
                Text(stringResource(R.string.preset_upload_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}
