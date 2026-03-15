package org.comon.streamlauncher.settings.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.roundToInt
import org.comon.streamlauncher.domain.model.ColorPresets
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState
import org.comon.streamlauncher.settings.model.ImageType
import org.comon.streamlauncher.settings.model.SettingsActionType
import org.comon.streamlauncher.settings.model.settingMenuList
import org.comon.streamlauncher.settings.navigation.SettingsMenu
import org.comon.streamlauncher.settings.navigation.SettingsRoute
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import org.comon.streamlauncher.ui.util.calculateIsCompactHeight

@Composable
fun SettingsScreen(
    onIntent: (SettingsIntent) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        MainSettingsContent(
            onNavigate = onNavigate,
            onIntent = onIntent
        )
    }
}

@Composable
private fun MainSettingsContent(
    onNavigate: (String) -> Unit,
    onIntent: (SettingsIntent) -> Unit,
) {
    val context = LocalContext.current
    val settingsWallpaperErrorMessage = stringResource(R.string.settings_wallpaper_error)
    val isCompactLandscape = calculateIsCompactHeight()
    val settingMenuList = remember { settingMenuList }
    val handleItemClick: (SettingsActionType) -> Unit = { actionType ->
        when (actionType) {
            SettingsActionType.COLOR -> onNavigate(SettingsRoute.detail(SettingsMenu.COLOR.name))
            SettingsActionType.IMAGE -> onNavigate(SettingsRoute.detail(SettingsMenu.IMAGE.name))
            SettingsActionType.APP_DRAWER -> onNavigate(SettingsRoute.detail(SettingsMenu.APP_DRAWER.name))
            SettingsActionType.FEED -> onNavigate(SettingsRoute.detail(SettingsMenu.FEED.name))
            SettingsActionType.NOTICE -> onIntent(SettingsIntent.ShowNotice)
            SettingsActionType.WALLPAPER -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
                } catch (_: Exception) {
                    Toast.makeText(context, settingsWallpaperErrorMessage, Toast.LENGTH_SHORT).show()
                }
            }
            SettingsActionType.DEFAULT_HOME -> {
                val homeIntent = Intent(Settings.ACTION_HOME_SETTINGS)
                val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                try {
                    if (homeIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(homeIntent)
                    } else {
                        context.startActivity(settingsIntent)
                    }
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(settingsIntent)
                }
            }
            SettingsActionType.PRESET -> onNavigate(SettingsRoute.detail(SettingsMenu.PRESET.name))
        }
    }

    if(isCompactLandscape){
        // 스마트폰 가로화면인 경우에만 좁은 height 대응 화면
        LandScapeSettingsScreen(settingMenuList, handleItemClick)
    } else {
        // 그외 나머지 화면일 경우
        PortraitSettingsScreen(settingMenuList, handleItemClick)
    }
}

