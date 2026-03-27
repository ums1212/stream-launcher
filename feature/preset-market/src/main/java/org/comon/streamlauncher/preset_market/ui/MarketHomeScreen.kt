package org.comon.streamlauncher.preset_market.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch
import android.content.Intent
import android.provider.Settings
import org.comon.streamlauncher.preset_market.*
import org.comon.streamlauncher.preset_market.R
import org.comon.streamlauncher.ui.component.GoogleSignInRequiredDialog
import org.comon.streamlauncher.ui.util.calculateIsCompactHeight

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
    presetDeleted: Boolean = false,
    onPresetDeletedConsumed: () -> Unit = {},
    presetReported: Boolean = false,
    onPresetReportedConsumed: () -> Unit = {},
    viewModel: PresetMarketViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentPresets = viewModel.recentPresetsPaging.collectAsLazyPagingItems()
    var showSignIn by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isCompactLandscape = calculateIsCompactHeight()

    val signInSuccessMessage = stringResource(R.string.preset_market_sign_in_success)
    val deleteSuccessMessage = stringResource(R.string.preset_market_delete_success)
    val reportSuccessMessage = stringResource(R.string.preset_market_report_success)

    LaunchedEffect(presetDeleted) {
        if (presetDeleted) {
            recentPresets.refresh()
            viewModel.handleIntent(PresetMarketIntent.LoadTopPresets)
            onPresetDeletedConsumed()
            snackbarHostState.showSnackbar(deleteSuccessMessage)
        }
    }

    LaunchedEffect(presetReported) {
        if (presetReported) {
            snackbarHostState.showSnackbar(reportSuccessMessage)
            onPresetReportedConsumed()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PresetMarketSideEffect.NavigateToDetail -> onNavigateToDetail(effect.presetId)
                is PresetMarketSideEffect.NavigateToSearch -> onNavigateToSearch()
                is PresetMarketSideEffect.NavigateToUserInfo -> onNavigateToUserInfo()
                is PresetMarketSideEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is PresetMarketSideEffect.ShowNetworkError -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "네트워크 연결을 확인해주세요",
                        actionLabel = "설정",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) {}
                    }
                }
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

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                SwipeToDismissBox(
                    state = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                snackbarData.dismiss()
                                true
                            } else false
                        }
                    ),
                    backgroundContent = {},
                ) {
                    Snackbar(snackbarData)
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preset_market_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    // landscape에서는 fake SearchBar 대신 TopAppBar 검색 아이콘 제공
                    if (isCompactLandscape) {
                        IconButton(onClick = { viewModel.handleIntent(PresetMarketIntent.NavigateToSearch) }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.preset_market_search_hint),
                            )
                        }
                    }
                    if (state.currentUser != null) {
                        IconButton(onClick = { viewModel.handleIntent(PresetMarketIntent.NavigateToUserInfo) }) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = stringResource(R.string.preset_market_user_info_title),
                            )
                        }
                    } else {
                        IconButton(onClick = { showSignInDialog = true }) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = stringResource(R.string.preset_market_login),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // 검색창 — portrait일 때만 표시
            if (!isCompactLandscape) {
                with(sharedTransitionScope) {
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
            }

            // AdMob 배너
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 58.dp),
                contentAlignment = Alignment.Center,
            ) {
                AdmobBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }

            // 콘텐츠 레이아웃
            if (isCompactLandscape) {
                MarketHomeLandscapeContent(
                    state = state,
                    recentPresets = recentPresets,
                    onIntent = { viewModel.handleIntent(it) },
                    onNavigateToDetailFromCard = onNavigateToDetailFromCard,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.weight(1f),
                )
            } else {
                MarketHomePortraitContent(
                    state = state,
                    recentPresets = recentPresets,
                    onIntent = { viewModel.handleIntent(it) },
                    onNavigateToDetailFromCard = onNavigateToDetailFromCard,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
