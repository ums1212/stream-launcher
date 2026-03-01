package org.comon.streamlauncher.settings.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import coil.compose.AsyncImage
import org.comon.streamlauncher.domain.model.preset.Preset
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
    onNavigateToMarket: () -> Unit = {}
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var presetToLoad by remember { mutableStateOf<Preset?>(null) }
    var presetToDelete by remember { mutableStateOf<Preset?>(null) }
    var presetToUpload by remember { mutableStateOf<Preset?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 프리셋 마켓 진입 버튼
        GlassSettingsTile(
            label = "프리셋 마켓",
            icon = Icons.Default.Store,
            accentColor = MaterialTheme.colorScheme.secondary,
            onClick = onNavigateToMarket,
        )

        // Add Preset Button
        GlassSettingsTile(
            label = stringResource(R.string.title_add_preset),
            icon = Icons.Default.Add,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = {
                if (state.presets.size >= 10) {
                    showLimitDialog = true
                } else {
                    showSaveDialog = true
                }
            }
        )

        Text(
            text = "${stringResource(R.string.title_presets)} (${state.presets.size}/10)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
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

                SwipeToDismissBox(
                    state = dismissState,
                    modifier = Modifier.animateItem(),
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
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
                            onClick = { presetToLoad = preset },
                            onShare = { presetToUpload = preset },
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
            title = { Text("프리셋 저장 불가") },
            text = { Text("저장 가능한 개수를 초과하였습니다.\n기존의 프리셋을 왼쪽 방향으로 스와이프하여 삭제 후 새로 추가해주세요.") },
            confirmButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("확인")
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
                onIntent(SettingsIntent.UploadPreset(preset, description, tags, previewUris))
                presetToUpload = null
            },
        )
    }

    // Delete Confirmation - Auto deleted by swipe but can add a dialog or snackbar if desired
    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text(stringResource(R.string.delete) + "?") },
            text = { Text("Are you sure you want to delete '${preset.name}'?") },
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

    // Upload Progress Dialog
    if (state.isUploading) {
        AlertDialog(
            onDismissRequest = { /* 업로드 중 닫기 불가 */ },
            title = { Text("마켓에 업로드 중...") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "이미지를 압축하고 업로드하는 중입니다.\n잠시만 기다려주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.7f),
                    )
                }
            },
            confirmButton = {},
        )
    }

}

@Composable
fun PresetItemCard(
    preset: Preset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: (() -> Unit)? = null
) {
    val dateString = remember(preset.createdAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(preset.createdAt))
    }
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.7f)
                )
                // Show included settings tags
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
            if (onShare != null) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "공유",
                        tint = Color.Black.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
fun PresetTag(label: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.05f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black
        )
    }
}

@Composable
fun SavePresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, saveHome: Boolean, saveFeed: Boolean, saveDrawer: Boolean, saveWallpaper: Boolean, saveTheme: Boolean, wallpaperUri: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
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
                    label = { Text("프리셋 이름") },
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
                            Text(if (selectedWallpaperUri != null) "선택됨 ✓" else "이미지 선택")
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
                        name.ifEmpty { "프리셋 ${System.currentTimeMillis() % 1000}" },
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
        title = { Text("'$presetName' 마켓에 업로드") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("설명") },
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
                    label = { Text("태그 (쉼표로 구분, 최대 5개)") },
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
                // 프리뷰 이미지 선택
                OutlinedButton(
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("프리뷰 이미지 선택 (최대 4장, ${previewUris.size}장 선택됨)")
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
                Text("업로드")
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
