package org.comon.streamlauncher.widget.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

/**
 * 편집 모드(isDeleteModeActive=true): dispatchTouchEvent에서 자식 터치 전부 차단.
 * 일반 모드: GestureDetector로 롱프레스 감지 → isDeleteModeActive를 동기적으로 true 세팅
 *   → 롱프레스를 유발한 제스처의 ACTION_UP이 자식에 도달하기 전에 차단되므로
 *     위젯 내부 버튼이 눌리지 않음.
 */
private class WidgetContainerView(context: Context) : FrameLayout(context) {

    var isDeleteModeActive: Boolean = false
    var onLongPressListener: (() -> Unit)? = null

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                isDeleteModeActive = true      // 동기적으로 즉시 차단
                onLongPressListener?.invoke()  // 편집 모드 진입 트리거
            }
        },
    )

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isDeleteModeActive) return true
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }
}

@Composable
fun WidgetScreen(
    widgetSlots: List<Int?>,
    appWidgetHost: AppWidgetHost,
    onAddWidgetClick: (slotIndex: Int) -> Unit,
    onDeleteWidgetClick: (slotIndex: Int) -> Unit,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = StreamLauncherTheme.colors.glassOnSurface

    // 편집 모드일 때 뒤로가기 → 편집 모드 해제 (홈으로 가지 않음)
    // 편집 모드가 아닐 때는 비활성화 → 기본 뒤로가기 동작(홈 이동)
    BackHandler(enabled = isEditMode) {
        onEditModeChange(false)
    }

    Column(modifier = modifier.fillMaxSize()) {

        // 편집 모드 타이틀바 — 위에서 슬라이드 인/아웃
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "위젯 편집",
                    color = onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
                TextButton(
                    onClick = { onEditModeChange(false) },
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Text("완료", color = onSurface)
                }
            }
        }

        val allEmpty = widgetSlots.all { it == null }

        if (allEmpty && !isEditMode) {
            // 위젯이 하나도 없을 때 안내 메시지
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onEditModeChange(true) })
                    },
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
                        text = "화면을 길게 눌러 위젯을 추가하세요",
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
        } else {
            // 위젯 그리드
            Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        for (col in 0 until 2) {
                            val index = row * 2 + col
                            val widgetId = widgetSlots[index]
                            if (widgetId != null) {
                                key(widgetId) {
                                    WidgetCell(
                                        widgetId = widgetId,
                                        appWidgetHost = appWidgetHost,
                                        isEditMode = isEditMode,
                                        onEnterEditMode = { onEditModeChange(true) },
                                        onDeleteClick = { onDeleteWidgetClick(index) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                            } else {
                                EmptyCell(
                                    isEditMode = isEditMode,
                                    onAddClick = { onAddWidgetClick(index) },
                                    onEnterEditMode = { onEditModeChange(true) },
                                    tintColor = onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetCell(
    widgetId: Int,
    appWidgetHost: AppWidgetHost,
    isEditMode: Boolean,
    onEnterEditMode: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(4.dp)) {
        AndroidView<WidgetContainerView>(
            factory = { ctx ->
                WidgetContainerView(ctx).apply {
                    val appWidgetManager = AppWidgetManager.getInstance(ctx)
                    val info: AppWidgetProviderInfo? = appWidgetManager.getAppWidgetInfo(widgetId)
                    val hostView = appWidgetHost.createView(ctx, widgetId, info).also { view ->
                        view.setAppWidget(widgetId, info)
                        view.layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                    }
                    addView(hostView)
                }
            },
            update = { container ->
                container.isDeleteModeActive = isEditMode
                // 일반 모드에서만 롱프레스로 편집 모드 진입.
                // 편집 모드에서는 isDeleteModeActive=true이므로 GestureDetector가
                // 호출되지 않아 null 설정은 방어적 처리.
                container.onLongPressListener = if (!isEditMode) onEnterEditMode else null
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 편집 모드에서만 삭제 버튼 표시 (롱프레스 없이 항상 보임)
        AnimatedVisibility(
            visible = isEditMode,
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
                    contentDescription = "위젯 삭제",
                )
            }
        }
    }
}

@Composable
private fun EmptyCell(
    isEditMode: Boolean,
    onAddClick: () -> Unit,
    onEnterEditMode: () -> Unit,
    tintColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(4.dp)
            // isEditMode를 key로 사용 → 모드 전환 시 코루틴 재시작
            .pointerInput(isEditMode) {
                // 일반 모드에서만 롱프레스로 편집 모드 진입
                if (!isEditMode) {
                    detectTapGestures(onLongPress = { onEnterEditMode() })
                }
                // 편집 모드: 블록이 즉시 반환 → pointerInput이 이벤트 가로채지 않음
                // → "+" 버튼 자체의 클릭이 정상 동작
            },
    ) {
        // 편집 모드에서만 "+" 버튼 표시
        AnimatedVisibility(visible = isEditMode) {
            FilledTonalIconButton(
                onClick = onAddClick,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = tintColor.copy(alpha = 0.12f),
                    contentColor = tintColor,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "위젯 추가",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
