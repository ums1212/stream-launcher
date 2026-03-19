package org.comon.streamlauncher.wallpaper

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.graphics.ColorUtils
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import org.comon.streamlauncher.settings.navigation.SettingsDetail
import org.comon.streamlauncher.settings.navigation.PresetMarketHost as PresetMarketRoute

class WallpaperSystemBarManager(private val activity: ComponentActivity) {

    val isWallpaperDark = mutableStateOf(false)
    val isCurrentScreenOpaque = mutableStateOf(false)

    private val wallpaperColorsChangedListener =
        WallpaperManager.OnColorsChangedListener { colors, _ ->
            val dark = isDarkFromColors(colors)
            isWallpaperDark.value = dark
            updateSystemBarStyle(dark, isCurrentScreenOpaque.value)
        }

    fun initialize() {
        val wallpaperManager = WallpaperManager.getInstance(activity)
        val initialColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        val initialDark = isDarkFromColors(initialColors)
        isWallpaperDark.value = initialDark
        updateSystemBarStyle(initialDark, false)
        wallpaperManager.addOnColorsChangedListener(
            wallpaperColorsChangedListener,
            Handler(Looper.getMainLooper()),
        )
    }

    fun onResume() {
        val colors = WallpaperManager.getInstance(activity)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        val isDark = isDarkFromColors(colors)
        if (isDark != isWallpaperDark.value) {
            isWallpaperDark.value = isDark
            updateSystemBarStyle(isDark, isCurrentScreenOpaque.value)
        }
    }

    fun onConfigurationChanged() {
        updateSystemBarStyle(isWallpaperDark.value, isCurrentScreenOpaque.value)
    }

    fun destroy() {
        WallpaperManager.getInstance(activity)
            .removeOnColorsChangedListener(wallpaperColorsChangedListener)
    }

    @Composable
    fun ObserveDestinationChanges(navController: NavHostController) {
        DisposableEffect(navController) {
            val listener = NavController.OnDestinationChangedListener { _, dest, _ ->
                val isOpaque = isOpaqueDestination(dest)
                isCurrentScreenOpaque.value = isOpaque
                updateSystemBarStyle(isWallpaperDark.value, isOpaque)
            }
            navController.addOnDestinationChangedListener(listener)
            onDispose { navController.removeOnDestinationChangedListener(listener) }
        }
    }

    /**
     * WallpaperColors에서 배경이 "어두운지" 판별합니다.
     *
     * - API 31+: HINT_SUPPORTS_DARK_TEXT 비트 플래그 (공식 방법)
     * - API 28-30: primaryColor의 luminance로 직접 판별
     *   (HINT_SUPPORTS_DARK_TEXT 상수 자체가 API 31에서 추가되었으므로
     *    그 이전 버전에서 colorHints를 비교하면 항상 0이 반환됨)
     *
     * @return true = 어두운 배경 → 시스템바 아이콘 흰색
     *         false = 밝은 배경 → 시스템바 아이콘 검은색
     */
    private fun isDarkFromColors(colors: WallpaperColors?): Boolean {
        if (colors == null) return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) == 0
        } else {
            val luminance = ColorUtils.calculateLuminance(colors.primaryColor.toArgb())
            luminance < 0.5
        }
    }

    /**
     * NavDestination이 불투명 배경(Material3)을 가진 화면인지 판별합니다.
     * - 설정 상세 화면 (SettingsDetail)
     * - 프리셋 마켓 호스트 화면 (PresetMarketRoute)
     */
    private fun isOpaqueDestination(dest: NavDestination): Boolean {
        val route = dest.route ?: return false
        return route.startsWith(SettingsDetail::class.qualifiedName ?: "") ||
            route.startsWith(PresetMarketRoute::class.qualifiedName ?: "")
    }

    /**
     * 시스템바 아이콘 색상을 동적으로 전환합니다.
     * - 불투명 화면(설정 상세, 프리셋 마켓): 시스템 다크/라이트 모드 기준
     * - 런처 홈 화면: 월페이퍼 밝기 기준
     */
    private fun updateSystemBarStyle(isWallpaperDark: Boolean, isOpaqueRoute: Boolean) {
        val transparent = android.graphics.Color.TRANSPARENT
        val useDarkIcons = if (isOpaqueRoute) {
            val nightMode = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightMode != Configuration.UI_MODE_NIGHT_YES
        } else {
            !isWallpaperDark
        }
        if (useDarkIcons) {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(transparent, transparent),
                navigationBarStyle = SystemBarStyle.light(transparent, transparent),
            )
        } else {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(transparent),
                navigationBarStyle = SystemBarStyle.dark(transparent),
            )
        }
    }
}
