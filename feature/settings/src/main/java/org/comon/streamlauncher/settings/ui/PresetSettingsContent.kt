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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.app.Activity
import android.net.Uri
import android.content.Intent
import android.provider.Settings
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState
import org.comon.streamlauncher.settings.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PresetSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetToLoad by remember { mutableStateOf<Preset?>(null) }
    var presetToDelete by remember { mutableStateOf<Preset?>(null) }

    val context = LocalContext.current
    val permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    var pendingSaveName by remember { mutableStateOf("") }
    var pendingSaveHome by remember { mutableStateOf(false) }
    var pendingSaveFeed by remember { mutableStateOf(false) }
    var pendingSaveDrawer by remember { mutableStateOf(false) }
    var pendingSaveWallpaper by remember { mutableStateOf(false) }
    var pendingSaveTheme by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onIntent(SettingsIntent.SavePreset(pendingSaveName, pendingSaveHome, pendingSaveFeed, pendingSaveDrawer, pendingSaveWallpaper, pendingSaveTheme))
        } else {
            // Permission denied
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionName)) {
                // Permanently denied or policy-blocked, show custom rationale dialog to redirect to settings
                showPermissionRationaleDialog = true
            } else {
                // Denied once but still can ask (or just normal deny), save without wallpaper
                onIntent(SettingsIntent.SavePreset(pendingSaveName, pendingSaveHome, pendingSaveFeed, pendingSaveDrawer, false, pendingSaveTheme))
            }
        }
        showSaveDialog = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Add Preset Button
        GlassSettingsTile(
            label = stringResource(R.string.title_add_preset),
            icon = Icons.Default.Add,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = {
                if (state.presets.size >= 10) {
                    onIntent(SettingsIntent.ShowNotice)
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
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { dismissValue ->
                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                            presetToDelete = preset
                        }
                        false // Always bounce back, deletion confirms via dialog
                    }
                )

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
                            onClick = { presetToLoad = preset }
                        )
                    }
                )
            }
        }
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, saveHome, saveFeed, saveDrawer, saveWallpaper, saveTheme ->
                if (saveWallpaper) {
                    if (ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED) {
                        onIntent(SettingsIntent.SavePreset(name, saveHome, saveFeed, saveDrawer, true, saveTheme))
                        showSaveDialog = false
                    } else {
                        pendingSaveName = name
                        pendingSaveHome = saveHome
                        pendingSaveFeed = saveFeed
                        pendingSaveDrawer = saveDrawer
                        pendingSaveWallpaper = true
                        pendingSaveTheme = saveTheme
                        permissionLauncher.launch(permissionName)
                    }
                } else {
                    onIntent(SettingsIntent.SavePreset(name, saveHome, saveFeed, saveDrawer, false, saveTheme))
                    showSaveDialog = false
                }
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

    // Permission Rationale Dialog
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionRationaleDialog = false
                // On dismiss without going to settings, fallback to saving without wallpaper
                onIntent(SettingsIntent.SavePreset(pendingSaveName, pendingSaveHome, pendingSaveFeed, pendingSaveDrawer, false, pendingSaveTheme))
            },
            title = { Text(stringResource(R.string.preset_permission_title)) },
            text = { Text(stringResource(R.string.preset_permission_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationaleDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    // After returning from settings, user has to try saving again
                }) {
                    Text(stringResource(R.string.preset_permission_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionRationaleDialog = false
                    onIntent(SettingsIntent.SavePreset(pendingSaveName, pendingSaveHome, pendingSaveFeed, pendingSaveDrawer, false, pendingSaveTheme))
                }) {
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
    modifier: Modifier = Modifier
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
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (preset.hasTopLeftImage || preset.hasTopRightImage || preset.hasBottomLeftImage || preset.hasBottomRightImage) PresetTag("Home")
                if (preset.hasFeedSettings) PresetTag("Feed")
                if (preset.hasAppDrawerSettings) PresetTag("Drawer")
                if (preset.hasThemeSettings) PresetTag("Theme")
                if (preset.hasWallpaperSettings) PresetTag("Wallpaper")
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
    onConfirm: (String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var saveHome by remember { mutableStateOf(true) }
    var saveFeed by remember { mutableStateOf(true) }
    var saveDrawer by remember { mutableStateOf(true) }
    var saveWallpaper by remember { mutableStateOf(true) }
    var saveTheme by remember { mutableStateOf(true) }

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
                CheckboxRow(stringResource(R.string.preset_item_wallpaper), saveWallpaper) { saveWallpaper = it }
                CheckboxRow(stringResource(R.string.preset_item_theme), saveTheme) { saveTheme = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.ifEmpty { "프리셋 ${System.currentTimeMillis() % 1000}" }, saveHome, saveFeed, saveDrawer, saveWallpaper, saveTheme) }
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
