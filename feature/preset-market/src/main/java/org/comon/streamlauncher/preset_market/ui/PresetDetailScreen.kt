package org.comon.streamlauncher.preset_market.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.comon.streamlauncher.preset_market.*
import org.comon.streamlauncher.preset_market.R
import org.comon.streamlauncher.ui.component.GoogleSignInRequiredDialog
import org.comon.streamlauncher.ui.component.PagerIndicator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PresetDetailScreen(
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    fromCard: Boolean = false,
    onStartDownloadService: (String) -> Unit = {},
    onStopDownloadService: () -> Unit = {},
    onDeleteSuccess: () -> Unit = {},
    viewModel: PresetDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSignInHandler by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showCancelDownloadDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportNotReadyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val downloadCompleteMessage = stringResource(R.string.preset_market_download_complete)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PresetDetailSideEffect.DownloadComplete ->
                    snackbarHostState.showSnackbar(message = downloadCompleteMessage)
                is PresetDetailSideEffect.ShowError ->
                    snackbarHostState.showSnackbar(message = effect.message)
                is PresetDetailSideEffect.RequireSignIn ->
                    showSignInDialog = true
                is PresetDetailSideEffect.PresetLimitExceeded ->
                    showLimitDialog = true
                is PresetDetailSideEffect.StartDownloadService ->
                    onStartDownloadService(effect.presetName)
                is PresetDetailSideEffect.StopDownloadService ->
                    onStopDownloadService()
                is PresetDetailSideEffect.DeleteComplete ->
                    onDeleteSuccess()
            }
        }
    }

    if (showSignInDialog) {
        GoogleSignInRequiredDialog(
            onConfirm = {
                showSignInDialog = false
                showSignInHandler = true
            },
            onDismiss = { showSignInDialog = false },
        )
    }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text(stringResource(R.string.preset_market_download_limit_title)) },
            text = { Text(stringResource(R.string.preset_market_download_limit_message)) },
            confirmButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        )
    }

    if (showCancelDownloadDialog) {
        LaunchedEffect(Unit) {
            viewModel.handleIntent(PresetDetailIntent.PauseDownload)
        }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.preset_market_cancel_download_title)) },
            text = { Text(stringResource(R.string.preset_market_cancel_download_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDownloadDialog = false
                    viewModel.handleIntent(PresetDetailIntent.CancelDownload)
                }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.handleIntent(PresetDetailIntent.ResumeDownload)
                    showCancelDownloadDialog = false
                }) {
                    Text(stringResource(R.string.preset_market_cancel_download_resume))
                }
            },
        )
    }

    if (showReportNotReadyDialog) {
        AlertDialog(
            onDismissRequest = { showReportNotReadyDialog = false },
            title = { Text(stringResource(R.string.preset_market_report_not_ready_title)) },
            text = { Text(stringResource(R.string.preset_market_report_not_ready_message)) },
            confirmButton = {
                TextButton(onClick = { showReportNotReadyDialog = false }) {
                    Text(stringResource(R.string.confirm))
                }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.preset_market_delete_title)) },
            text = { Text(stringResource(R.string.preset_market_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.handleIntent(PresetDetailIntent.DeletePreset)
                }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showSignInHandler) {
        GoogleSignInHandler(
            onSignInSuccess = { idToken ->
                viewModel.handleIntent(PresetDetailIntent.SignInWithGoogle(idToken))
            },
            onSignInFailure = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
            onDismiss = { showSignInHandler = false },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.preset?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (state.preset != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.preset_market_report)) },
                                    onClick = {
                                        showMenu = false
                                        showReportNotReadyDialog = true
                                    },
                                )
                                if (state.isOwnPreset) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.preset_market_delete),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            showDeleteDialog = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
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
        bottomBar = {
            if (state.preset != null) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // 좋아요 버튼
                        OutlinedButton(
                            onClick = { viewModel.handleIntent(PresetDetailIntent.ToggleLike) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = if (state.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.preset_market_like))
                        }

                        // 다운로드 버튼
                        if (state.isAlreadyDownloaded) {
                            // 이미 다운로드한 경우 — 비활성 회색 버튼
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(MaterialTheme.shapes.small),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                ) {}
                                Text(
                                    text = stringResource(R.string.preset_market_already_downloaded),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        } else {
                            // 다운로드 가능 — 프로그레스 fill 버튼
                            val downloadProgress = state.downloadProgress
                            val targetFraction = downloadProgress?.percentage ?: 0f
                            val animatedFraction by animateFloatAsState(
                                targetValue = targetFraction,
                                animationSpec = tween(300),
                                label = "downloadProgress",
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable {
                                        if (state.isDownloading) {
                                            showCancelDownloadDialog = true
                                        } else {
                                            viewModel.handleIntent(PresetDetailIntent.DownloadPreset)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                // 배경
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.small),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.small),
                                    ) {
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                        ) {}
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = if (state.isDownloading) animatedFraction else 1f),
                                            color = MaterialTheme.colorScheme.primary,
                                        ) {}
                                    }
                                }
                                // 텍스트/아이콘
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    when {
                                        // 서비스 기동 대기 중 (클릭 직후 ~ 서비스 첫 emit 전)
                                        state.isDownloading && downloadProgress == null -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                        // 서비스 진행 중 (fill 애니메이션 + %)
                                        state.isDownloading -> {
                                            Text(
                                                text = "${(animatedFraction * 100).toInt()}%",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                        // 대기 상태
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(R.string.preset_market_download),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.preset != null -> {
                val preset = state.preset!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val sharedKey = if (fromCard) "preset-card-thumb-${preset.id}" else "preset-list-thumb-${preset.id}"

                    // 프리뷰 이미지 HorizontalPager (shared element 대상)
                    if (preset.previewImageUrls.isNotEmpty()) {
                        val pagerState = rememberPagerState { preset.previewImageUrls.size }
                        var fullScreenImageIndex by remember { mutableStateOf<Int?>(null) }

                        // 10초 자동 스크롤
                        LaunchedEffect(pagerState.pageCount) {
                            if (pagerState.pageCount <= 1) return@LaunchedEffect
                            while (true) {
                                delay(10_000L)
                                val next = (pagerState.currentPage + 1) % pagerState.pageCount
                                pagerState.animateScrollToPage(next)
                            }
                        }

                        with(sharedTransitionScope) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = sharedKey),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                    ),
                            ) { page ->
                                AsyncImage(
                                    model = preset.previewImageUrls[page],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { fullScreenImageIndex = page },
                                )
                            }
                        } // with(sharedTransitionScope)
                        // 페이지 인디케이터
                        PagerIndicator(
                            pageCount = preset.previewImageUrls.size,
                            currentPage = pagerState.currentPage,
                            modifier = Modifier.fillMaxWidth(),
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        )

                        // 전체화면 이미지 뷰어
                        fullScreenImageIndex?.let { startIndex ->
                            FullScreenImagePager(
                                imageUrls = preset.previewImageUrls,
                                initialPage = startIndex,
                                onDismiss = { fullScreenImageIndex = null },
                            )
                        }
                    }

                    AdmobBanner(modifier = Modifier.fillMaxWidth())

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 제목, 작성자
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.preset_market_by, preset.authorDisplayName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // 설명
                        if (preset.description.isNotEmpty()) {
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        // 태그
                        if (preset.tags.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.preset_market_detail_tags),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(preset.tags) { tag ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("#$tag") },
                                    )
                                }
                            }
                        }

                        // 통계 (다운로드 / 좋아요)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    text = stringResource(R.string.preset_market_download_count, preset.downloadCount),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    text = stringResource(R.string.preset_market_like_count, preset.likeCount),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }

                        // 포함된 설정 태그
                        Text(
                            text = stringResource(R.string.preset_market_detail_includes),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (preset.hasTopLeftImage || preset.hasTopRightImage || preset.hasBottomLeftImage || preset.hasBottomRightImage) {
                                AssistChip(onClick = {}, label = { Text(stringResource(R.string.preset_market_includes_home)) })
                            }
                            if (preset.hasFeedSettings) AssistChip(onClick = {}, label = { Text(stringResource(R.string.preset_market_includes_feed)) })
                            if (preset.hasAppDrawerSettings) AssistChip(onClick = {}, label = { Text(stringResource(R.string.preset_market_includes_drawer)) })
                            if (preset.hasThemeSettings) AssistChip(onClick = {}, label = { Text(stringResource(R.string.preset_market_includes_theme)) })
                            if (preset.hasWallpaperSettings) AssistChip(onClick = {}, label = { Text(stringResource(R.string.preset_market_includes_wallpaper)) })
                        }

                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FullScreenImagePager(
    imageUrls: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pagerState = rememberPagerState(initialPage = initialPage) { imageUrls.size }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                AsyncImage(
                    model = imageUrls[page],
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onDismiss),
                )
            }
            // 페이지 인디케이터
            PagerIndicator(
                pageCount = imageUrls.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                selectedColor = Color.White,
            )
        }
    }
}
