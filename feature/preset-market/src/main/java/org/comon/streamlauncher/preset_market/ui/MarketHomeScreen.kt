package org.comon.streamlauncher.preset_market.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.preset_market.*
import org.comon.streamlauncher.preset_market.R
import org.comon.streamlauncher.ui.component.GoogleSignInRequiredDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MarketHomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToDetailFromCard: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToUserInfo: () -> Unit,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: PresetMarketViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentPresets = viewModel.recentPresetsPaging.collectAsLazyPagingItems()
    var showSignIn by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val signInSuccessMessage = stringResource(R.string.preset_market_sign_in_success)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PresetMarketSideEffect.NavigateToDetail -> onNavigateToDetail(effect.presetId)
                is PresetMarketSideEffect.NavigateToSearch -> onNavigateToSearch()
                is PresetMarketSideEffect.NavigateToUserInfo -> onNavigateToUserInfo()
                is PresetMarketSideEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is PresetMarketSideEffect.RequireSignIn -> showSignInDialog = true
                is PresetMarketSideEffect.SignInSuccess -> snackbarHostState.showSnackbar(signInSuccessMessage)
            }
        }
    }

    if (showSignInDialog) {
        GoogleSignInRequiredDialog(
            onConfirm = {
                showSignInDialog = false
                showSignIn = true
            },
            onDismiss = { showSignInDialog = false },
        )
    }

    if (showSignIn) {
        GoogleSignInHandler(
            onSignInSuccess = { idToken ->
                viewModel.handleIntent(PresetMarketIntent.SignInWithGoogle(idToken))
            },
            onSignInFailure = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
            onDismiss = { showSignIn = false },
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    with(sharedTransitionScope) {
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preset_market_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (state.currentUser != null) {
                        IconButton(onClick = { viewModel.handleIntent(PresetMarketIntent.NavigateToUserInfo) }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = stringResource(R.string.preset_market_user_info_title))
                        }
                    } else {
                        IconButton(onClick = { showSignInDialog = true }) {
                            Icon(Icons.Default.Person, contentDescription = stringResource(R.string.preset_market_login))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // 검색바 (탭하면 검색 화면으로 이동하는 fake SearchBar)
            item {
                Surface(
                    onClick = { viewModel.handleIntent(PresetMarketIntent.NavigateToSearch) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "market-search-bar"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        ),
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.preset_market_search_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // AdMob 배너 (광고 로딩 전에도 고정 높이 확보 → 레이아웃 이동 방지)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp), // BANNER(50dp) + 수직 패딩 4dp×2
                ) {
                    AdmobBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }
            }

            // 탭 (다운로드 / 좋아요)
            item {
                PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                    Tab(
                        selected = state.selectedTab == MarketTab.DOWNLOADS,
                        onClick = { viewModel.handleIntent(PresetMarketIntent.SelectTab(MarketTab.DOWNLOADS)) },
                        text = { Text(stringResource(R.string.preset_market_top_downloads)) },
                    )
                    Tab(
                        selected = state.selectedTab == MarketTab.LIKES,
                        onClick = { viewModel.handleIntent(PresetMarketIntent.SelectTab(MarketTab.LIKES)) },
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
                        onClick = { viewModel.handleIntent(PresetMarketIntent.ClickPreset(preset.id)) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
    } // with(sharedTransitionScope)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TopPresetPager(
    presets: List<MarketPreset>,
    onClick: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
        modifier = Modifier
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
