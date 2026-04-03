package org.comon.streamlauncher.settings.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.comon.streamlauncher.domain.model.preset.Preset
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.preset.PresetSettingsIntent
import org.comon.streamlauncher.settings.preset.PresetSettingsSideEffect
import org.comon.streamlauncher.settings.preset.PresetSettingsViewModel
import org.comon.streamlauncher.ui.component.GoogleSignInRequiredDialog
import org.comon.streamlauncher.ui.util.calculateIsCompactHeight

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PresetSettingsContent(
    onNavigateToMarket: () -> Unit = {},
    onNavigateToAddPreset: () -> Unit = {},
    onRequireSignIn: () -> Unit = {},
    onStartUploadService: (String) -> Unit = {},
    onStopUploadService: () -> Unit = {},
    viewModel: PresetSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isCompactLandscape = calculateIsCompactHeight()

    var presetToLoad by remember { mutableStateOf<Preset?>(null) }
    var presetToDelete by remember { mutableStateOf<Preset?>(null) }
    var presetToUpload by remember { mutableStateOf<Preset?>(null) }
    var pendingSharePreset by remember { mutableStateOf<Preset?>(null) }
    var showSignInDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn && pendingSharePreset != null) {
            presetToUpload = pendingSharePreset
            pendingSharePreset = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PresetSettingsSideEffect.RequireSignIn -> {
                    showSignInDialog = true
                }
                is PresetSettingsSideEffect.StartUploadService -> {
                    onStartUploadService(effect.presetName)
                }
                is PresetSettingsSideEffect.UploadStarted ->
                    snackbarHostState.showSnackbar("${effect.presetName}을 마켓에 업로드합니다")
                is PresetSettingsSideEffect.UploadSuccess ->
                    snackbarHostState.showSnackbar("프리셋이 마켓에 업로드되었습니다!")
                is PresetSettingsSideEffect.UploadError ->
                    snackbarHostState.showSnackbar("업로드 실패: ${effect.message}")
                is PresetSettingsSideEffect.StopUploadService -> onStopUploadService()
                is PresetSettingsSideEffect.LaunchLiveWallpaperPicker -> {
                    // no-op here: LoadPreset에서 라이브 배경화면 피커가 필요한 경우
                    // LiveWallpaperSettingsContent에서 별도 처리
                }
                is PresetSettingsSideEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
                is PresetSettingsSideEffect.ShowNetworkError -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "네트워크 연결을 확인해주세요",
                        actionLabel = "설정",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        try {
                            context.startActivity(
                                android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    val isUploadInProgress = state.uploadProgress != null || state.pendingUploadPresetName != null

    val notificationPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) {}

    if (isCompactLandscape) {
        LandscapePresetSettingsScreen(
            presets = state.presets,
            pendingUploadPresetName = state.pendingUploadPresetName,
            uploadProgress = state.uploadProgress,
            isUploadInProgress = isUploadInProgress,
            onNavigateToMarket = onNavigateToMarket,
            addPresetEvent = {
                if (state.presets.size >= 10) showLimitDialog = true
                else onNavigateToAddPreset()
            },
            presetToDeleteEvent = { presetToDelete = it },
            presetItemCardOnclickEvent = { if (!isUploadInProgress) presetToLoad = it },
            onShareEvent = if (!isUploadInProgress) {
                { preset ->
                    if (state.isSignedIn) presetToUpload = preset
                    else { pendingSharePreset = preset; showSignInDialog = true }
                }
            } else null,
            onIntent = viewModel::handleIntent,
        )
    } else {
        PortraitPresetSettingsScreen(
            presets = state.presets,
            pendingUploadPresetName = state.pendingUploadPresetName,
            uploadProgress = state.uploadProgress,
            isUploadInProgress = isUploadInProgress,
            onNavigateToMarket = onNavigateToMarket,
            addPresetEvent = {
                if (state.presets.size >= 10) showLimitDialog = true
                else onNavigateToAddPreset()
            },
            presetToDeleteEvent = { presetToDelete = it },
            presetItemCardOnclickEvent = { if (!isUploadInProgress) presetToLoad = it },
            onShareEvent = if (!isUploadInProgress) {
                { preset ->
                    if (state.isSignedIn) presetToUpload = preset
                    else { pendingSharePreset = preset; showSignInDialog = true }
                }
            } else null,
            onIntent = viewModel::handleIntent,
        )
    }

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

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text(stringResource(R.string.preset_limit_title)) },
            text = { Text(stringResource(R.string.preset_limit_message)) },
            confirmButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text(stringResource(R.string.preset_confirm))
                }
            },
        )
    }

    presetToLoad?.let { preset ->
        LoadPresetDialog(
            presetName = preset.name,
            onDismiss = { presetToLoad = null },
            onConfirm = { loadHome, loadFeed, loadDrawer, loadWallpaper, loadTheme ->
                viewModel.handleIntent(PresetSettingsIntent.LoadPreset(preset, loadHome, loadFeed, loadDrawer, loadWallpaper, loadTheme))
                presetToLoad = null
            },
        )
    }

    presetToUpload?.let { preset ->
        UploadToMarketDialog(
            presetName = preset.name,
            onDismiss = { presetToUpload = null },
            onUpload = { description, tags, previewUris ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.handleIntent(PresetSettingsIntent.UploadPreset(preset, description, tags, previewUris))
                presetToUpload = null
            },
        )
    }

    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text(stringResource(R.string.delete) + "?") },
            text = { Text(stringResource(R.string.preset_delete_confirm, preset.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.handleIntent(PresetSettingsIntent.DeletePreset(preset))
                    presetToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
