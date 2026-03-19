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
    // → 전체 ColorScheme 속성을 accent 색상 기반으로 파생하여 시각적 조화 확보
    val colorScheme = if (accentPrimaryOverride != null || accentSecondaryOverride != null) {
        val primary = accentPrimaryOverride ?: baseColorScheme.primary
        val tertiary = accentSecondaryOverride ?: baseColorScheme.tertiary
        // 다크/라이트 기준 베이스 서피스 색상
        val surfaceBase = if (darkTheme) Color(0xFF1C1B1E) else Color(0xFFFFFBFE)

        // on-surface 기준 색상 (다크/라이트 MD3 기본값)
        val onSurfaceBase = if (darkTheme) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
        val onSurfaceVariantBase = if (darkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F)
        val outlineBase = if (darkTheme) Color(0xFF938F99) else Color(0xFF79747E)
        val outlineVariantBase = if (darkTheme) Color(0xFF49454F) else Color(0xFFCAC4D0)

        // on-surface 파생 (accent 약하게 혼합)
        val onSurface = lerp(onSurfaceBase, primary, if (darkTheme) 0.08f else 0.05f)
        val onSurfaceVariant = lerp(onSurfaceVariantBase, primary, if (darkTheme) 0.10f else 0.08f)

        baseColorScheme.copy(
            // Primary / Tertiary
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = lerp(surfaceBase, primary, if (darkTheme) 0.30f else 0.22f),
            onPrimaryContainer = lerp(if (darkTheme) Color.White else Color.Black, primary, if (darkTheme) 0.15f else 0.10f),
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = lerp(surfaceBase, tertiary, if (darkTheme) 0.30f else 0.22f),
            onTertiaryContainer = lerp(if (darkTheme) Color.White else Color.Black, tertiary, if (darkTheme) 0.15f else 0.10f),
            // Secondary (tertiary에서 파생)
            secondary = tertiary,
            onSecondary = Color.White,
            secondaryContainer = lerp(surfaceBase, tertiary, if (darkTheme) 0.25f else 0.18f),
            onSecondaryContainer = lerp(if (darkTheme) Color.White else Color.Black, tertiary, if (darkTheme) 0.15f else 0.10f),
            // Background / Surface family
            background = lerp(surfaceBase, primary, if (darkTheme) 0.06f else 0.03f),
            onBackground = onSurface,
            surface = lerp(surfaceBase, primary, if (darkTheme) 0.06f else 0.03f),
            onSurface = onSurface,
            surfaceDim = lerp(surfaceBase, primary, if (darkTheme) 0.03f else 0.01f),
            surfaceBright = lerp(surfaceBase, primary, if (darkTheme) 0.12f else 0.06f),
            surfaceContainerLowest = lerp(surfaceBase, primary, if (darkTheme) 0.02f else 0.01f),
            surfaceContainerLow = lerp(surfaceBase, primary, if (darkTheme) 0.04f else 0.02f),
            surfaceContainer = lerp(surfaceBase, primary, if (darkTheme) 0.08f else 0.04f),
            surfaceContainerHigh = lerp(surfaceBase, primary, if (darkTheme) 0.12f else 0.08f),
            surfaceContainerHighest = lerp(surfaceBase, primary, if (darkTheme) 0.16f else 0.10f),
            surfaceTint = primary,
            // SurfaceVariant (그리드 셀 배경)
            surfaceVariant = lerp(surfaceBase, primary, if (darkTheme) 0.18f else 0.12f),
            onSurfaceVariant = onSurfaceVariant,
            // Outline
            outline = lerp(outlineBase, primary, if (darkTheme) 0.20f else 0.15f),
            outlineVariant = lerp(outlineVariantBase, primary, if (darkTheme) 0.15f else 0.10f),
            // Inverse
            inverseSurface = lerp(if (darkTheme) Color(0xFFE6E1E5) else Color(0xFF313033), primary, 0.06f),
            inverseOnSurface = lerp(if (darkTheme) Color(0xFF313033) else Color(0xFFF4EFF4), primary, 0.06f),
            inversePrimary = if (darkTheme) lerp(primary, Color.White, 0.40f) else lerp(primary, Color.Black, 0.30f),
            // Error 색상은 시맨틱 의미 보존을 위해 기본값 유지
        )
    } else {
        baseColorScheme
    }

    // 3단계: StreamLauncherColors 계산 — 항상 colorScheme에서 파생 (accent override 자동 반영)
    val baseStreamColors = StreamLauncherColors(
        accentPrimary = colorScheme.primary,
        accentSecondary = colorScheme.tertiary,
        gridBorder = colorScheme.outlineVariant,
        gridBorderExpanded = colorScheme.primary,
        searchBarFocused = colorScheme.primary,
        glassSurface = colorScheme.surface.copy(alpha = 0.85f),
        glassOnSurface = colorScheme.onSurface,
    )

    // 4단계: accent 오버라이드를 StreamLauncherColors에도 반영 (gridBorder/glassSurface/glassOnSurface 포함)
    val streamColors = if (accentPrimaryOverride != null || accentSecondaryOverride != null) {
        baseStreamColors.copy(
            accentPrimary = accentPrimaryOverride ?: baseStreamColors.accentPrimary,
            accentSecondary = accentSecondaryOverride ?: baseStreamColors.accentSecondary,
            gridBorderExpanded = accentPrimaryOverride ?: baseStreamColors.gridBorderExpanded,
            searchBarFocused = accentPrimaryOverride ?: baseStreamColors.searchBarFocused,
            gridBorder = colorScheme.outlineVariant,
            glassSurface = colorScheme.surface.copy(alpha = 0.85f),
            glassOnSurface = colorScheme.onSurface,
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
