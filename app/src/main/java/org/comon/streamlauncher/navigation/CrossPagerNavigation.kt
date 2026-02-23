package org.comon.streamlauncher.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.comon.streamlauncher.apps_drawer.ui.AppIcon
import org.comon.streamlauncher.ui.dragdrop.LocalDragDropState
import org.comon.streamlauncher.ui.modifier.glassEffect
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * 십자형(상/하/좌/우) 스와이프 내비게이션 컨테이너.
 *
 * 구조:
 *   VerticalPager(3페이지, 초기=1)
 *   ├─ [0] UpPage     — Notifications & Settings
 *   ├─ [1] CenterRow  — HorizontalPager(3페이지, 초기=1)
 *   │       ├─ [0] LeftPage  — Feed & Announcements (feedContent)
 *   │       ├─ [1] homeContent()
 *   │       └─ [2] RightPage — Widget Area
 *   └─ [2] DownPage   — App Drawer (appDrawerContent()) — 글래스 배경
 *
 * 뒤로가기: 중앙이 아닐 때 animateScrollToPage(1)로 홈 복귀
 * 제스처 간섭 방지: 한 Pager 스크롤 중 다른 Pager userScrollEnabled = false
 * Alpha 효과: 중앙에 가까울수록 1.0f, 멀수록 0.5f
 * 키보드 정리: DownPage(2)에서 벗어나면 소프트 키보드 자동 숨김
 * 드래그 중: userScrollEnabled = false, 드래그 오버레이 표시
 */
@Composable
fun CrossPagerNavigation(
    modifier: Modifier = Modifier,
    resetTrigger: Int = 0,
    wallpaperImage: String? = null,
    appDrawerContent: @Composable () -> Unit,
    widgetContent: @Composable () -> Unit = {},
    settingsContent: @Composable () -> Unit = {},
    feedContent: @Composable () -> Unit = {},
    homeContent: @Composable () -> Unit,
) {
    val verticalPagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val horizontalPagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val dragDropState = LocalDragDropState.current

    val isAtCenter = verticalPagerState.currentPage == 1 && horizontalPagerState.currentPage == 1

    // 드래그 시작 시 홈 화면으로 스크롤 콜백 등록
    LaunchedEffect(Unit) {
        dragDropState.onScrollToHome = {
            scope.launch {
                verticalPagerState.animateScrollToPage(1, animationSpec = tween(300))
                horizontalPagerState.animateScrollToPage(1, animationSpec = tween(300))
            }
        }
    }

    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0) {
            verticalPagerState.animateScrollToPage(1, animationSpec = tween(300))
            horizontalPagerState.animateScrollToPage(1, animationSpec = tween(300))
        }
    }

    // DownPage(2)에서 벗어날 때 키보드 숨기기
    LaunchedEffect(verticalPagerState.currentPage) {
        if (verticalPagerState.currentPage != 2) {
            keyboardController?.hide()
        }
    }

    BackHandler(enabled = !isAtCenter) {
        scope.launch {
            if (verticalPagerState.currentPage != 1) {
                verticalPagerState.animateScrollToPage(1)
            } else {
                horizontalPagerState.animateScrollToPage(1)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        VerticalPager(
            state = verticalPagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = !horizontalPagerState.isScrollInProgress
                && !dragDropState.isDragging
                && horizontalPagerState.currentPage != 0,
        ) { verticalPage ->
            when (verticalPage) {
                0 -> UpPage(
                    pagerState = verticalPagerState,
                    page = verticalPage,
                    content = settingsContent,
                )
                1 -> CenterRow(
                    verticalPagerState = verticalPagerState,
                    horizontalPagerState = horizontalPagerState,
                    wallpaperImage = wallpaperImage,
                    homeContent = homeContent,
                    widgetContent = widgetContent,
                    feedContent = feedContent,
                    isDragging = dragDropState.isDragging,
                )
                2 -> DownPage(
                    pagerState = verticalPagerState,
                    page = verticalPage,
                    content = appDrawerContent,
                )
            }
        }

        // 드래그 오버레이
        if (dragDropState.isDragging) {
            val density = LocalDensity.current
            val iconSizePx = with(density) { 48.dp.toPx() }
            val halfIconPx = iconSizePx / 2

            val cancelAlpha by animateFloatAsState(
                targetValue = if (dragDropState.isInCancelZone) 1f else 0f,
                animationSpec = tween(200),
                label = "cancelAlpha",
            )

            dragDropState.draggedApp?.let { app ->
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (dragDropState.dragOffset.x - halfIconPx).roundToInt(),
                                y = (dragDropState.dragOffset.y - halfIconPx).roundToInt(),
                            )
                        }
                        .size(48.dp)
                        .graphicsLayer { alpha = 0.85f },
                ) {
                    AppIcon(
                        packageName = app.packageName,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Cancel Zone 피드백: 반투명 빨간 원 + X 아이콘
                    if (cancelAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = cancelAlpha }
                                .background(Color.Red.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "취소",
                                tint = Color.Red.copy(alpha = 0.8f),
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterRow(
    verticalPagerState: PagerState,
    horizontalPagerState: PagerState,
    wallpaperImage: String? = null,
    homeContent: @Composable () -> Unit,
    widgetContent: @Composable () -> Unit,
    feedContent: @Composable () -> Unit,
    isDragging: Boolean = false,
) {
    HorizontalPager(
        state = horizontalPagerState,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pageAlpha(verticalPagerState, 1) },
        beyondViewportPageCount = 1,
        userScrollEnabled = !verticalPagerState.isScrollInProgress && !isDragging,
    ) { horizontalPage ->
        when (horizontalPage) {
            0 -> LeftPage(
                pagerState = horizontalPagerState,
                page = horizontalPage,
                wallpaperImage = wallpaperImage,
                pageIndex = 0,
                content = feedContent,
            )
            1 -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer { alpha = pageAlpha(horizontalPagerState, 1) },
            ) {
                WallpaperLayer(wallpaperImage = wallpaperImage, pageIndex = 1)
                Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    homeContent()
                }
            }
            2 -> RightPage(
                pagerState = horizontalPagerState,
                page = horizontalPage,
                wallpaperImage = wallpaperImage,
                pageIndex = 2,
                content = widgetContent,
            )
        }
    }
}

@Composable
private fun UpPage(
    pagerState: PagerState,
    page: Int,
    content: @Composable () -> Unit,
) {
    val glassSurface = StreamLauncherTheme.colors.glassSurface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pageAlpha(pagerState, page) },
    ) {
        // 배경 레이어: 글래스 효과
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassEffect(overlayColor = glassSurface),
        )
        // 콘텐츠 레이어
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            content()
        }
    }
}

