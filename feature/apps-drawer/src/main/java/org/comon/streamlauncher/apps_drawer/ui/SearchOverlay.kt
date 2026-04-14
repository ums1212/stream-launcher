package org.comon.streamlauncher.apps_drawer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.comon.streamlauncher.apps_drawer.R
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.util.ChosungMatcher
import org.comon.streamlauncher.ui.component.AppIcon
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme

@Composable
fun SearchOverlay(
    isVisible: Boolean,
    allApps: List<AppEntity>,
    onAppClick: (AppEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        ),
        exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 250),
        ),
    ) {
        BackHandler(enabled = true) { onDismiss() }

        val colors = StreamLauncherTheme.colors
        val focusRequester = remember { FocusRequester() }
        // clearFocus()는 Android View의 requestDefaultFocus()를 트리거해 키보드가 다시 올라오는
        // bounce를 유발함. 대신 보이지 않는 더미 포커스 요소로 포커스를 옮겨 bounce 없이 해제.
        val dummyFocusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        val density = LocalDensity.current
        val imeInsets = WindowInsets.ime

        // 로컬 검색어 — ViewModel 상태와 완전히 분리
        var searchQuery by remember { mutableStateOf("") }

        // 오버레이가 닫힐 때 검색어 초기화
        LaunchedEffect(isVisible) {
            if (!isVisible) searchQuery = ""
        }

        // 로컬 필터링 — ChosungMatcher 직접 사용, ViewModel 미호출
        val overlayApps by remember {
            derivedStateOf {
                if (searchQuery.isEmpty()) emptyList()
                else allApps.filter { ChosungMatcher.matchesChosung(it.label, searchQuery) }
            }
        }

        // 키보드가 완전히 닫히면 더미로 포커스 이동 → TextField 포커스 해제
        LaunchedEffect(Unit) {
            snapshotFlow { imeInsets.getBottom(density) }
                .collect { imeBottom ->
                    if (imeBottom == 0) {
                        try { dummyFocusRequester.requestFocus() } catch (_: Exception) { }
                    }
                }
        }

        LaunchedEffect(Unit) {
            delay(300L)
            try { focusRequester.requestFocus() } catch (_: Exception) { }
        }

        // 크기 0의 더미 포커스 요소 — 키보드 없이 포커스만 받음
        Box(
            modifier = Modifier
                .size(0.dp)
                .focusRequester(dummyFocusRequester)
                .focusable(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.glassSurface.copy(alpha = 0.97f))
                .statusBarsPadding()
                .imePadding(),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.app_drawer_search)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.app_drawer_clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        try { dummyFocusRequester.requestFocus() } catch (_: Exception) { }
                    },
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.searchBarFocused,
                    cursorColor = colors.accentPrimary,
                    focusedLeadingIconColor = colors.accentPrimary,
                    focusedContainerColor = colors.glassSurface,
                    unfocusedContainerColor = colors.glassSurface,
                ),
            )

            if (searchQuery.isNotEmpty() && overlayApps.isEmpty()) {
                Text(
                    text = stringResource(R.string.app_drawer_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.glassOnSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    items(
                        items = overlayApps,
                        key = { it.packageName },
                    ) { app ->
                        SearchResultItem(
                            app = app,
                            onClick = { onAppClick(app) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    app: AppEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StreamLauncherTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            packageName = app.packageName,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.glassOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
