package org.comon.streamlauncher.preset_market.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.preset_market.ui.component.PresetStatsRow

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MarketPresetCard(
    preset: MarketPreset,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    rank: Int? = null,
) {
    with(sharedTransitionScope) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key = "preset-card-thumb-${preset.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        // 썸네일 배경
        AsyncImage(
            model = preset.thumbnailUrl.ifEmpty { null },
            contentDescription = preset.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // 하단 그라디언트 오버레이
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    )
                ),
        )

        // 정보 오버레이
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (rank != null) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            PresetStatsRow(
                downloadCount = preset.downloadCount,
                likeCount = preset.likeCount,
                color = Color.White,
                iconSize = 14.dp,
                outerSpacing = 12.dp,
                innerSpacing = 4.dp,
            )
            if (preset.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    preset.tags.take(3).forEach { tag ->
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "#$tag",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
    } // with(sharedTransitionScope)
}
