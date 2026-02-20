package org.comon.streamlauncher.ui.modifier

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism 배경 효과 Modifier.
 *
 * 배경 레이어에 적용하고, 콘텐츠 레이어는 별도의 Box에 배치해야 한다.
 *
 * - API 31+: Compose [blur] + [overlayColor] 반투명 오버레이
 * - API 28-30: [overlayColor] 단색 폴백 (blur 미지원 기기 대응)
 *
 * @param blurRadius 블러 반경. 기본값 20.dp
 * @param overlayColor 반투명 오버레이 색상 (glassSurface 권장)
 */
fun Modifier.glassEffect(
    blurRadius: Dp = 20.dp,
    overlayColor: Color = Color(0x88000000),
): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    this
        .drawBehind { drawRect(overlayColor) }
        .blur(blurRadius)
} else {
    this.drawBehind { drawRect(overlayColor) }
}
