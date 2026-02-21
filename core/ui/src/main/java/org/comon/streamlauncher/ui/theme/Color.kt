package org.comon.streamlauncher.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Material3 기본 폴백 색상 (구 템플릿 호환)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * StreamLauncher 전용 컬러 시스템.
 * Dynamic Color(API 31+) 연동 시 Theme.kt에서 채워지고,
 * 미지원 기기는 [DarkStreamLauncherColors] / [LightStreamLauncherColors]로 폴백.
 */
@Immutable
data class StreamLauncherColors(
    /** 그리드 셀 기본 테두리 */
    val gridBorder: Color,
    /** 확장된 그리드 셀 테두리 (포인트 컬러) */
    val gridBorderExpanded: Color,
    /** 검색바 포커스 테두리 */
    val searchBarFocused: Color,
    /** 글래스 배경 Surface 색상 (반투명) */
    val glassSurface: Color,
    /** 글래스 위 콘텐츠 색상 */
    val glassOnSurface: Color,
    /** 주 포인트 컬러 */
    val accentPrimary: Color,
    /** 보조 포인트 컬러 */
    val accentSecondary: Color,
)

val DarkStreamLauncherColors = StreamLauncherColors(
    gridBorder = Color(0xFF3D3D4E),
    gridBorderExpanded = Color(0xFF9B8FFF),
    searchBarFocused = Color(0xFF9B8FFF),
    glassSurface = Color(0xFF1A1A2E).copy(alpha = 0.85f),
    glassOnSurface = Color(0xFFE8E8F0),
    accentPrimary = Color(0xFF9B8FFF),
    accentSecondary = Color(0xFFFF8FB1),
)

val LightStreamLauncherColors = StreamLauncherColors(
    gridBorder = Color(0xFFCCCCDD),
    gridBorderExpanded = Color(0xFF6650A4),
    searchBarFocused = Color(0xFF6650A4),
    glassSurface = Color(0xFFF5F5FF).copy(alpha = 0.85f),
    glassOnSurface = Color(0xFF1C1B2E),
    accentPrimary = Color(0xFF6650A4),
    accentSecondary = Color(0xFF7D5260),
)

val LocalStreamLauncherColors = staticCompositionLocalOf { DarkStreamLauncherColors }
