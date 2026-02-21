package org.comon.streamlauncher.launcher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.domain.model.GridCellImage
import org.comon.streamlauncher.launcher.HomeIntent
import org.comon.streamlauncher.launcher.HomeState
import org.comon.streamlauncher.domain.model.GridCell

@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedCell = state.expandedCell

    val topRowTargetWeight = when (expandedCell) {
        GridCell.TOP_LEFT, GridCell.TOP_RIGHT -> 0.8f
        GridCell.BOTTOM_LEFT, GridCell.BOTTOM_RIGHT -> 0.2f
        null -> 0.5f
    }
    val topRowWeight by animateFloatAsState(
        targetValue = topRowTargetWeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "topRowWeight",
    )
    val bottomRowWeight = 1f - topRowWeight

    val leftColTargetWeight = when (expandedCell) {
        GridCell.TOP_LEFT, GridCell.BOTTOM_LEFT -> 0.8f
        GridCell.TOP_RIGHT, GridCell.BOTTOM_RIGHT -> 0.2f
        null -> 0.5f
    }
    val leftColWeight by animateFloatAsState(
        targetValue = leftColTargetWeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "leftColWeight",
    )
    val rightColWeight = 1f - leftColWeight

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Row(
            modifier = Modifier
                .weight(topRowWeight)
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        ) {
            GridCellContent(
                cell = GridCell.TOP_LEFT,
                weight = leftColWeight,
                isExpanded = expandedCell == GridCell.TOP_LEFT,
                apps = state.appsInCells[GridCell.TOP_LEFT] ?: emptyList(),
                gridCellImage = state.gridCellImages[GridCell.TOP_LEFT],
                onIntent = onIntent,
                modifier = Modifier.padding(end = 4.dp),
            )
            GridCellContent(
                cell = GridCell.TOP_RIGHT,
                weight = rightColWeight,
                isExpanded = expandedCell == GridCell.TOP_RIGHT,
                apps = state.appsInCells[GridCell.TOP_RIGHT] ?: emptyList(),
                gridCellImage = state.gridCellImages[GridCell.TOP_RIGHT],
                onIntent = onIntent,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .weight(bottomRowWeight)
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            GridCellContent(
                cell = GridCell.BOTTOM_LEFT,
                weight = leftColWeight,
                isExpanded = expandedCell == GridCell.BOTTOM_LEFT,
                apps = state.appsInCells[GridCell.BOTTOM_LEFT] ?: emptyList(),
                gridCellImage = state.gridCellImages[GridCell.BOTTOM_LEFT],
                onIntent = onIntent,
                modifier = Modifier.padding(end = 4.dp),
            )
            GridCellContent(
                cell = GridCell.BOTTOM_RIGHT,
                weight = rightColWeight,
                isExpanded = expandedCell == GridCell.BOTTOM_RIGHT,
                apps = state.appsInCells[GridCell.BOTTOM_RIGHT] ?: emptyList(),
                gridCellImage = state.gridCellImages[GridCell.BOTTOM_RIGHT],
                onIntent = onIntent,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun RowScope.GridCellContent(
    cell: GridCell,
    weight: Float,
    isExpanded: Boolean,
    apps: List<AppEntity>,
    gridCellImage: GridCellImage?,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = ((weight - 0.6f) / 0.2f).coerceIn(0f, 1f)
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)

    // 확장 상태: accentPrimary 2dp / 축소: gridBorder 1dp
    // feature 모듈은 app 모듈 의존 불가 → MaterialTheme.colorScheme 색상 직접 사용
    val borderColor = if (isExpanded) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isExpanded) 2.dp else 1.dp

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onIntent(HomeIntent.ClickGrid(cell))
        },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .weight(weight)
            .fillMaxSize()
            .border(width = borderWidth, color = borderColor, shape = shape),
    ) {
        if (isExpanded) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 확장 이미지 배경
                val expandedUri = gridCellImage?.expandedImageUri
                if (expandedUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(expandedUri)
                            .crossfade(300)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // 앱 목록 가독성을 위한 반투명 오버레이
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(contentAlpha)
                        .padding(8.dp),
                ) {
                    items(apps) { app ->
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onIntent(HomeIntent.ClickApp(app))
                                }
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                // 축소 이미지 배경
                val idleUri = gridCellImage?.idleImageUri
                if (idleUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(idleUri)
                            .crossfade(300)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    text = cell.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (idleUri != null) Color.White else Color.Unspecified,
                )
            }
        }
    }
}
