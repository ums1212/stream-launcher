package org.comon.streamlauncher.preset_market.ui

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import org.comon.streamlauncher.preset_market.*
import org.comon.streamlauncher.preset_market.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PresetDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresetDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSignInHandler by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }

    val downloadCompleteMessage = stringResource(R.string.preset_market_download_complete)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PresetDetailSideEffect.DownloadComplete ->
                    snackbarHostState.showSnackbar(message = downloadCompleteMessage)
                is PresetDetailSideEffect.ShowError ->
                    snackbarHostState.showSnackbar(message = effect.message)
                is PresetDetailSideEffect.RequireSignIn ->
                    showSignInHandler = true
                is PresetDetailSideEffect.PresetLimitExceeded ->
                    showLimitDialog = true
            }
        }
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
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    // 프리뷰 이미지 HorizontalPager
                    if (preset.previewImageUrls.isNotEmpty()) {
                        val pagerState = rememberPagerState { preset.previewImageUrls.size }
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                        ) { page ->
                            AsyncImage(
                                model = preset.previewImageUrls[page],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }
                        // 페이지 인디케이터
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            repeat(preset.previewImageUrls.size) { idx ->
                                val color = if (pagerState.currentPage == idx) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                                Surface(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(8.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = color,
                                ) {}
                            }
                        }
                    }

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

                        Spacer(modifier = Modifier.height(8.dp))

                        // 액션 버튼
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                            Button(
                                onClick = { viewModel.handleIntent(PresetDetailIntent.DownloadPreset) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isDownloading,
                            ) {
                                if (state.isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.preset_market_download))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