@Composable
private fun DownPage(
    pagerState: PagerState,
    page: Int,
    content: @Composable () -> Unit,
) {
    val glassSurface = StreamLauncherTheme.colors.glassSurface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pageAlpha(pagerState, page) },
    ) {
        // 배경 레이어: 글래스 효과
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassEffect(overlayColor = glassSurface),
        )
        // 콘텐츠 레이어: 선명하게 렌더링
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            content()
        }
    }
}

@Composable
private fun WallpaperLayer(
    wallpaperImage: String?,
    pageIndex: Int,
) {
    if (wallpaperImage == null) return
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(LocalDensity.current) { screenWidthDp.toPx() }

    AsyncImage(
        model = wallpaperImage,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxHeight()
            .requiredWidth(screenWidthDp * 3)
            .graphicsLayer { translationX = (1 - pageIndex) * screenWidthPx },
    )
}

@Composable
private fun LeftPage(
    pagerState: PagerState,
    page: Int,
    wallpaperImage: String? = null,
    pageIndex: Int = 0,
    content: @Composable () -> Unit,
) {
    val glassSurface = StreamLauncherTheme.colors.glassSurface.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .graphicsLayer { alpha = pageAlpha(pagerState, page) },
    ) {
        WallpaperLayer(wallpaperImage = wallpaperImage, pageIndex = pageIndex)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassEffect(overlayColor = glassSurface),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            content()
        }
    }
}

@Composable
private fun RightPage(
    pagerState: PagerState,
    page: Int,
    wallpaperImage: String? = null,
    pageIndex: Int = 2,
    content: @Composable () -> Unit,
) {
    val glassSurface = StreamLauncherTheme.colors.glassSurface.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .graphicsLayer { alpha = pageAlpha(pagerState, page) },
    ) {
        WallpaperLayer(wallpaperImage = wallpaperImage, pageIndex = pageIndex)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassEffect(overlayColor = glassSurface),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            content()
        }
    }
}

/**
 * 페이지의 현재 오프셋을 계산하여 [0.5f, 1.0f] 범위의 alpha 반환.
 * 중앙(offset=0) → 1.0f, 인접 페이지(offset=±1) → 0.5f
 */
private fun pageAlpha(pagerState: PagerState, page: Int): Float {
    val offset = pagerState.currentPageOffsetFraction + (pagerState.currentPage - page)
    return lerp(
        start = 0.5f,
        stop = 1f,
        fraction = 1f - offset.absoluteValue.coerceIn(0f, 1f),
    )
}
