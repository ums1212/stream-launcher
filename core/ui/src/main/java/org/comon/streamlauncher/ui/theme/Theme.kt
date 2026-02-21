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
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Dynamic Color(API 31+) 연동: 배경화면 색상을 포인트 컬러에 반영
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

    // 사용자 지정 accent 색상이 있으면 덮어씀
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
            colorScheme = colorScheme,
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
