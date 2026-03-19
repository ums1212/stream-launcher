package org.comon.streamlauncher.preset_market.ui.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.preset_market.R

/**
 * LazyRow에 배치되는 세로형 고정 너비 카드 (landscape 전용).
 *
 * @param fromCard true면 sharedBounds key = "preset-card-thumb-{id}" (TopPreset 기원),
 *                 false면 "preset-list-thumb-{id}" (Recent 기원)
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MarketPresetLandscapeCard(
    preset: MarketPreset,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    fromCard: Boolean = true,
) {
    val sharedKey = if (fromCard) {
        "preset-card-thumb-${preset.id}"
    } else {
        "preset-list-thumb-${preset.id}"
    }

    with(sharedTransitionScope) {
        Card(
            modifier = modifier
                .width(200.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column {
                // 썸네일
                AsyncImage(
                    model = preset.thumbnailUrl.ifEmpty { null },
                    contentDescription = preset.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = sharedKey),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                )

                // 정보 영역
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 순위 + 이름
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (rank != null) {
                            Text(
                                text = "#$rank",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // 작성자
                    Text(
                        text = stringResource(R.string.preset_market_by, preset.authorDisplayName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // 태그
                    if (preset.tags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            preset.tags.take(2).forEach { tag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        text = "#$tag",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }

                    // 통계
                    PresetStatsRow(
                        downloadCount = preset.downloadCount,
                        likeCount = preset.likeCount,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconSize = 12.dp,
                        outerSpacing = 8.dp,
                        innerSpacing = 2.dp,
                    )
                }
            }
        }
    }
}
