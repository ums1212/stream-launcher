package org.comon.streamlauncher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    selectedColor: Color = Color.White,
    unselectedColor: Color = selectedColor.copy(alpha = 0.4f),
    dotSize: Dp = 8.dp,
    smallDotSize: Dp = dotSize,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) dotSize else smallDotSize)
                    .clip(CircleShape)
                    .background(if (isSelected) selectedColor else unselectedColor),
            )
        }
    }
}
