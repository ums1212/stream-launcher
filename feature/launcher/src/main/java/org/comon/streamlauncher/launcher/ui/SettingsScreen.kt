package org.comon.streamlauncher.launcher.ui

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.roundToInt
import org.comon.streamlauncher.domain.model.ColorPresets
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.launcher.HomeIntent
import org.comon.streamlauncher.launcher.HomeState
import org.comon.streamlauncher.launcher.model.ImageType
import org.comon.streamlauncher.launcher.model.SettingsTab
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

@Composable
fun SettingsScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.currentSettingsTab != SettingsTab.MAIN) {
        onIntent(HomeIntent.ChangeSettingsTab(SettingsTab.MAIN))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        when (state.currentSettingsTab) {
            SettingsTab.MAIN -> MainSettingsContent(onIntent = onIntent)
            SettingsTab.COLOR -> ColorSettingsContent(state = state, onIntent = onIntent)
            SettingsTab.IMAGE -> ImageSettingsContent(state = state, onIntent = onIntent)
            SettingsTab.FEED -> FeedSettingsContent(state = state, onIntent = onIntent)
            SettingsTab.APP_DRAWER -> AppDrawerSettingsContent(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun MainSettingsContent(onIntent: (HomeIntent) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val accentSecondary = StreamLauncherTheme.colors.accentSecondary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsButton(
                label = "테마 컬러",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIntent(HomeIntent.ChangeSettingsTab(SettingsTab.COLOR))
                },
                containerColor = accentPrimary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            SettingsButton(
                label = "홈 이미지",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIntent(HomeIntent.ChangeSettingsTab(SettingsTab.IMAGE))
                },
                containerColor = accentSecondary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsButton(
                label = "앱 서랍",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIntent(HomeIntent.ChangeSettingsTab(SettingsTab.APP_DRAWER))
                },
                containerColor = accentPrimary.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.width(16.dp))
            SettingsButton(
                label = "피드 설정",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIntent(HomeIntent.ChangeSettingsTab(SettingsTab.FEED))
                },
                containerColor = accentSecondary.copy(alpha = 0.7f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsButton(
                label = "배경화면",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    try {
                        context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
                    } catch (e: Exception) {
                        Toast.makeText(context, "배경화면 설정 창을 열 수 없습니다", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = accentPrimary.copy(alpha = 0.4f),
            )
            Spacer(modifier = Modifier.width(16.dp))
            SettingsButton(
                label = "기본 홈 앱",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                containerColor = accentSecondary.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun SettingsButton(
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "buttonScale_$label",
    )

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .scale(scale),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ColorSettingsContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "테마 컬러",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )
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
                            // 좌반: primary, 우반: secondary
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
                            onIntent(HomeIntent.ChangeAccentColor(preset.index))
                        },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "선택됨",
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
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val cellShape = RoundedCornerShape(8.dp)

    var selectedCell by remember { mutableStateOf(GridCell.TOP_LEFT) }

    // 축소 이미지 선택 런처
    val idleImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        onIntent(HomeIntent.SetGridImage(selectedCell, ImageType.IDLE, uri.toString()))
    }

    // 확장 이미지 선택 런처
    val expandedImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        onIntent(HomeIntent.SetGridImage(selectedCell, ImageType.EXPANDED, uri.toString()))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "홈 이미지",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )

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
                                if (cellImage?.idleImageUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(cellImage.idleImageUri)
                                            .crossfade(300)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    // 텍스트 가독성을 위한 반투명 오버레이
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
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

        // 이미지 선택 버튼
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
                Text(text = "축소 이미지")
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
                Text(text = "확장 이미지")
            }
        }
    }
}

@Composable
private fun FeedSettingsContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary

    var chzzkChannelId by remember(state.chzzkChannelId) { mutableStateOf(state.chzzkChannelId) }
    var youtubeChannelId by remember(state.youtubeChannelId) { mutableStateOf(state.youtubeChannelId) }
    var rssUrl by remember(state.rssUrl) { mutableStateOf(state.rssUrl) }

    val rssUrlError by remember {
        derivedStateOf {
            rssUrl.isNotBlank() && !rssUrl.trim().startsWith("http")
        }
    }
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
        derivedStateOf { !rssUrlError && !chzzkError && !youtubeError }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "피드 설정",
            style = MaterialTheme.typography.titleLarge,
        )

        // 치지직 채널 ID
        OutlinedTextField(
            value = chzzkChannelId,
            onValueChange = { chzzkChannelId = it },
            label = { Text("치지직 채널 ID") },
            isError = chzzkError,
            supportingText = if (chzzkError) {
                { Text("공백을 포함할 수 없습니다") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // YouTube 채널 ID
        OutlinedTextField(
            value = youtubeChannelId,
            onValueChange = { youtubeChannelId = it },
            label = { Text("YouTube 채널 ID") },
            placeholder = { Text("@handle 또는 채널 ID") },
            isError = youtubeError,
            supportingText = if (youtubeError) {
                { Text("공백을 포함할 수 없습니다") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // RSS URL
        OutlinedTextField(
            value = rssUrl,
            onValueChange = { rssUrl = it },
            label = { Text("RSS 피드 URL") },
            isError = rssUrlError,
            supportingText = if (rssUrlError) {
                { Text("올바른 URL 형식이 아닙니다") }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // 저장 버튼
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onIntent(
                    HomeIntent.SaveFeedSettings(
                        chzzkChannelId = chzzkChannelId.trim(),
                        youtubeChannelId = youtubeChannelId.trim(),
                        rssUrl = rssUrl.trim(),
                    ),
                )
            },
            enabled = isSaveEnabled,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
        ) {
            Text(text = "저장")
        }
    }
}

@Composable
private fun AppDrawerSettingsContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
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
        Text(
            text = "앱 서랍 설정",
            style = MaterialTheme.typography.titleLarge,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "가로 앱 개수 (열)", style = MaterialTheme.typography.bodyLarge)
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
                Text(text = "세로 앱 개수 (행)", style = MaterialTheme.typography.bodyLarge)
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
                Text(text = "아이콘 크기 배율", style = MaterialTheme.typography.bodyLarge)
                Text(text = "${(iconSizeRatio * 100).roundToInt()}%", style = MaterialTheme.typography.bodyLarge, color = accentPrimary)
            }
            Slider(
                value = iconSizeRatio,
                onValueChange = { iconSizeRatio = (it * 20).roundToInt() / 20f }, // 0.05 단위 스냅
                valueRange = 0.5f..1.5f,
                steps = 19,
            )
            Text(
                text = "디바이스 해상도에 따라 아이콘이 잘릴 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                Text(text = "초기화")
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIntent(HomeIntent.SaveAppDrawerSettings(columns, rows, iconSizeRatio))
//                Toast.makeText(LocalContext.current, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                },
                enabled = hasChanges,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
            ) {
                Text(text = "저장")
            }
        }
    }
}

