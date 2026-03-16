package org.comon.streamlauncher.preset_market.ui

import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.comon.streamlauncher.preset_market.navigation.MarketRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PresetMarketHost(
    onBack: () -> Unit,
    onStartDownloadService: (String) -> Unit,
    onStopDownloadService: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    SharedTransitionLayout {
        val sharedTransitionScope = this
        NavHost(
            navController = navController,
            startDestination = MarketRoute.HOME,
            modifier = modifier,
        ) {
            composable(route = MarketRoute.HOME) {
                MarketHomeScreen(
                    onNavigateToDetail = { navController.navigate(MarketRoute.detail(it)) },
                    onNavigateToSearch = { navController.navigate(MarketRoute.search()) },
                    onBack = onBack,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                )
            }
            composable(
                route = MarketRoute.SEARCH,
                arguments = listOf(navArgument("query") { defaultValue = "" }),
                enterTransition = { fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                popExitTransition = { fadeOut() },
            ) { backStackEntry ->
                val query = Uri.decode(backStackEntry.arguments?.getString("query") ?: "")
                MarketSearchScreen(
                    initialQuery = query,
                    onNavigateToDetail = { navController.navigate(MarketRoute.detail(it)) },
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                )
            }
            composable(
                route = MarketRoute.DETAIL,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
            ) {
                PresetDetailScreen(
                    onBack = { navController.popBackStack() },
                    onStartDownloadService = onStartDownloadService,
                    onStopDownloadService = onStopDownloadService,
                )
            }
        }
    }
}
