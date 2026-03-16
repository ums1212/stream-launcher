package org.comon.streamlauncher.widget.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import org.comon.streamlauncher.domain.model.WidgetGrid
import org.comon.streamlauncher.domain.model.WidgetPlacement
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import org.comon.streamlauncher.widget.R
import org.comon.streamlauncher.widget.WidgetIntent
import org.comon.streamlauncher.widget.WidgetState

/**
 * 편집 모드(isDeleteModeActive=true): dispatchTouchEvent에서 자식 터치 전부 차단.
 * 일반 모드: GestureDetector로 롱프레스 감지 → 편집 모드 진입
 */
private class WidgetContainerView(context: Context) : FrameLayout(context) {

    var isDeleteModeActive: Boolean = false
    var onLongPressListener: (() -> Unit)? = null
    private var hostedView: AppWidgetHostView? = null

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                isDeleteModeActive = true
                onLongPressListener?.invoke()
            }
        },
    )

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isDeleteModeActive) return true
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onDetachedFromWindow() {
        releaseHostedView()
        super.onDetachedFromWindow()
    }

    fun attachHostedView(hostView: AppWidgetHostView) {
        if (hostedView === hostView && hostView.parent === this) return

        releaseHostedView()
        hostedView = hostView
        (hostView.parent as? ViewGroup)?.removeView(hostView)
        addView(hostView)
    }

    fun releaseHostedView() {
        onLongPressListener = null
        hostedView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        hostedView = null
        removeAllViews()
    }
}

