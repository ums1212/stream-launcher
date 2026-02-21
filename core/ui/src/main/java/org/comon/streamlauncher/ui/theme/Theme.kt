package org.comon.streamlauncher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

@Composable
fun StreamLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    accentPrimaryOverride: Color? = null,
    accentSecondaryOverride: Color? = null,
    content: @Composable () -> Unit,
) {
    // 1단계: 기본 colorScheme 결정 (Dynamic Color 또는 정적 다크/라이트)
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 2단계: accent 오버라이드가 있으면 MaterialTheme.colorScheme도 함께 갱신
    // → 그리드 배경(surfaceVariant), 보더(primary), 앱 배경(surface/background) 모두 반영
    val colorScheme = if (accentPrimaryOverride != null || accentSecondaryOverride != null) {
        val primary = accentPrimaryOverride ?: baseColorScheme.primary
        val tertiary = accentSecondaryOverride ?: baseColorScheme.tertiary
        // 다크/라이트 기준 베이스 서피스 색상
        val surfaceBase = if (darkTheme) Color(0xFF1C1B1E) else Color(0xFFFFFBFE)
        baseColorScheme.copy(
            primary = primary,
            onPrimary = Color.White,
            tertiary = tertiary,
            onTertiary = Color.White,
            // 배경: accent 색상을 아주 약하게 혼합 (3~6%)
            background = lerp(surfaceBase, primary, if (darkTheme) 0.06f else 0.03f),
            surface = lerp(surfaceBase, primary, if (darkTheme) 0.06f else 0.03f),
            // 그리드 셀 배경(surfaceVariant): accent 색상을 적당히 혼합 (12~18%)
            surfaceVariant = lerp(surfaceBase, primary, if (darkTheme) 0.18f else 0.12f),
        )
    } else {
        baseColorScheme
    }

    // 3단계: StreamLauncherColors 계산 (colorScheme 기반)
    val baseStreamColors: StreamLauncherColors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> StreamLauncherColors(
            accentPrimary = colorScheme.primary,
            accentSecondary = colorScheme.tertiary,
            gridBorder = colorScheme.outlineVariant,
            gridBorderExpanded = colorScheme.primary,
            searchBarFocused = colorScheme.primary,
            glassSurface = colorScheme.surface.copy(alpha = 0.85f),
            glassOnSurface = colorScheme.onSurface,
        )
        darkTheme -> DarkStreamLauncherColors
        else -> LightStreamLauncherColors
    }

    // 4단계: accent 오버라이드를 StreamLauncherColors에도 반영
    val streamColors = if (accentPrimaryOverride != null || accentSecondaryOverride != null) {
        baseStreamColors.copy(
            accentPrimary = accentPrimaryOverride ?: baseStreamColors.accentPrimary,
            accentSecondary = accentSecondaryOverride ?: baseStreamColors.accentSecondary,
            gridBorderExpanded = accentPrimaryOverride ?: baseStreamColors.gridBorderExpanded,
            searchBarFocused = accentPrimaryOverride ?: baseStreamColors.searchBarFocused,
        )
    } else {
        baseStreamColors
    }

    CompositionLocalProvider(LocalStreamLauncherColors provides streamColors) {
        MaterialTheme(
            colorScheme = colorScheme, // accent override가 반영된 colorScheme 전달
            typography = Typography,
            content = content,
        )
    }
}

/** `StreamLauncherTheme.colors` 편의 접근자 */
object StreamLauncherTheme {
    val colors: StreamLauncherColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStreamLauncherColors.current
}
