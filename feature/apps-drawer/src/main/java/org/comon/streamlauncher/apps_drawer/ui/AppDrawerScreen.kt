package org.comon.streamlauncher.apps_drawer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.comon.streamlauncher.apps_drawer.R
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.ui.component.AppIcon
import org.comon.streamlauncher.ui.component.PagerIndicator
import org.comon.streamlauncher.ui.component.calculateFixedAppGridMetrics
import org.comon.streamlauncher.ui.dragdrop.LocalDragDropState
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import kotlin.math.ceil

private const val CONTEXT_MENU_DELAY_MS = 500L

@Composable
fun AppDrawerScreen(
    searchQuery: String,
    filteredApps: List<AppEntity>,
    onSearch: (String) -> Unit,
    onAppClick: (AppEntity) -> Unit,
    onAppAssigned: (AppEntity, GridCell) -> Unit = { _, _ -> },
    onShowAppInfo: (AppEntity) -> Unit = {},
    onRequestUninstall: (AppEntity) -> Unit = {},
    columns: Int = 4,
    rows: Int = 6,
    iconSizeRatio: Float = 1.0f,
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as Activity).window
        val prevMode = window.attributes.softInputMode
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose { window.setSoftInputMode(prevMode) }
    }

    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime

    LaunchedEffect(Unit) {
        snapshotFlow { imeInsets.getBottom(density) }
            .collect { imeBottom ->
                if (imeBottom == 0) focusManager.clearFocus()
            }
    }

    var searchBarHeightPx by remember { mutableIntStateOf(0) }
    val searchBarHeight = with(density) { searchBarHeightPx.toDp() }

    val colors = StreamLauncherTheme.colors
    val appsPerPage = columns * rows
    val pageCount = if (filteredApps.isEmpty()) 1 else ceil(filteredApps.size / appsPerPage.toFloat()).toInt()
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                val startIndex = page * appsPerPage
                val endIndex = minOf(startIndex + appsPerPage, filteredApps.size)
                val pageApps = if (startIndex < filteredApps.size) {
                    filteredApps.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }

                AppGridPage(
                    apps = pageApps,
                    columns = columns,
                    rows = rows,
                    iconSizeRatio = iconSizeRatio,
                    onAppClick = onAppClick,
                    onAppAssigned = onAppAssigned,
                    onShowAppInfo = onShowAppInfo,
                    onRequestUninstall = onRequestUninstall,
                )
            }

            if (pageCount > 1) {
                PagerIndicator(
                    pageCount = pageCount,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    selectedColor = colors.accentPrimary,
                    unselectedColor = colors.accentPrimary.copy(alpha = 0.3f),
                    dotSize = 8.dp,
                    smallDotSize = 6.dp,
                )
            }

            // 검색창이 마지막 행과 겹치지 않도록 검색창 높이만큼 공간 확보
            Spacer(modifier = Modifier.height(searchBarHeight))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onGloballyPositioned { searchBarHeightPx = it.size.height }
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text(stringResource(R.string.app_drawer_search)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearch("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.app_drawer_clear_search),
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.searchBarFocused,
                cursorColor = colors.accentPrimary,
                focusedLeadingIconColor = colors.accentPrimary,
                focusedContainerColor = colors.glassSurface,
                unfocusedContainerColor = colors.glassSurface,
            ),
        )
    }
}

