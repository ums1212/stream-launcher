package org.comon.streamlauncher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    selectedColor: Color = Color.White,
    unselectedColor: Color = selectedColor.copy(alpha = 0.4f),
    dotSize: Dp = 8.dp,
    smallDotSize: Dp = dotSize,
    onPageClick: ((Int) -> Unit)? = null,
    // 드래그 중 손가락 아래에 있는 도트의 페이지 인덱스(0-based)를 전달
    onSwipe: ((Int) -> Unit)? = null,
) {
    val dotCentersX = remember(pageCount) { Array(pageCount) { 0f } }

    fun nearestPage(x: Float): Int =
        dotCentersX.indices.minByOrNull { abs(dotCentersX[it] - x) } ?: 0

    val swipeModifier = if (onSwipe != null) {
        Modifier.pointerInput(onSwipe, pageCount) {
            var lastTriggeredPage = -1
            detectHorizontalDragGestures(
                onDragStart = { lastTriggeredPage = -1 },
                onDragEnd = { lastTriggeredPage = -1 },
                onDragCancel = { lastTriggeredPage = -1 },
            ) { change, _ ->
                val page = nearestPage(change.position.x)
                if (page != lastTriggeredPage) {
                    lastTriggeredPage = page
                    onSwipe(page)
                }
            }
        }
    } else {
        Modifier
    }

    Row(
        modifier = modifier.then(swipeModifier),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                Modifier
                    .onGloballyPositioned { coords ->
                        dotCentersX[index] = coords.positionInParent().x + coords.size.width / 2f
                    }
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) dotSize else smallDotSize)
                    .clip(CircleShape)
                    .background(if (isSelected) selectedColor else unselectedColor)
                    .then(
                        if (onPageClick != null) Modifier.clickable { onPageClick(index) }
                        else Modifier
                    ),
            )
        }
    }
}
