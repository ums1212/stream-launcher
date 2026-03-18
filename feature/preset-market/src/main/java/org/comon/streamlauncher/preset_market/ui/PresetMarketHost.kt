package org.comon.streamlauncher.preset_market.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
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
                exitTransition = { fadeOut(tween(300)) },
                popEnterTransition = { fadeIn(tween(300)) },
            ) {
                MarketHomeScreen(
                    onNavigateToDetail = { navController.navigate(MarketDetail(presetId = it, fromCard = false)) },
                    onNavigateToDetailFromCard = { navController.navigate(MarketDetail(presetId = it, fromCard = true)) },
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
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(300)) },
                popEnterTransition = { fadeIn(tween(300)) },
                popExitTransition = { fadeOut(tween(300)) },
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<MarketDetail>()
                PresetDetailScreen(
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                    fromCard = route.fromCard,
                    onStartDownloadService = onStartDownloadService,
                    onStopDownloadService = onStopDownloadService,
                )
            }
        }
    }
}
