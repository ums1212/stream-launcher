package org.comon.streamlauncher.settings.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.ui.component.GoogleSignInRequiredDialog
import org.comon.streamlauncher.ui.util.calculateIsCompactHeight

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PresetSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onNavigateToMarket: () -> Unit = {},
    onShowSnackbar: (String) -> Unit = {},
    onRequireSignIn: () -> Unit = {},
) {
    val isCompactLandscape = calculateIsCompactHeight()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var presetToLoad by remember { mutableStateOf<Preset?>(null) }
    var presetToDelete by remember { mutableStateOf<Preset?>(null) }
    var presetToUpload by remember { mutableStateOf<Preset?>(null) }
    var pendingSharePreset by remember { mutableStateOf<Preset?>(null) }
    var showSignInDialog by remember { mutableStateOf(false) }

    // 로그인 완료 후 업로드 다이얼로그를 보여준다
    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn && pendingSharePreset != null) {
            presetToUpload = pendingSharePreset
            pendingSharePreset = null
        }
    }

    val isUploadInProgress = state.uploadProgress != null || state.pendingUploadPresetName != null

    // Android 13+ POST_NOTIFICATIONS 권한 요청
    val uploadNotificationDeniedMsg = stringResource(R.string.upload_notification_denied)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (!granted) {
            onShowSnackbar(uploadNotificationDeniedMsg)
        }
    }

    if(isCompactLandscape){
        LandscapePresetSettingsScreen(
            state = state,
            isUploadInProgress = isUploadInProgress,
            onNavigateToMarket = { onNavigateToMarket() },
            addPresetEvent = {
                if (state.presets.size >= 10) {
                    showLimitDialog = true
                } else {
                    showSaveDialog = true
                }
            },
            presetToDeleteEvent = { presetToDelete = it },
            presetItemCardOnclickEvent = {
                if (!isUploadInProgress) presetToLoad = it
            },
            onShareEvent = if (!isUploadInProgress) {
                { preset ->
                    if (state.isSignedIn) {
                        presetToUpload = preset
                    } else {
                        pendingSharePreset = preset
                        showSignInDialog = true
                    }
                }
            } else null,
            onIntent = onIntent
        )
    } else {
        PortraitPresetSettingsScreen(
            state = state,
            isUploadInProgress = isUploadInProgress,
            onNavigateToMarket = { onNavigateToMarket() },
            addPresetEvent = {
                if (state.presets.size >= 10) {
                    showLimitDialog = true
                } else {
                    showSaveDialog = true
                }
            },
            presetToDeleteEvent = { presetToDelete = it },
            presetItemCardOnclickEvent = {
                if (!isUploadInProgress) presetToLoad = it
            },
            onShareEvent = if (!isUploadInProgress) {
                { preset ->
                    if (state.isSignedIn) {
                        presetToUpload = preset
                    } else {
                        pendingSharePreset = preset
                        showSignInDialog = true
                    }
                }
            } else null,
            onIntent = onIntent
        )
    }

    // Sign-In Required Dialog (업로드 전 로그인 안내)
    if (showSignInDialog) {
        GoogleSignInRequiredDialog(
            onConfirm = {
                showSignInDialog = false
                onRequireSignIn()
            },
            onDismiss = {
                showSignInDialog = false
                pendingSharePreset = null
            },
        )
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
