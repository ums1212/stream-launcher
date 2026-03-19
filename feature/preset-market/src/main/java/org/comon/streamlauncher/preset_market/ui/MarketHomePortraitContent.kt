package org.comon.streamlauncher.preset_market.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.delay
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.preset_market.MarketTab
import org.comon.streamlauncher.preset_market.PresetMarketIntent
import org.comon.streamlauncher.preset_market.PresetMarketState
import org.comon.streamlauncher.preset_market.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun MarketHomePortraitContent(
    state: PresetMarketState,
    recentPresets: LazyPagingItems<MarketPreset>,
    onIntent: (PresetMarketIntent) -> Unit,
    onNavigateToDetailFromCard: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // 탭 (다운로드 / 좋아요)
        item {
            PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                Tab(
                    selected = state.selectedTab == MarketTab.DOWNLOADS,
                    onClick = { onIntent(PresetMarketIntent.SelectTab(MarketTab.DOWNLOADS)) },
                    text = { Text(stringResource(R.string.preset_market_top_downloads)) },
                )
                Tab(
                    selected = state.selectedTab == MarketTab.LIKES,
                    onClick = { onIntent(PresetMarketIntent.SelectTab(MarketTab.LIKES)) },
                    text = { Text(stringResource(R.string.preset_market_top_likes)) },
                )
            }
        }

        // Top 10 HorizontalPager (auto-scroll)
        item {
            val topPresets = if (state.selectedTab == MarketTab.DOWNLOADS) {
                state.topDownloadPresets
            } else {
                state.topLikePresets
            }
            if (topPresets.isNotEmpty()) {
                TopPresetPager(
                    presets = topPresets,
                    onClick = onNavigateToDetailFromCard,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            } else if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // 최근 업로드 헤더
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.preset_market_recent),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // 최근 업로드 목록 (Paging)
        items(
            count = recentPresets.itemCount,
            key = recentPresets.itemKey { it.id },
        ) { index ->
            val preset = recentPresets[index]
            if (preset != null) {
                MarketPresetListItem(
                    preset = preset,
                    onClick = { onIntent(PresetMarketIntent.ClickPreset(preset.id)) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun TopPresetPager(
    presets: List<MarketPreset>,
    onClick: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { presets.size }

    // 3초마다 자동 스크롤
    LaunchedEffect(pagerState.pageCount) {
        while (true) {
            delay(3000L)
            val next = (pagerState.currentPage + 1) % pagerState.pageCount
            pagerState.animateScrollToPage(next)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        pageSpacing = 8.dp,
    ) { page ->
        MarketPresetCard(
            preset = presets[page],
            onClick = { onClick(presets[page].id) },
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            rank = page + 1,
        )
    }
}
