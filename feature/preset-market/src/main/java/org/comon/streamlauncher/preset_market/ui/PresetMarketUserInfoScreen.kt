package org.comon.streamlauncher.preset_market.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.comon.streamlauncher.domain.model.preset.MarketUser
import org.comon.streamlauncher.preset_market.PresetMarketUserInfoIntent
import org.comon.streamlauncher.preset_market.PresetMarketUserInfoSideEffect
import org.comon.streamlauncher.preset_market.PresetMarketUserInfoViewModel
import org.comon.streamlauncher.preset_market.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetMarketUserInfoScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresetMarketUserInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PresetMarketUserInfoSideEffect.NavigateToDetail -> onNavigateToDetail(effect.presetId)
                is PresetMarketUserInfoSideEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is PresetMarketUserInfoSideEffect.SignedOut -> onBack()
            }
        }
    }

    val containerSize = LocalWindowInfo.current.containerSize
    val isLandscape = containerSize.width > containerSize.height

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preset_market_user_info_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        if (isLandscape) {
            LandscapeLayout(
                user = state.user,
                presets = state.presets,
                isLoading = state.isLoading,
                onSignOut = { viewModel.handleIntent(PresetMarketUserInfoIntent.SignOut) },
                onClickPreset = { viewModel.handleIntent(PresetMarketUserInfoIntent.ClickPreset(it)) },
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            )
        } else {
            PortraitLayout(
                user = state.user,
                presets = state.presets,
                isLoading = state.isLoading,
                onSignOut = { viewModel.handleIntent(PresetMarketUserInfoIntent.SignOut) },
                onClickPreset = { viewModel.handleIntent(PresetMarketUserInfoIntent.ClickPreset(it)) },
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    user: MarketUser?,
    presets: List<org.comon.streamlauncher.domain.model.preset.MarketPreset>,
    isLoading: Boolean,
    onSignOut: () -> Unit,
    onClickPreset: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            UserProfileCard(
                user = user,
                onSignOut = onSignOut,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.preset_market_my_presets),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (presets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.preset_market_no_presets),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(presets, key = { it.id }) { preset ->
                SimplePresetListItem(
                    preset = preset,
                    onClick = { onClickPreset(preset.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LandscapeLayout(
    user: MarketUser?,
    presets: List<org.comon.streamlauncher.domain.model.preset.MarketPreset>,
    isLoading: Boolean,
    onSignOut: () -> Unit,
    onClickPreset: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        UserProfileCard(
            user = user,
            onSignOut = onSignOut,
            modifier = Modifier
                .width(240.dp)
                .padding(16.dp),
        )
        VerticalDivider()
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.preset_market_my_presets),
                    style = MaterialTheme.typography.titleMedium,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (presets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.preset_market_no_presets),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(presets, key = { it.id }) { preset ->
                    SimplePresetListItem(
                        preset = preset,
                        onClick = { onClickPreset(preset.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun UserProfileCard(
    user: MarketUser?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (user?.photoUrl != null) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = user?.displayName ?: "",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        user?.email?.let { email ->
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onSignOut) {
            Text(stringResource(R.string.preset_market_logout))
        }
    }
}

@Composable
private fun SimplePresetListItem(
    preset: org.comon.streamlauncher.domain.model.preset.MarketPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = preset.thumbnailUrl.ifEmpty { null },
                contentDescription = preset.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "↓ ${preset.downloadCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "♥ ${preset.likeCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
