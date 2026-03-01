package org.comon.streamlauncher.preset_market.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import org.comon.streamlauncher.preset_market.*
import org.comon.streamlauncher.preset_market.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketSearchResultScreen(
    initialQuery: String,
    onNavigateToDetail: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MarketSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchResults = viewModel.searchResultsPaging.collectAsLazyPagingItems()
    var searchInput by remember { mutableStateOf(initialQuery) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MarketSearchSideEffect.NavigateToDetail -> onNavigateToDetail(effect.presetId)
            }
        }
    }

    // 초기 쿼리 적용
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && state.query != initialQuery) {
            viewModel.handleIntent(MarketSearchIntent.Search(initialQuery))
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.preset_market_search_hint)) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (searchInput.isNotBlank()) {
                                        viewModel.handleIntent(MarketSearchIntent.Search(searchInput))
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                        },
                        singleLine = true,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                searchResults.loadState.refresh is LoadState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                searchResults.itemCount == 0 &&
                    searchResults.loadState.refresh is LoadState.NotLoading -> {
                    Text(
                        text = stringResource(R.string.preset_market_no_results),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.preset_market_search_results),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(
                            count = searchResults.itemCount,
                            key = searchResults.itemKey { it.id },
                        ) { index ->
                            val preset = searchResults[index]
                            if (preset != null) {
                                MarketPresetListItem(
                                    preset = preset,
                                    onClick = {
                                        viewModel.handleIntent(MarketSearchIntent.ClickPreset(preset.id))
                                    },
                                )
                            }
                        }
                        if (searchResults.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
