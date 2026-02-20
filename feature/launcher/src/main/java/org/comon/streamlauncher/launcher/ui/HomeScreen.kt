package org.comon.streamlauncher.launcher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import org.comon.streamlauncher.domain.model.AppEntity
import org.comon.streamlauncher.launcher.HomeIntent
import org.comon.streamlauncher.launcher.HomeState
import org.comon.streamlauncher.launcher.model.GridCell

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
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
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
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
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
                onIntent = onIntent,
                modifier = Modifier.padding(end = 4.dp),
            )
            GridCellContent(
                cell = GridCell.TOP_RIGHT,
                weight = rightColWeight,
                isExpanded = expandedCell == GridCell.TOP_RIGHT,
                apps = state.appsInCells[GridCell.TOP_RIGHT] ?: emptyList(),
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
                onIntent = onIntent,
                modifier = Modifier.padding(end = 4.dp),
            )
            GridCellContent(
                cell = GridCell.BOTTOM_RIGHT,
                weight = rightColWeight,
                isExpanded = expandedCell == GridCell.BOTTOM_RIGHT,
                apps = state.appsInCells[GridCell.BOTTOM_RIGHT] ?: emptyList(),
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
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = ((weight - 0.6f) / 0.2f).coerceIn(0f, 1f)
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onIntent(HomeIntent.ClickGrid(cell))
        },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .weight(weight)
            .fillMaxSize(),
    ) {
        if (isExpanded) {
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
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = cell.name,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
