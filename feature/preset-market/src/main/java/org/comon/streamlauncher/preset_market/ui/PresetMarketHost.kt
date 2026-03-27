package org.comon.streamlauncher.preset_market.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.comon.streamlauncher.preset_market.navigation.MarketDetail
import org.comon.streamlauncher.preset_market.navigation.MarketHome
import org.comon.streamlauncher.preset_market.navigation.MarketReport
import org.comon.streamlauncher.preset_market.navigation.MarketSearch
import org.comon.streamlauncher.preset_market.navigation.MarketUserInfo

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
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
        ) {
            composable<MarketHome> { backStackEntry ->
                val presetDeleted by backStackEntry.savedStateHandle
                    .getStateFlow("presetDeleted", false)
                    .collectAsStateWithLifecycle()
                val presetReported by backStackEntry.savedStateHandle
                    .getStateFlow("presetReported", false)
                    .collectAsStateWithLifecycle()

                MarketHomeScreen(
                    onNavigateToDetail = { navController.navigate(MarketDetail(presetId = it, fromCard = false)) },
                    onNavigateToDetailFromCard = { navController.navigate(MarketDetail(presetId = it, fromCard = true)) },
                    onNavigateToSearch = { navController.navigate(MarketSearch()) },
                    onNavigateToUserInfo = { navController.navigate(MarketUserInfo) },
                    onBack = onBack,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                    presetDeleted = presetDeleted,
                    onPresetDeletedConsumed = { backStackEntry.savedStateHandle["presetDeleted"] = false },
                    presetReported = presetReported,
                    onPresetReportedConsumed = { backStackEntry.savedStateHandle["presetReported"] = false },
                )
            }
            composable<MarketSearch> { backStackEntry ->
                val route = backStackEntry.toRoute<MarketSearch>()
                MarketSearchScreen(
                    initialQuery = route.query,
                    onNavigateToDetail = { navController.navigate(MarketDetail(presetId = it)) },
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                )
            }
            composable<MarketDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<MarketDetail>()
                PresetDetailScreen(
                    onBack = { navController.popBackStack() },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                    fromCard = route.fromCard,
                    onStartDownloadService = onStartDownloadService,
                    onStopDownloadService = onStopDownloadService,
                    onDeleteSuccess = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("presetDeleted", true)
                        navController.popBackStack()
                    },
                    onNavigateToReport = { presetId, presetName, authorUid, authorDisplayName ->
                        navController.navigate(
                            MarketReport(
                                presetId = presetId,
                                presetName = presetName,
                                presetAuthorUid = authorUid,
                                presetAuthorDisplayName = authorDisplayName,
                            )
                        )
                    },
                )
            }
            composable<MarketUserInfo> {
                PresetMarketUserInfoScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { navController.navigate(MarketDetail(presetId = it)) },
                )
            }
            composable<MarketReport> {
                ReportPresetScreen(
                    onBack = { navController.popBackStack() },
                    onReportSuccess = {
                        navController.getBackStackEntry(MarketHome)
                            .savedStateHandle["presetReported"] = true
                        navController.popBackStack(MarketHome, inclusive = false)
                    },
                )
            }
        }
    }
}