@Composable
private fun AppGridPage(
    apps: List<AppEntity>,
    columns: Int,
    rows: Int,
    iconSizeRatio: Float,
    onAppClick: (AppEntity) -> Unit,
    onAppAssigned: (AppEntity, GridCell) -> Unit,
    onShowAppInfo: (AppEntity) -> Unit,
    onRequestUninstall: (AppEntity) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val gridMetrics = calculateFixedAppGridMetrics(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            columns = columns,
            rows = rows,
            iconSizeRatio = iconSizeRatio,
        )
        val fontSize = if (maxWidth < 360.dp) 10.sp else 11.sp

        Column(modifier = Modifier.fillMaxSize()) {
            for (rowIndex in 0 until gridMetrics.rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (colIndex in 0 until gridMetrics.columns) {
                        val appIndex = rowIndex * gridMetrics.columns + colIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (appIndex < apps.size) {
                                AppDrawerGridItem(
                                    app = apps[appIndex],
                                    iconSize = gridMetrics.iconSize,
                                    textWidth = gridMetrics.textWidth,
                                    fontSize = fontSize,
                                    onClick = { onAppClick(apps[appIndex]) },
                                    onAppAssigned = onAppAssigned,
                                    onShowAppInfo = onShowAppInfo,
                                    onRequestUninstall = onRequestUninstall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDrawerGridItem(
    app: AppEntity,
    iconSize: Dp,
    textWidth: Dp,
    fontSize: TextUnit,
    onClick: () -> Unit,
    onAppAssigned: (AppEntity, GridCell) -> Unit,
    onShowAppInfo: (AppEntity) -> Unit,
    onRequestUninstall: (AppEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val dragDropState = LocalDragDropState.current

    var itemRootOffset by remember { mutableStateOf(Offset.Zero) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showRedTint by remember { mutableStateOf(false) }

    if (showContextMenu) {
        AppContextMenuDialog(
            app = app,
            onDismiss = { showContextMenu = false },
            onOpenAppInfo = {
                showContextMenu = false
                onShowAppInfo(app)
            },
            onRequestUninstall = {
                showContextMenu = false
                onRequestUninstall(app)
            },
        )
    }

    Column(
        modifier = modifier
            .onGloballyPositioned { coords ->
                itemRootOffset = coords.positionInRoot()
            }
            .pointerInput(app) {
                val pointerScope = this
                val longPressMs = viewConfiguration.longPressTimeoutMillis
                val touchSlopPx = viewConfiguration.touchSlop
                val dragThresholdPx = 10.dp.toPx()

                val ctx = currentCoroutineContext()
                while (ctx.isActive) {
                    try {
                        coroutineScope {
                            // coroutineScope 레퍼런스 보관:
                            // awaitPointerEventScope 내부에서 타이머 job을 launch하기 위해 필요
                            val gestureScope = this
                            var isDragStarted = false
                            var totalDragDistance = 0f

                            try {
                                pointerScope.awaitPointerEventScope {
                                    // ① 반드시 손가락이 눌린 이후에만 타이머를 시작한다.
                                    //    이전 코드는 awaitFirstDown 전에 launch해서
                                    //    제스처가 끝난 뒤 다음 이터레이션에서도 2000ms 타이머가
                                    //    손가락 없이 계속 돌아 showRedTint=true가 발화되는 버그가 있었다.
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val pendingStartOffset = itemRootOffset + down.position
                                    var isReadyForDrag = false

                                    val longPressJob = gestureScope.launch {
                                        delay(longPressMs)
                                        isReadyForDrag = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    val contextMenuJob = gestureScope.launch {
                                        delay(CONTEXT_MENU_DELAY_MS)
                                        showRedTint = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }

                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.find { it.id == down.id }
                                                ?: break
                                            if (!change.pressed) break

                                            val delta = change.position - change.previousPosition
                                            totalDragDistance += delta.getDistance()

                                            // 장누름 전에 touchSlop 초과 이동 → Pager 스크롤 허용
                                            if (!isReadyForDrag && totalDragDistance > touchSlopPx) break

                                            // 장누름 이후 이벤트 소비 (Pager 스크롤 차단)
                                            if (isReadyForDrag) change.consume()

                                            // 드래그 시작: 500ms 후 + 10dp 이상 이동
                                            if (isReadyForDrag && !isDragStarted && totalDragDistance >= dragThresholdPx) {
                                                isDragStarted = true
                                                showRedTint = false
                                                contextMenuJob.cancel()
                                                dragDropState.startDrag(app, pendingStartOffset)
                                            }

                                            if (isDragStarted) {
                                                dragDropState.updateDrag(itemRootOffset + change.position)
                                            }
                                        }
                                    } finally {
                                        longPressJob.cancel()
                                        contextMenuJob.cancel()
                                    }
                                }
                            } catch (e: CancellationException) {
                                showRedTint = false
                                throw e
                            }

                            val wasRedTintActive = showRedTint
                            showRedTint = false

                            when {
                                isDragStarted -> {
                                    val result = dragDropState.endDrag()
                                    result?.let { onAppAssigned(it.app, it.targetCell) }
                                }
                                wasRedTintActive -> showContextMenu = true
                                totalDragDistance < touchSlopPx -> {
                                    // 빠른 탭 → 앱 실행
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onClick()
                                }
                                // else: 장누름 전 스와이프 → Pager가 처리
                            }
                        }
                    } catch (e: CancellationException) {
                        showRedTint = false
                        if (!ctx.isActive) throw e
                    }
                }
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(iconSize)) {
            AppIcon(
                packageName = app.packageName,
                modifier = Modifier.fillMaxSize(),
            )
            if (showRedTint) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red.copy(alpha = 0.40f)),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = fontSize),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(textWidth),
        )
    }
}

@Composable
private fun AppContextMenuDialog(
    app: AppEntity,
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestUninstall: () -> Unit,
) {
    val colors = StreamLauncherTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = colors.accentPrimary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                )
                .background(colors.glassSurface),
        ) {
            // 상단: 앱 아이콘 + 앱 이름
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AppIcon(
                    packageName = app.packageName,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.glassOnSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            HorizontalDivider(color = colors.accentPrimary.copy(alpha = 0.2f))

            // 하단: 삭제 | 정보
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                TextButton(
                    onClick = onRequestUninstall,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(bottomStart = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_drawer_menu_uninstall),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                VerticalDivider(color = colors.accentPrimary.copy(alpha = 0.2f))
                TextButton(
                    onClick = onOpenAppInfo,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(bottomEnd = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_drawer_menu_app_info),
                        color = colors.accentPrimary,
                    )
                }
            }
        }
    }
}
