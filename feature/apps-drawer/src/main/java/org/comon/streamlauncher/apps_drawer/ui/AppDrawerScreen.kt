package org.comon.streamlauncher.apps_drawer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCell
import org.comon.streamlauncher.ui.component.AppIcon
import org.comon.streamlauncher.ui.dragdrop.LocalDragDropState
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import kotlin.math.ceil

private const val COLUMNS = 4
private const val ROWS = 6
private const val ITEMS_PER_PAGE = COLUMNS * ROWS

@Composable
fun AppDrawerScreen(
    searchQuery: String,
    filteredApps: List<AppEntity>,
    onSearch: (String) -> Unit,
    onAppClick: (AppEntity) -> Unit,
    onAppAssigned: (AppEntity, GridCell) -> Unit = { _, _ -> },
) {
    val colors = StreamLauncherTheme.colors
    val pageCount = if (filteredApps.isEmpty()) 1 else ceil(filteredApps.size / ITEMS_PER_PAGE.toFloat()).toInt()
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("앱 검색") },
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
                            contentDescription = "검색어 지우기",
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.searchBarFocused,
                cursorColor = colors.accentPrimary,
                focusedLeadingIconColor = colors.accentPrimary,
            ),
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            val startIndex = page * ITEMS_PER_PAGE
            val endIndex = minOf(startIndex + ITEMS_PER_PAGE, filteredApps.size)
            val pageApps = if (startIndex < filteredApps.size) {
                filteredApps.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            AppGridPage(
                apps = pageApps,
                onAppClick = onAppClick,
                onAppAssigned = onAppAssigned,
            )
        }

        // 페이지 인디케이터
        if (pageCount > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pageCount) { index ->
                    PageIndicatorDot(
                        pagerState = pagerState,
                        index = index,
                        color = colors.accentPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PageIndicatorDot(
    pagerState: PagerState,
    index: Int,
    color: androidx.compose.ui.graphics.Color
) {
    val isSelected = pagerState.currentPage == index
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(if (isSelected) 8.dp else 6.dp)
            .then(Modifier.padding(0.dp)),
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = if (isSelected) {
                    color
                } else {
                    color.copy(alpha = 0.3f)
                },
            )
        }
    }
}

@Composable
private fun AppGridPage(
    apps: List<AppEntity>,
    onAppClick: (AppEntity) -> Unit,
    onAppAssigned: (AppEntity, GridCell) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        for (rowIndex in 0 until ROWS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (colIndex in 0 until COLUMNS) {
                    val appIndex = rowIndex * COLUMNS + colIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (appIndex < apps.size) {
                            AppDrawerGridItem(
                                app = apps[appIndex],
                                onClick = { onAppClick(apps[appIndex]) },
                                onAppAssigned = onAppAssigned,
                            )
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
    onClick: () -> Unit,
    onAppAssigned: (AppEntity, GridCell) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val dragDropState = LocalDragDropState.current
    var itemRootOffset by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .onGloballyPositioned { coords ->
                itemRootOffset = coords.positionInRoot()
            }
            .pointerInput(app) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val rootPosition = itemRootOffset + localOffset
                        dragDropState.startDrag(app, rootPosition)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val rootPosition = itemRootOffset + change.position
                        dragDropState.updateDrag(rootPosition)
                    },
                    onDragEnd = {
                        val result = dragDropState.endDrag()
                        if (result != null) {
                            onAppAssigned(result.first, result.second)
                        }
                    },
                    onDragCancel = {
                        dragDropState.cancelDrag()
                    },
                )
            }
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(
            packageName = app.packageName,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(64.dp),
        )
    }
}
