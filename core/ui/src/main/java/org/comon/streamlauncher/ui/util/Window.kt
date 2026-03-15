package org.comon.streamlauncher.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

enum class StreamLauncherWindowWidthSizeClass {
    Compact, Medium, Expanded
}

val LocalWindowWidthSizeClass = staticCompositionLocalOf<StreamLauncherWindowWidthSizeClass> {
    error("CompositionLocal LocalWindowWidthSizeClass not present")
}

val LocalIsCompactHeight = staticCompositionLocalOf { false }

@Composable
fun calculateWindowWidthSizeClass(): StreamLauncherWindowWidthSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    return when {
        screenWidth < 600 -> StreamLauncherWindowWidthSizeClass.Compact
        screenWidth < 840 -> StreamLauncherWindowWidthSizeClass.Medium
        else -> StreamLauncherWindowWidthSizeClass.Expanded
    }
}

@Composable
fun calculateIsCompactHeight(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp < 480
}