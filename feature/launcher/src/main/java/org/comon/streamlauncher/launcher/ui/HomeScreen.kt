package org.comon.streamlauncher.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import org.comon.streamlauncher.launcher.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.launcher.HomeIntent
import org.comon.streamlauncher.launcher.HomeState
import org.comon.streamlauncher.ui.component.AppIcon
import org.comon.streamlauncher.ui.dragdrop.LocalDragDropState

private const val MAX_APPS_PER_CELL = 6
private const val GRID_COLUMNS = 2
private const val GRID_ROWS = 3

@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragDropState = LocalDragDropState.current
    // 드래그 중일 때는 hoveredCell을 expandedCell로 임시 사용
    val expandedCell = if (dragDropState.isDragging) {
        dragDropState.hoveredCell
    } else {
        state.expandedCell
    }
    val editingCell = state.editingCell

    BackHandler(enabled = editingCell != null) {
        onIntent(HomeIntent.SetEditingCell(null))
    }

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
                editingCell = editingCell,
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
                editingCell = editingCell,
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
                editingCell = editingCell,
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
                editingCell = editingCell,
                onIntent = onIntent,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun RowScope.GridCellContent(
    cell: GridCell,
    weight: Float,
    isExpanded: Boolean,
    apps: List<AppEntity>,
    gridCellImage: GridCellImage?,
    pinnedPackages: Set<String>,
    editingCell: GridCell?,
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
            if (editingCell != null) {
                onIntent(HomeIntent.SetEditingCell(null))
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onIntent(HomeIntent.ClickGrid(cell))
            }
        },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
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
                val displayApps = apps.take(MAX_APPS_PER_CELL)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(contentAlpha)
                        .padding(5.dp),
                ) {
                    for (row in 0 until GRID_ROWS) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            for (col in 0 until GRID_COLUMNS) {
                                val index = row * GRID_COLUMNS + col
                                val app = displayApps.getOrNull(index)
                                
                                // 드래그 앤 드롭 로컬 상태
                                val isDragged = dragDropState.draggedApp == app && dragDropState.isDragging
                                val alphaVal = if (isDragged) 0.5f else 1f

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(5.dp)
                                        .alpha(alphaVal),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (app != null) {
                                        GridAppItem(
                                            app = app,
                                            cell = cell,
                                            isPinned = app.packageName in pinnedPackages,
                                            isEditing = editingCell == cell,
                                            onIntent = onIntent,
                                        )
                                    }
                                }
                            }
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
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridAppItem(
    app: AppEntity,
    cell: GridCell,
    isPinned: Boolean,
    isEditing: Boolean,
    onIntent: (HomeIntent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val dragDropState = LocalDragDropState.current
    
    // 흔들림 애니메이션 효과 (편집 모드 시)
    var isWiggling by remember { mutableStateOf(false)}
    val rotation by animateFloatAsState(
        targetValue = if (isWiggling) 2f else if (isEditing) -2f else 0f,
        animationSpec = if (isEditing) tween(100) else spring(),
        label = "wiggle"
    )

    LaunchedEffect(isEditing) {
        if (isEditing) {
            while (true) {
                isWiggling = true
                delay(100)
                isWiggling = false
                delay(100)
            }
        } else {
            isWiggling = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                }
                .combinedClickable(
                    onClick = {
                        if (isEditing) return@combinedClickable
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onIntent(HomeIntent.ClickApp(app))
                    },
                    onLongClick = {
                        if (isPinned && dragDropState.draggedApp == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!isEditing) {
                                onIntent(HomeIntent.SetEditingCell(cell))
                            }
                        } else null
                    },
                )
                .draggable(
                    state = rememberDraggableState { delta ->
                        // 간단한 드래그 구현 (수평/수직 제스처 등 고도화 필요하지만 예시로)
                        // DragDropState를 엮을 수도 있음
                    },
                    orientation = Orientation.Vertical,
                    onDragStarted = {
                        if (isEditing) {
                            dragDropState.startDrag(app, Offset.Zero)
                        }
                    },
                    onDragStopped = {
                        if (isEditing && dragDropState.isDragging) {
                            dragDropState.endDrag()
                        }
                    }
                ),
        ) {
            AppIcon(
                packageName = app.packageName,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
            )
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isEditing) {
            IconButton(
                onClick = { onIntent(HomeIntent.UnassignApp(app)) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(24.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        CircleShape
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onErrorContainer,
                        CircleShape
                    )
                    .zIndex(2f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.home_remove_app),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}