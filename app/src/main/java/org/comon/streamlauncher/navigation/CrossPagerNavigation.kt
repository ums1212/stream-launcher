package org.comon.streamlauncher.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * 십자형(상/하/좌/우) 스와이프 내비게이션 컨테이너.
 *
 * 구조:
 *   VerticalPager(3페이지, 초기=1)
 *   ├─ [0] UpPage     — Notifications & Settings
 *   ├─ [1] CenterRow  — HorizontalPager(3페이지, 초기=1)
 *   │       ├─ [0] LeftPage  — Feed & Announcements
 *   │       ├─ [1] homeContent()
 *   │       └─ [2] RightPage — Widget Area
 *   └─ [2] DownPage   — App Drawer
 *
 * 뒤로가기: 중앙이 아닐 때 animateScrollToPage(1)로 홈 복귀
 * 제스처 간섭 방지: 한 Pager 스크롤 중 다른 Pager userScrollEnabled = false
 * Alpha 효과: 중앙에 가까울수록 1.0f, 멀수록 0.5f
 */
@Composable
fun CrossPagerNavigation(
    modifier: Modifier = Modifier,
    resetTrigger: Int = 0,
    homeContent: @Composable () -> Unit,
) {
    val verticalPagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val horizontalPagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val isAtCenter = verticalPagerState.currentPage == 1 && horizontalPagerState.currentPage == 1

    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0) {
            verticalPagerState.animateScrollToPage(1, animationSpec = tween(300))
            horizontalPagerState.animateScrollToPage(1, animationSpec = tween(300))
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

    VerticalPager(
        state = verticalPagerState,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        userScrollEnabled = !horizontalPagerState.isScrollInProgress,
    ) { verticalPage ->
        when (verticalPage) {
            0 -> UpPage(pagerState = verticalPagerState, page = verticalPage)
            1 -> CenterRow(
                verticalPagerState = verticalPagerState,
                horizontalPagerState = horizontalPagerState,
                homeContent = homeContent,
            )
            2 -> DownPage(pagerState = verticalPagerState, page = verticalPage)
        }
    }
}

@Composable
private fun CenterRow(
    verticalPagerState: PagerState,
    horizontalPagerState: PagerState,
    homeContent: @Composable () -> Unit,
) {
    HorizontalPager(
        state = horizontalPagerState,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pageAlpha(verticalPagerState, 1) },
        beyondViewportPageCount = 1,
        userScrollEnabled = !verticalPagerState.isScrollInProgress,
    ) { horizontalPage ->
        when (horizontalPage) {
            0 -> LeftPage(pagerState = horizontalPagerState, page = horizontalPage)
            1 -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = pageAlpha(horizontalPagerState, 1) },
            ) {
                Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    homeContent()
                }
            }
            2 -> RightPage(pagerState = horizontalPagerState, page = horizontalPage)
        }
    }
}

@Composable
private fun UpPage(pagerState: PagerState, page: Int) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pageAlpha(pagerState, page) },
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Text("Notifications & Settings", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun DownPage(pagerState: PagerState, page: Int) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pageAlpha(pagerState, page) },
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            Text("App Drawer", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun LeftPage(pagerState: PagerState, page: Int) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pageAlpha(pagerState, page) },
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Text("Feed & Announcements", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun RightPage(pagerState: PagerState, page: Int) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pageAlpha(pagerState, page) },
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Text("Widget Area", style = MaterialTheme.typography.headlineSmall)
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
