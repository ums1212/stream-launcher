package org.comon.streamlauncher.preset_market.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.preset_market.MarketTab
import org.comon.streamlauncher.preset_market.PresetMarketIntent
import org.comon.streamlauncher.preset_market.PresetMarketState
import org.comon.streamlauncher.preset_market.R
import org.comon.streamlauncher.preset_market.ui.component.MarketPresetLandscapeCard

private enum class LandscapeSideTab { TOP_10, RECENT }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun MarketHomeLandscapeContent(
    state: PresetMarketState,
    recentPresets: LazyPagingItems<MarketPreset>,
    onIntent: (PresetMarketIntent) -> Unit,
    onNavigateToDetailFromCard: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    var selectedSideTab by remember { mutableStateOf(LandscapeSideTab.TOP_10) }

    Row(
        modifier = modifier.fillMaxSize(),
    ) {
        // 왼쪽 사이드탭 — IntrinsicSize.Max로 가장 넓은 아이템 너비에 맞추고 fillMaxWidth로 채움
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SideTabItem(
                selected = selectedSideTab == LandscapeSideTab.TOP_10,
                onClick = { selectedSideTab = LandscapeSideTab.TOP_10 },
                icon = Icons.Default.Star,
                label = stringResource(R.string.preset_market_side_tab_top10),
                modifier = Modifier.fillMaxWidth(),
            )
            SideTabItem(
                selected = selectedSideTab == LandscapeSideTab.RECENT,
                onClick = { selectedSideTab = LandscapeSideTab.RECENT },
                icon = Icons.Default.Schedule,
                label = stringResource(R.string.preset_market_recent),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        VerticalDivider()

        // 오른쪽 콘텐츠
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (selectedSideTab) {
                LandscapeSideTab.TOP_10 -> {
                    Top10LandscapeContent(
                        state = state,
                        onIntent = onIntent,
                        onNavigateToDetailFromCard = onNavigateToDetailFromCard,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }
                LandscapeSideTab.RECENT -> {
                    RecentLandscapeContent(
                        recentPresets = recentPresets,
                        onIntent = onIntent,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun Top10LandscapeContent(
    state: PresetMarketState,
    onIntent: (PresetMarketIntent) -> Unit,
    onNavigateToDetailFromCard: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Downloads / Likes 서브탭
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
                modifier = Modifier.weight(1f),
            )
        } else if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun RecentLandscapeContent(
    recentPresets: LazyPagingItems<MarketPreset>,
    onIntent: (PresetMarketIntent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                count = recentPresets.itemCount,
                key = recentPresets.itemKey { it.id },
            ) { index ->
                val preset = recentPresets[index]
                if (preset != null) {
                    MarketPresetLandscapeCard(
                        preset = preset,
                        onClick = { onIntent(PresetMarketIntent.ClickPreset(preset.id)) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        fromCard = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun SideTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
