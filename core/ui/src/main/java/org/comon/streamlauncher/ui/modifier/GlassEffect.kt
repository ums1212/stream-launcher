package org.comon.streamlauncher.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

/**
 * Glassmorphism 배경 효과 Modifier.
 *
 * 배경 레이어에 적용하고, 콘텐츠 레이어는 별도의 Box에 배치해야 한다.
 * 반투명 오버레이로 뒤의 배경(배경화면 등)이 비치는 글래스 효과를 연출한다.
 *
 * @param overlayColor 반투명 오버레이 색상 (glassSurface 권장)
 */
fun Modifier.glassEffect(
    overlayColor: Color = Color(0x88000000),
): Modifier = this.drawBehind { drawRect(overlayColor) }