@Composable
fun WidgetScreen(
    state: WidgetState,
    appWidgetHost: AppWidgetHost,
    onAddWidgetClick: () -> Unit,
    onDeleteWidgetClick: (appWidgetId: Int) -> Unit,
    onIntent: (WidgetIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = StreamLauncherTheme.colors.glassOnSurface
    val density = LocalDensity.current

    BackHandler(enabled = state.isEditMode) {
        onIntent(WidgetIntent.SetEditMode(false))
    }

    // 그리드 영역 크기 (px)
    var gridAreaWidthPx by remember { mutableFloatStateOf(0f) }
    var gridAreaHeightPx by remember { mutableFloatStateOf(0f) }

    val gridColumns = if (gridAreaWidthPx <= 0f) 5
        else WidgetGrid.computeColumns(with(density) { gridAreaWidthPx.toDp().value.toInt() })
    val gridRows = if (gridAreaHeightPx <= 0f) 10
        else WidgetGrid.computeRows(with(density) { gridAreaHeightPx.toDp().value.toInt() })

    val cellWidthPx = if (gridColumns > 0 && gridAreaWidthPx > 0f) gridAreaWidthPx / gridColumns else 0f
    val cellHeightPx = if (gridRows > 0 && gridAreaHeightPx > 0f) gridAreaHeightPx / gridRows else 0f

    Column(modifier = modifier.fillMaxSize()) {

        // 편집 모드 타이틀바
        AnimatedVisibility(
            visible = state.isEditMode,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.widget_edit_title),
                    color = onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                    FilledTonalIconButton(
                        onClick = onAddWidgetClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = onSurface.copy(alpha = 0.12f),
                            contentColor = onSurface,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.widget_add),
                        )
                    }
                    TextButton(onClick = { onIntent(WidgetIntent.SetEditMode(false)) }) {
                        Text(stringResource(R.string.widget_edit_done), color = onSurface)
                    }
                }
            }
        }

        // 그리드 영역
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    gridAreaWidthPx = coords.size.width.toFloat()
                    gridAreaHeightPx = coords.size.height.toFloat()
                }
                .pointerInput(state.isEditMode) {
                    if (!state.isEditMode) {
                        detectTapGestures(onLongPress = { onIntent(WidgetIntent.SetEditMode(true)) })
                    }
                },
        ) {

            // 그리드 가이드 라인 (편집 모드)
            if (state.isEditMode && cellWidthPx > 1f && cellHeightPx > 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            val lineColor = Color.White.copy(alpha = 0.18f)
                            val strokeWidth = 1f
                            for (col in 1 until gridColumns) {
                                val x = col * cellWidthPx
                                drawLine(
                                    color = lineColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = strokeWidth,
                                    pathEffect = dashEffect,
                                )
                            }
                            for (row in 1 until gridRows) {
                                val y = row * cellHeightPx
                                drawLine(
                                    color = lineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = strokeWidth,
                                    pathEffect = dashEffect,
                                )
                            }
                        },
                )
            }

            // 드래그 프리뷰
            if (state.draggingWidgetId != null && cellWidthPx > 1f) {
                val dragged = state.placements.find { it.appWidgetId == state.draggingWidgetId }
                if (dragged != null) {
                    val previewW = with(density) { (dragged.columnSpan * cellWidthPx).toDp() }
                    val previewH = with(density) { (dragged.rowSpan * cellHeightPx).toDp() }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (state.dragPreviewCol * cellWidthPx).roundToInt(),
                                    (state.dragPreviewRow * cellHeightPx).roundToInt(),
                                )
                            }
                            .size(previewW, previewH)
                            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                    )
                }
            }

            // 리사이즈 프리뷰
            if (state.resizingWidgetId != null && cellWidthPx > 1f) {
                val resizing = state.placements.find { it.appWidgetId == state.resizingWidgetId }
                if (resizing != null) {
                    val previewW = with(density) { (state.resizePreviewColSpan * cellWidthPx).toDp() }
                    val previewH = with(density) { (state.resizePreviewRowSpan * cellHeightPx).toDp() }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (resizing.column * cellWidthPx).roundToInt(),
                                    (resizing.row * cellHeightPx).roundToInt(),
                                )
                            }
                            .size(previewW, previewH)
                            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                    )
                }
            }

            // 위젯 렌더링
            if (cellWidthPx > 1f) {
                state.placements.forEach { placement ->
                    val displayCol = if (placement.appWidgetId == state.draggingWidgetId)
                        state.dragPreviewCol else placement.column
                    val displayRow = if (placement.appWidgetId == state.draggingWidgetId)
                        state.dragPreviewRow else placement.row
                    val displayColSpan = if (placement.appWidgetId == state.resizingWidgetId)
                        state.resizePreviewColSpan else placement.columnSpan
                    val displayRowSpan = if (placement.appWidgetId == state.resizingWidgetId)
                        state.resizePreviewRowSpan else placement.rowSpan

                    key(placement.appWidgetId) {
                        WidgetItem(
                            placement = placement,
                            displayCol = displayCol,
                            displayRow = displayRow,
                            displayColSpan = displayColSpan,
                            displayRowSpan = displayRowSpan,
                            cellWidthPx = cellWidthPx,
                            cellHeightPx = cellHeightPx,
                            gridCols = gridColumns,
                            gridRows = gridRows,
                            appWidgetHost = appWidgetHost,
                            isEditMode = state.isEditMode,
                            onDeleteClick = { onDeleteWidgetClick(placement.appWidgetId) },
                            onEnterEditMode = { onIntent(WidgetIntent.SetEditMode(true)) },
                            onIntent = onIntent,
                        )
                    }
                }
            }

            // 빈 상태 안내
            if (state.placements.isEmpty() && !state.isEditMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(48.dp)
                                .drawBehind {
                                    drawIntoCanvas { canvas ->
                                        val paint = Paint()
                                        paint.asFrameworkPaint().apply {
                                            isAntiAlias = true
                                            color = Color.Black.copy(alpha = 0.1f).toArgb()
                                            maskFilter = android.graphics.BlurMaskFilter(
                                                12f,
                                                android.graphics.BlurMaskFilter.Blur.NORMAL,
                                            )
                                        }
                                        canvas.drawCircle(
                                            center = Offset(size.width / 2f, size.height / 2f + 1f),
                                            radius = size.minDimension / 2f,
                                            paint = paint,
                                        )
                                    }
                                },
                        )
                        Text(
                            text = stringResource(R.string.widget_empty_hint),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.75f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 12f,
                                ),
                            ),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetItem(
    placement: WidgetPlacement,
    displayCol: Int,
    displayRow: Int,
    displayColSpan: Int,
    displayRowSpan: Int,
    cellWidthPx: Float,
    cellHeightPx: Float,
    gridCols: Int,
    gridRows: Int,
    appWidgetHost: AppWidgetHost,
    isEditMode: Boolean,
    onDeleteClick: () -> Unit,
    onEnterEditMode: () -> Unit,
    onIntent: (WidgetIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val cellWidthDp = with(density) { cellWidthPx.toDp() }
    val cellHeightDp = with(density) { cellHeightPx.toDp() }

    // 드래그 누적 오프셋 (px)
    var dragAccumX by remember { mutableFloatStateOf(0f) }
    var dragAccumY by remember { mutableFloatStateOf(0f) }

    // 리사이즈 누적 오프셋 (px)
    var resizeAccumX by remember { mutableFloatStateOf(0f) }
    var resizeAccumY by remember { mutableFloatStateOf(0f) }

    // pointerInput 재생성 없이 최신 값 참조
    val currentCellWidthPx by rememberUpdatedState(cellWidthPx)
    val currentCellHeightPx by rememberUpdatedState(cellHeightPx)
    val currentGridCols by rememberUpdatedState(gridCols)
    val currentGridRows by rememberUpdatedState(gridRows)
    val currentPlacementCol by rememberUpdatedState(placement.column)
    val currentPlacementRow by rememberUpdatedState(placement.row)
    val currentColSpan by rememberUpdatedState(placement.columnSpan)
    val currentRowSpan by rememberUpdatedState(placement.rowSpan)
    val currentMinColSpan by rememberUpdatedState(placement.minColumnSpan)
    val currentMinRowSpan by rememberUpdatedState(placement.minRowSpan)

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (displayCol * cellWidthPx).roundToInt(),
                    (displayRow * cellHeightPx).roundToInt(),
                )
            }
            .size(cellWidthDp * displayColSpan, cellHeightDp * displayRowSpan),
    ) {
        AndroidView<WidgetContainerView>(
            factory = { ctx ->
                WidgetContainerView(ctx).apply {
                    val mgr = AppWidgetManager.getInstance(ctx)
                    val info: AppWidgetProviderInfo? = mgr.getAppWidgetInfo(placement.appWidgetId)
                    val hostView = appWidgetHost.createView(ctx, placement.appWidgetId, info).also { v ->
                        v.setAppWidget(placement.appWidgetId, info)
                        v.layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                    }
                    attachHostedView(hostView)
                }
            },
            update = { container ->
                container.isDeleteModeActive = isEditMode
                container.onLongPressListener = if (!isEditMode) onEnterEditMode else null
            },
            onRelease = { container ->
                container.releaseHostedView()
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 편집 모드 오버레이
        if (isEditMode) {
            // 드래그 핸들 (위젯 전체 영역, 삭제/리사이즈 핸들보다 낮은 z-order)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isEditMode) {
                        detectDragGestures(
                            onDragStart = {
                                dragAccumX = 0f
                                dragAccumY = 0f
                                onIntent(WidgetIntent.StartDrag(placement.appWidgetId))
                            },
                            onDrag = { change, delta ->
                                change.consume()
                                dragAccumX += delta.x
                                dragAccumY += delta.y
                                val newCol = (currentPlacementCol + dragAccumX / currentCellWidthPx)
                                    .roundToInt()
                                    .coerceIn(0, (currentGridCols - currentColSpan).coerceAtLeast(0))
                                val newRow = (currentPlacementRow + dragAccumY / currentCellHeightPx)
                                    .roundToInt()
                                    .coerceIn(0, (currentGridRows - currentRowSpan).coerceAtLeast(0))
                                onIntent(WidgetIntent.UpdateDrag(newCol, newRow))
                            },
                            onDragEnd = {
                                dragAccumX = 0f
                                dragAccumY = 0f
                                onIntent(WidgetIntent.EndDrag)
                            },
                            onDragCancel = {
                                dragAccumX = 0f
                                dragAccumY = 0f
                                onIntent(WidgetIntent.EndDrag)
                            },
                        )
                    },
            )

            // 삭제 버튼 (우상단)
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                FilledTonalIconButton(
                    onClick = onDeleteClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.Red.copy(alpha = 0.85f),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.widget_delete),
                    )
                }
            }

            // 리사이즈 핸들 (우하단) — 3개 대각선 줄로 시각 표시
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomEnd)
                    .background(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(topStart = 6.dp),
                    )
                    .drawBehind {
                        val strokeWidth = 1.5.dp.toPx()
                        val color = Color.White.copy(alpha = 0.85f)
                        for (i in 1..3) {
                            val offset = i * size.width / 4f
                            drawLine(color, Offset(size.width, offset), Offset(offset, size.height), strokeWidth)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                resizeAccumX = 0f
                                resizeAccumY = 0f
                                onIntent(WidgetIntent.StartResize(placement.appWidgetId))
                            },
                            onDrag = { change, delta ->
                                change.consume()
                                resizeAccumX += delta.x
                                resizeAccumY += delta.y
                                val newColSpan = (currentColSpan + resizeAccumX / currentCellWidthPx)
                                    .roundToInt()
                                    .coerceIn(
                                        currentMinColSpan,
                                        (currentGridCols - currentPlacementCol).coerceAtLeast(currentMinColSpan),
                                    )
                                val newRowSpan = (currentRowSpan + resizeAccumY / currentCellHeightPx)
                                    .roundToInt()
                                    .coerceIn(
                                        currentMinRowSpan,
                                        (currentGridRows - currentPlacementRow).coerceAtLeast(currentMinRowSpan),
                                    )
                                onIntent(WidgetIntent.UpdateResize(newColSpan, newRowSpan))
                            },
                            onDragEnd = {
                                resizeAccumX = 0f
                                resizeAccumY = 0f
                                onIntent(WidgetIntent.EndResize)
                            },
                            onDragCancel = {
                                resizeAccumX = 0f
                                resizeAccumY = 0f
                                onIntent(WidgetIntent.EndResize)
                            },
                        )
                    },
            )
        }
    }
}