@Composable
internal fun ColorSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ColorPresets.defaults) { preset ->
                val isSelected = state.colorPresetIndex == preset.index
                val primary = Color(preset.accentPrimaryArgb)
                val secondary = Color(preset.accentSecondaryArgb)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(shape)
                        .drawBehind {
                            val half = size.width / 2f
                            drawRect(color = primary, topLeft = Offset.Zero, size = size.copy(width = half))
                            drawRect(color = secondary, topLeft = Offset(half, 0f), size = size.copy(width = half))
                        }
                        .then(
                            if (isSelected) {
                                Modifier.border(width = 3.dp, color = accentPrimary, shape = shape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onIntent(SettingsIntent.ChangeAccentColor(preset.index))
                        },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.settings_color_selected),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ImageSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val cellShape = RoundedCornerShape(8.dp)

    var selectedCell by remember { mutableStateOf(GridCell.TOP_LEFT) }
    var showResetDialog by remember { mutableStateOf(false) }

    val idleImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        onIntent(SettingsIntent.SetGridImage(selectedCell, ImageType.IDLE, uri.toString()))
    }

    val expandedImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        onIntent(SettingsIntent.SetGridImage(selectedCell, ImageType.EXPANDED, uri.toString()))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 2×2 미니 그리드 — 셀 선택
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                GridCell.TOP_LEFT to GridCell.TOP_RIGHT,
                GridCell.BOTTOM_LEFT to GridCell.BOTTOM_RIGHT,
            ).forEach { (left, right) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(left, right).forEach { cell ->
                        val isSelected = selectedCell == cell
                        val cellImage = state.gridCellImages[cell]

                        Surface(
                            shape = cellShape,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accentPrimary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = cellShape,
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedCell = cell
                                },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                val hasIdle = cellImage?.idleImageUri != null
                                val hasExpanded = cellImage?.expandedImageUri != null

                                if (hasIdle && hasExpanded) {
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(cellImage.idleImageUri)
                                                    .crossfade(300)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.3f)),
                                            )
                                            Text(
                                                text = stringResource(R.string.settings_idle_label),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .padding(top = 4.dp)
                                                    .background(
                                                        color = accentPrimary.copy(alpha = 0.8f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(cellImage.expandedImageUri)
                                                    .crossfade(300)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.3f)),
                                            )
                                            Text(
                                                text = stringResource(R.string.settings_expanded_label),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .padding(top = 4.dp)
                                                    .background(
                                                        color = StreamLauncherTheme.colors.accentSecondary.copy(alpha = 0.8f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                } else if (hasIdle) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(cellImage.idleImageUri)
                                            .crossfade(300)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_idle_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 4.dp)
                                            .background(
                                                color = accentPrimary.copy(alpha = 0.8f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                } else if (hasExpanded) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(cellImage.expandedImageUri)
                                            .crossfade(300)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_expanded_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 4.dp)
                                            .background(
                                                color = StreamLauncherTheme.colors.accentSecondary.copy(alpha = 0.8f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                                Text(
                                    text = when (cell) {
                                        GridCell.TOP_LEFT -> stringResource(R.string.grid_cell_top_left)
                                        GridCell.TOP_RIGHT -> stringResource(R.string.grid_cell_top_right)
                                        GridCell.BOTTOM_LEFT -> stringResource(R.string.grid_cell_bottom_left)
                                        GridCell.BOTTOM_RIGHT -> stringResource(R.string.grid_cell_bottom_right)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    idleImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
            ) {
                Text(text = stringResource(R.string.settings_idle_image))
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    expandedImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = StreamLauncherTheme.colors.accentSecondary),
            ) {
                Text(text = stringResource(R.string.settings_expanded_image))
            }
        }

        OutlinedButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showResetDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.settings_image_reset))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            text = {
                Text(text = stringResource(R.string.settings_image_reset_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onIntent(SettingsIntent.ResetAllGridImages)
                        showResetDialog = false
                    },
                ) {
                    Text(text = stringResource(R.string.preset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
internal fun FeedSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary

    var chzzkChannelId by remember(state.chzzkChannelId) { mutableStateOf(state.chzzkChannelId) }
    var youtubeChannelId by remember(state.youtubeChannelId) { mutableStateOf(state.youtubeChannelId) }

    val chzzkError by remember {
        derivedStateOf {
            chzzkChannelId.any { it.isWhitespace() }
        }
    }
    val youtubeError by remember {
        derivedStateOf {
            youtubeChannelId.any { it.isWhitespace() }
        }
    }
    val isSaveEnabled by remember {
        derivedStateOf { !chzzkError && !youtubeError }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        OutlinedTextField(
            value = chzzkChannelId,
            onValueChange = { chzzkChannelId = it },
            label = { Text(stringResource(R.string.settings_chzzk_channel_id)) },
            isError = chzzkError,
            supportingText = if (chzzkError) {
                { Text(stringResource(R.string.settings_no_whitespace)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = youtubeChannelId,
            onValueChange = { youtubeChannelId = it },
            label = { Text(stringResource(R.string.settings_youtube_channel_id)) },
            placeholder = { Text(stringResource(R.string.settings_youtube_placeholder)) },
            isError = youtubeError,
            supportingText = if (youtubeError) {
                { Text(stringResource(R.string.settings_no_whitespace)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onIntent(
                    SettingsIntent.SaveFeedSettings(
                        chzzkChannelId = chzzkChannelId.trim(),
                        youtubeChannelId = youtubeChannelId.trim(),
                    ),
                )
            },
            enabled = isSaveEnabled,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
        ) {
            Text(text = stringResource(R.string.settings_save))
        }
    }
}

@Composable
internal fun AppDrawerSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary

    var columns by remember(state.appDrawerGridColumns) { mutableIntStateOf(state.appDrawerGridColumns) }
    var rows by remember(state.appDrawerGridRows) { mutableIntStateOf(state.appDrawerGridRows) }
    var iconSizeRatio by remember(state.appDrawerIconSizeRatio) { mutableFloatStateOf(state.appDrawerIconSizeRatio) }

    val hasChanges by remember(columns, rows, iconSizeRatio, state.appDrawerGridColumns, state.appDrawerGridRows, state.appDrawerIconSizeRatio) {
        derivedStateOf {
            columns != state.appDrawerGridColumns ||
                rows != state.appDrawerGridRows ||
                iconSizeRatio != state.appDrawerIconSizeRatio
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.settings_columns), style = MaterialTheme.typography.bodyLarge)
                Text(text = "$columns", style = MaterialTheme.typography.bodyLarge, color = accentPrimary)
            }
            Slider(
                value = columns.toFloat(),
                onValueChange = { columns = it.roundToInt() },
                valueRange = 3f..6f,
                steps = 2,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.settings_rows), style = MaterialTheme.typography.bodyLarge)
                Text(text = "$rows", style = MaterialTheme.typography.bodyLarge, color = accentPrimary)
            }
            Slider(
                value = rows.toFloat(),
                onValueChange = { rows = it.roundToInt() },
                valueRange = 4f..8f,
                steps = 3,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.settings_icon_size_ratio), style = MaterialTheme.typography.bodyLarge)
                Text(text = "${(iconSizeRatio * 100).roundToInt()}%", style = MaterialTheme.typography.bodyLarge, color = accentPrimary)
            }
            Slider(
                value = iconSizeRatio,
                onValueChange = { iconSizeRatio = (it * 20).roundToInt() / 20f },
                valueRange = 0.5f..1.5f,
                steps = 19,
            )
            Text(
                text = stringResource(R.string.settings_icon_clip_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    columns = 4
                    rows = 6
                    iconSizeRatio = 1.0f
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(R.string.settings_reset))
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIntent(SettingsIntent.SaveAppDrawerSettings(columns, rows, iconSizeRatio))
                },
                enabled = hasChanges,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
            ) {
                Text(text = stringResource(R.string.settings_save))
            }
        }
    }
}
