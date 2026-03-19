package org.comon.streamlauncher.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.comon.streamlauncher.preset_market.ui.PresetMarketHost
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState
import org.comon.streamlauncher.settings.navigation.Launcher
import org.comon.streamlauncher.settings.navigation.SettingsDetail
import org.comon.streamlauncher.settings.navigation.SettingsMenu
import org.comon.streamlauncher.settings.ui.SettingsDetailScreen
import org.comon.streamlauncher.settings.navigation.PresetMarketHost as PresetMarketRoute

@Composable
fun MainNavHost(
    navController: NavHostController,
    launcherContent: @Composable () -> Unit,
    settingsState: SettingsState,
    onSettingsIntent: (SettingsIntent) -> Unit,
    onShowSnackbar: (String) -> Unit,
    onRequireSignIn: () -> Unit,
    onStartDownloadService: (String) -> Unit,
    onStopDownloadService: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Launcher,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
    ) {
        composable<Launcher>(
            enterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            launcherContent()
        }

        composable<SettingsDetail>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<SettingsDetail>()
            val menu = runCatching { SettingsMenu.valueOf(route.menu) }.getOrNull()
                ?: return@composable
            SettingsDetailScreen(
                menu = menu,
                state = settingsState,
                onIntent = onSettingsIntent,
                onBack = { navController.popBackStack() },
                onNavigateToMarket = { navController.navigate(PresetMarketRoute) },
                onShowSnackbar = onShowSnackbar,
                onRequireSignIn = onRequireSignIn,
            )
        }

        composable<PresetMarketRoute>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
        ) {
            PresetMarketHost(
                onBack = { navController.popBackStack() },
                onStartDownloadService = onStartDownloadService,
                onStopDownloadService = onStopDownloadService,
            )
        }
    }
}
