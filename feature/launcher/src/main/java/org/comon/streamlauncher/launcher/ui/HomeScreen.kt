package org.comon.streamlauncher.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import org.comon.streamlauncher.launcher.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.launcher.HomeIntent
import org.comon.streamlauncher.launcher.HomeState
import org.comon.streamlauncher.ui.component.AppIcon
import org.comon.streamlauncher.ui.component.calculateAdaptiveHomeGridMetrics
import org.comon.streamlauncher.ui.dragdrop.LocalDragDropState

@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragDropState = LocalDragDropState.current
    // 드래그 중에는 hoveredCell(대상 셀)을 확장하여 시각적 피드백 제공.
    // 소스 셀이 수축해도 GridCellContent 내부에서 GridAppItem을 트리에 유지하여
    // pointerInput coroutine이 dispose되지 않도록 별도 처리함.
    val expandedCell = when {
        dragDropState.isDragging && dragDropState.dragSourceCell != null -> dragDropState.dragSourceCell
        dragDropState.isDragging -> dragDropState.hoveredCell
        else -> state.expandedCell
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
                iconSizeRatio = state.appDrawerIconSizeRatio,
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
                iconSizeRatio = state.appDrawerIconSizeRatio,
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
                iconSizeRatio = state.appDrawerIconSizeRatio,
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
                iconSizeRatio = state.appDrawerIconSizeRatio,
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
    iconSizeRatio: Float,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = ((weight - 0.6f) / 0.2f).coerceIn(0f, 1f)
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val shape = RoundedCornerShape(12.dp)
    val dragDropState = LocalDragDropState.current
    var cellWidthPx by remember { mutableIntStateOf(0) }
    var cellHeightPx by remember { mutableIntStateOf(0) }

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
                cellWidthPx = size.width
                cellHeightPx = size.height
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
        // 드래그 소스 셀은 isExpanded=false여도 GridAppItem을 트리에 유지해야
        // pointerInput coroutine이 dispose되지 않아 드래그가 계속된다.
        // contentAlpha가 0이 되어 시각적으로는 보이지 않지만 레이아웃은 살아있음.
        val isDragSource = dragDropState.isDragging && dragDropState.dragSourceCell == cell
        if (isExpanded || isDragSource) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 확장 이미지 배경
                val expandedUri = gridCellImage?.expandedImageUri
                if (expandedUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(appContext)
                            .data(expandedUri)
                            .apply {
                                if (cellWidthPx > 0 && cellHeightPx > 0) {
                                    size(cellWidthPx, cellHeightPx)
                                }
                            }
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
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(contentAlpha)
                        .padding(5.dp),
                ) {
                    val gridMetrics = calculateAdaptiveHomeGridMetrics(
                        maxWidth = maxWidth,
                        maxHeight = maxHeight,
                        iconSizeRatio = iconSizeRatio,
                    )
                    val displayApps = apps.take(gridMetrics.capacity)
                    val labelFontSize = if (gridMetrics.itemWidth < 72.dp) 10.sp else 11.sp
                    val hiddenDraggedApp = if (
                        isDragSource &&
                        dragDropState.dragSourceIndex >= 0 &&
                        dragDropState.draggedApp != null &&
                        dragDropState.dragSourceIndex >= displayApps.size
                    ) {
                        dragDropState.draggedApp
                    } else {
                        null
                    }

                    LaunchedEffect(cell, gridMetrics.capacity) {
                        onIntent(HomeIntent.UpdateCellCapacity(cell, gridMetrics.capacity))
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        for (row in 0 until gridMetrics.rows) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            ) {
                                for (col in 0 until gridMetrics.columns) {
                                    val index = row * gridMetrics.columns + col
                                    val app = displayApps.getOrNull(index)
                                    val isDragged = dragDropState.draggedApp == app && dragDropState.isDragging
                                    val alphaVal = if (isDragged) 0.5f else 1f

                                    GridSlot(
                                        cell = cell,
                                        slotIndex = index,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(5.dp)
                                            .alpha(alphaVal),
                                    ) {
                                        if (app != null) {
                                            GridAppItem(
                                                app = app,
                                                cell = cell,
                                                appIndex = index,
                                                iconSize = gridMetrics.iconSize,
                                                labelFontSize = labelFontSize,
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

                    // 소스 셀이 축소되며 dynamic capacity 밖으로 밀린 앱도 드래그 중에는
                    // 트리에 남겨 pointerInput coroutine이 취소되지 않게 유지한다.
                    if (hiddenDraggedApp != null) {
                        GridAppItem(
                            app = hiddenDraggedApp,
                            cell = cell,
                            appIndex = dragDropState.dragSourceIndex,
                            iconSize = gridMetrics.iconSize,
                            labelFontSize = labelFontSize,
                            isPinned = hiddenDraggedApp.packageName in pinnedPackages,
                            isEditing = true,
                            onIntent = onIntent,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(1.dp)
                                .alpha(0f),
                        )
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
                        model = ImageRequest.Builder(appContext)
                            .data(idleUri)
                            .apply {
                                if (cellWidthPx > 0 && cellHeightPx > 0) {
                                    size(cellWidthPx, cellHeightPx)
                                }
                            }
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

@Composable
private fun GridSlot(
    cell: GridCell,
    slotIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dragDropState = LocalDragDropState.current

    DisposableEffect(cell, slotIndex) {
        onDispose { dragDropState.unregisterSlotBounds(cell, slotIndex) }
    }

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            val pos = coords.positionInRoot()
            val size = coords.size
            dragDropState.registerSlotBounds(
                cell = cell,
                index = slotIndex,
                rect = Rect(pos.x, pos.y, pos.x + size.width, pos.y + size.height),
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridAppItem(
    app: AppEntity,
    cell: GridCell,
    appIndex: Int,
    iconSize: Dp,
    labelFontSize: androidx.compose.ui.unit.TextUnit,
    isPinned: Boolean,
    isEditing: Boolean,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val dragDropState = LocalDragDropState.current
    var itemRootOffset by remember { mutableStateOf(Offset.Zero) }
    // 편집 모드 탭 소비용 (indication 없이 이벤트만 막음)
    val noOpInteractionSource = remember { MutableInteractionSource() }

    // 흔들림 애니메이션 효과 (편집 모드 시, 드래그 중에는 정지)
    val isDragged = dragDropState.isDragging && dragDropState.draggedApp == app
    val editRotation = if (isEditing && !isDragged) {
        val wiggleTransition = rememberInfiniteTransition(label = "wiggleTransition")
        val animatedRotation by wiggleTransition.animateFloat(
            initialValue = -2f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(100),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "wiggleRotation",
        )
        animatedRotation
    } else {
        0f
    }
    val rotation by animateFloatAsState(
        targetValue = if (isDragged) 0f else editRotation,
        animationSpec = if (isEditing && !isDragged) tween(100) else spring(),
        label = "wiggle",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
                .onGloballyPositioned { coords ->
                    itemRootOffset = coords.positionInRoot()
                }
                // 편집 모드가 아닐 때: 탭(앱 실행) + 길게 누름(편집 모드 진입)
                .then(
                    if (!isEditing) {
                        Modifier.combinedClickable(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onIntent(HomeIntent.ClickApp(app))
                            },
                            onLongClick = {
                                if (isPinned && !dragDropState.isDragging) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onIntent(HomeIntent.SetEditingCell(cell))
                                }
                            },
                        )
                    } else {
                        // 편집 모드에서 다시 길게 누르면 드래그 시작.
                        // pointerInput이 롱프레스+드래그를 먼저 처리하고,
                        // 단순 탭은 clickable이 소비하여 부모 Surface로 전파를 차단.
                        Modifier
                            .pointerInput(app, cell, appIndex) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val rootPos = itemRootOffset + localOffset
                                        dragDropState.startDrag(app, rootPos, cell, appIndex)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        dragDropState.updateDrag(itemRootOffset + change.position)
                                    },
                                    onDragEnd = {
                                        val result = dragDropState.endDrag()
                                        if (result != null) {
                                            val src = result.sourceCell
                                            when {
                                                // 같은 셀 내 슬롯 변경
                                                src != null && src == result.targetCell -> {
                                                    val toSlot = result.targetSlotIndex
                                                    if (toSlot >= 0 && toSlot != result.sourceIndex) {
                                                        onIntent(
                                                            HomeIntent.MoveAppInCell(
                                                                src,
                                                                result.sourceIndex,
                                                                toSlot,
                                                            ),
                                                        )
                                                    }
                                                }
                                                // 다른 셀로 이동
                                                src != null -> {
                                                    onIntent(
                                                        HomeIntent.MoveAppBetweenCells(
                                                            result.app,
                                                            src,
                                                            result.targetCell,
                                                            result.targetSlotIndex,
                                                        ),
                                                    )
                                                }
                                                else -> Unit
                                            }
                                        }
                                        onIntent(HomeIntent.SetEditingCell(null))
                                    },
                                    onDragCancel = {
                                        dragDropState.cancelDrag()
                                        onIntent(HomeIntent.SetEditingCell(null))
                                    },
                                )
                            }
                            // 탭 이벤트를 소비하여 부모 Surface.onClick 전파 차단
                            .clickable(
                                indication = null,
                                interactionSource = noOpInteractionSource,
                            ) { /* 탭 소비만, 동작 없음 */ }
                    },
                ),
        ) {
            AppIcon(
                packageName = app.packageName,
                modifier = Modifier.size(iconSize),
            )
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = labelFontSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.9f),
            )
        }

        // 편집 모드이고 드래그 중이 아닐 때만 X 버튼 표시
        if (isEditing && !dragDropState.isDragging) {
            IconButton(
                onClick = { onIntent(HomeIntent.UnassignApp(app)) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(24.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        CircleShape,
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onErrorContainer,
                        CircleShape,
                    )
                    .zIndex(2f),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.home_remove_app),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}