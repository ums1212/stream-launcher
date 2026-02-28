package org.comon.streamlauncher.settings.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.comon.streamlauncher.settings.model.SettingsTab
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.currentTab != SettingsTab.MAIN) {
        onIntent(SettingsIntent.ChangeTab(SettingsTab.MAIN))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        when (state.currentTab) {
            SettingsTab.MAIN -> MainSettingsContent(onIntent = onIntent)
            SettingsTab.COLOR -> ColorSettingsContent(state = state, onIntent = onIntent)
            SettingsTab.IMAGE -> ImageSettingsContent(state = state, onIntent = onIntent)
            SettingsTab.FEED -> FeedSettingsContent(state = state, onIntent = onIntent)
            SettingsTab.APP_DRAWER -> AppDrawerSettingsContent(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun MainSettingsContent(onIntent: (SettingsIntent) -> Unit) {
    val context = LocalContext.current
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val accentSecondary = StreamLauncherTheme.colors.accentSecondary
    val settingsWallpaperErrorMessage = stringResource(R.string.settings_wallpaper_error)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            GlassSettingsTile(
                label = stringResource(R.string.settings_theme_color),
                icon = Icons.Rounded.Palette,
                accentColor = accentPrimary,
                onClick = { onIntent(SettingsIntent.ChangeTab(SettingsTab.COLOR)) },
                modifier = Modifier.weight(1f),
            )
            GlassSettingsTile(
                label = stringResource(R.string.settings_home_image),
                icon = Icons.Rounded.Image,
                accentColor = lerp(accentPrimary, accentSecondary, 0.17f),
                onClick = { onIntent(SettingsIntent.ChangeTab(SettingsTab.IMAGE)) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            GlassSettingsTile(
                label = stringResource(R.string.settings_app_drawer),
                icon = Icons.Rounded.GridView,
                accentColor = lerp(accentPrimary, accentSecondary, 0.33f),
                onClick = { onIntent(SettingsIntent.ChangeTab(SettingsTab.APP_DRAWER)) },
                modifier = Modifier.weight(1f),
            )
            GlassSettingsTile(
                label = stringResource(R.string.settings_feed),
                icon = Icons.Rounded.RssFeed,
                accentColor = lerp(accentPrimary, accentSecondary, 0.5f),
                onClick = { onIntent(SettingsIntent.ChangeTab(SettingsTab.FEED)) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            GlassSettingsTile(
                label = stringResource(R.string.settings_notice),
                icon = Icons.Rounded.Campaign,
                accentColor = lerp(accentPrimary, accentSecondary, 0.67f),
                onClick = { onIntent(SettingsIntent.ShowNotice) },
                modifier = Modifier.weight(1f),
            )
            GlassSettingsTile(
                label = stringResource(R.string.settings_wallpaper),
                icon = Icons.Rounded.Wallpaper,
                accentColor = lerp(accentPrimary, accentSecondary, 0.83f),
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
                    } catch (e: Exception) {
                        Toast.makeText(context, settingsWallpaperErrorMessage, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            GlassSettingsTile(
                label = stringResource(R.string.settings_default_home),
                icon = Icons.Rounded.Home,
                accentColor = accentSecondary,
                onClick = {
                    try {
                        val homeIntent = Intent(Settings.ACTION_HOME_SETTINGS)
                        if (homeIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(homeIntent)
                        } else {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun GlassSettingsTile(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tileScale_$label",
    )
    val glassSurface = StreamLauncherTheme.colors.glassSurface
    val glassOnSurface = StreamLauncherTheme.colors.glassOnSurface
    val tileShape = RoundedCornerShape(16.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .height(88.dp)
            .clip(tileShape)
            .background(glassSurface)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.35f),
                shape = tileShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = glassOnSurface,
            )
        }
    }
}

@Composable
private fun SettingsPageHeader(
    title: String,
    icon: ImageVector,
) {
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val glassOnSurface = StreamLauncherTheme.colors.glassOnSurface

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentPrimary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = glassOnSurface,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            color = accentPrimary.copy(alpha = 0.4f),
            thickness = 1.dp,
        )
    }
}

@Composable
private fun ColorSettingsContent(
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
        SettingsPageHeader(
            title = stringResource(R.string.settings_theme_color),
            icon = Icons.Rounded.Palette,
        )
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
private fun ImageSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val cellShape = RoundedCornerShape(8.dp)

    var selectedCell by remember { mutableStateOf(GridCell.TOP_LEFT) }

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
        SettingsPageHeader(
            title = stringResource(R.string.settings_home_image),
            icon = Icons.Rounded.Image,
        )
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
                                    text = cell.name,
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
    }
}

@Composable
private fun FeedSettingsContent(
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
        SettingsPageHeader(
            title = stringResource(R.string.settings_feed_title),
            icon = Icons.Rounded.RssFeed,
        )

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
private fun AppDrawerSettingsContent(
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
        SettingsPageHeader(
            title = stringResource(R.string.settings_app_drawer_title),
            icon = Icons.Rounded.GridView,
        )

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
