package org.comon.streamlauncher.launcher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.launcher.HomeIntent
import org.comon.streamlauncher.launcher.HomeState
import org.comon.streamlauncher.ui.dragdrop.LocalDragDropState

@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedCell = state.expandedCell

    val topRowTargetWeight = when (expandedCell) {
        GridCell.TOP_LEFT, GridCell.TOP_RIGHT -> 0.8f
        GridCell.BOTTOM_LEFT, GridCell.BOTTOM_RIGHT -> 0.2f
        null -> 0.5f
    }
    val topRowWeight by animateFloatAsState(
        targetValue = topRowTargetWeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "topRowWeight",
    )
    val bottomRowWeight = 1f - topRowWeight

    val leftColTargetWeight = when (expandedCell) {
        GridCell.TOP_LEFT, GridCell.BOTTOM_LEFT -> 0.8f
        GridCell.TOP_RIGHT, GridCell.BOTTOM_RIGHT -> 0.2f
        null -> 0.5f
    }
    val leftColWeight by animateFloatAsState(
        targetValue = leftColTargetWeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "leftColWeight",
    )
    val rightColWeight = 1f - leftColWeight

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Row(
            modifier = Modifier
                .weight(topRowWeight)
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        ) {
            GridCellContent(
                cell = GridCell.TOP_LEFT,
                weight = leftColWeight,
                isExpanded = expandedCell == GridCell.TOP_LEFT,
                apps = state.appsInCells[GridCell.TOP_LEFT] ?: emptyList(),
                gridCellImage = state.gridCellImages[GridCell.TOP_LEFT],
                pinnedPackages = state.pinnedPackages,
                onIntent = onIntent,
                modifier = Modifier.padding(end = 4.dp),
            )
            GridCellContent(
                cell = GridCell.TOP_RIGHT,
                weight = rightColWeight,
                isExpanded = expandedCell == GridCell.TOP_RIGHT,
                apps = state.appsInCells[GridCell.TOP_RIGHT] ?: emptyList(),
                gridCellImage = state.gridCellImages[GridCell.TOP_RIGHT],
                pinnedPackages = state.pinnedPackages,
                onIntent = onIntent,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .weight(bottomRowWeight)
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            GridCellContent(
                cell = GridCell.BOTTOM_LEFT,
                weight = leftColWeight,
                isExpanded = expandedCell == GridCell.BOTTOM_LEFT,
                apps = state.appsInCells[GridCell.BOTTOM_LEFT] ?: emptyList(),
                gridCellImage = state.gridCellImages[GridCell.BOTTOM_LEFT],
                pinnedPackages = state.pinnedPackages,
                onIntent = onIntent,
                modifier = Modifier.padding(end = 4.dp),
            )
            GridCellContent(
                cell = GridCell.BOTTOM_RIGHT,
                weight = rightColWeight,
                isExpanded = expandedCell == GridCell.BOTTOM_RIGHT,
                apps = state.appsInCells[GridCell.BOTTOM_RIGHT] ?: emptyList(),
                gridCellImage = state.gridCellImages[GridCell.BOTTOM_RIGHT],
                pinnedPackages = state.pinnedPackages,
                onIntent = onIntent,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.GridCellContent(
    cell: GridCell,
    weight: Float,
    isExpanded: Boolean,
    apps: List<AppEntity>,
    gridCellImage: GridCellImage?,
    pinnedPackages: Set<String>,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = ((weight - 0.6f) / 0.2f).coerceIn(0f, 1f)
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)
    val dragDropState = LocalDragDropState.current

    // 셀 영역을 DragDropState에 등록
    DisposableEffect(cell) {
        onDispose { dragDropState.unregisterCellBounds(cell) }
    }

    // 호버 글로우 애니메이션
    val isHovered = dragDropState.hoveredCell == cell
    val glowAlpha by animateFloatAsState(
        targetValue = if (isHovered) 1f else 0f,
        animationSpec = tween(200),
        label = "glowAlpha",
    )

    // 호버 셀 변경 시 햅틱 피드백 (자력 느낌)
    LaunchedEffect(dragDropState.hoveredCell) {
        if (dragDropState.hoveredCell == cell) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // 확장 상태: accentPrimary 2dp / 축소: gridBorder 1dp
    val borderColor = if (isExpanded) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isExpanded) 2.dp else 1.dp
    val glowColor = MaterialTheme.colorScheme.primary

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onIntent(HomeIntent.ClickGrid(cell))
        },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .weight(weight)
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                val size = coords.size
                dragDropState.registerCellBounds(
                    cell,
                    Rect(
                        left = pos.x,
                        top = pos.y,
                        right = pos.x + size.width,
                        bottom = pos.y + size.height,
                    ),
                )
            }
            .drawBehind {
                if (glowAlpha > 0f) {
                    val radius = 12.dp.toPx()
                    val cornerRadius = CornerRadius(radius)
                    // 외곽: 12dp 폭, alpha 0.15 (은은한 확산)
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.15f * glowAlpha),
                        cornerRadius = cornerRadius,
                        style = Stroke(width = 12.dp.toPx()),
                    )
                    // 중간: 6dp 폭, alpha 0.3
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.3f * glowAlpha),
                        cornerRadius = cornerRadius,
                        style = Stroke(width = 6.dp.toPx()),
                    )
                    // 코어: 2dp 폭, alpha 0.8 (선명한 테두리)
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.8f * glowAlpha),
                        cornerRadius = cornerRadius,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
            .border(width = borderWidth, color = borderColor, shape = shape),
    ) {
        if (isExpanded) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 확장 이미지 배경
                val expandedUri = gridCellImage?.expandedImageUri
                if (expandedUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(expandedUri)
                            .crossfade(300)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // 앱 목록 가독성을 위한 반투명 오버레이
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(contentAlpha)
                        .padding(8.dp),
                ) {
                    items(apps) { app ->
                        val isPinned = app.packageName in pinnedPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onIntent(HomeIntent.ClickApp(app))
                                    },
                                    onLongClick = if (isPinned) {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onIntent(HomeIntent.UnassignApp(app))
                                        }
                                    } else null,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isPinned) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "고정됨",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                // 축소 이미지 배경
                val idleUri = gridCellImage?.idleImageUri
                if (idleUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(idleUri)
                            .crossfade(300)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    text = cell.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (idleUri != null) Color.White else Color.Unspecified,
                )
            }
        }
    }
}
