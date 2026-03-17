package org.comon.streamlauncher.preset_market.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.navigation.toRoute
import org.comon.streamlauncher.preset_market.navigation.MarketDetail
import org.comon.streamlauncher.preset_market.navigation.MarketHome
import org.comon.streamlauncher.preset_market.navigation.MarketSearch

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
            startDestination = MarketHome,
            modifier = modifier,
        ) {
            composable<MarketHome>(
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
            ) {
                MarketHomeScreen(
                    onNavigateToDetail = { navController.navigate(MarketDetail(presetId = it)) },
                    onNavigateToSearch = { navController.navigate(MarketSearch()) },
                    onBack = onBack,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                )
            }
            composable<MarketSearch>(
                enterTransition = { fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                popExitTransition = { fadeOut() },
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<MarketSearch>()
                MarketSearchScreen(
                    initialQuery = route.query,
                    onNavigateToDetail = { navController.navigate(MarketDetail(presetId = it)) },
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                )
            }
            composable<MarketDetail>(
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
