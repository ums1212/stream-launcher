package org.comon.streamlauncher.launcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import org.comon.streamlauncher.domain.model.ChannelProfile
import org.comon.streamlauncher.domain.model.FeedItem
import org.comon.streamlauncher.domain.model.LiveStatus
import org.comon.streamlauncher.launcher.FeedIntent
import org.comon.streamlauncher.launcher.FeedState
import org.comon.streamlauncher.ui.theme.StreamLauncherTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedScreen(
    state: FeedState,
    onIntent: (FeedIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        // 배경 이미지 레이어
        if (state.feedBackgroundImage != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(state.feedBackgroundImage)
                    .crossfade(300)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.4f,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(8.dp),
            )
        }

        // 피드 콘텐츠
        FeedContent(state = state, onIntent = onIntent)
    }
}

@Composable
private fun FeedContent(
    state: FeedState,
    onIntent: (FeedIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "피드",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { onIntent(FeedIntent.Refresh) }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "새로고침",
                    tint = StreamLauncherTheme.colors.accentPrimary,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 라이브 상태 카드
        state.liveStatus?.let { liveStatus ->
            if (liveStatus.isLive) {
                LiveStatusCard(
                    liveStatus = liveStatus,
                    onClick = { onIntent(FeedIntent.ClickLiveStatus) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 채널 프로필 카드
        state.channelProfile?.let { profile ->
            if (profile.name.isNotEmpty()) {
                ChannelProfileCard(
                    profile = profile,
                    onClick = { onIntent(FeedIntent.ClickChannelProfile) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 피드 목록
        when {
            state.isLoading && state.feedItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StreamLauncherTheme.colors.accentPrimary)
                }
            }
            state.feedItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "피드 항목이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        state.feedItems,
                        key = { "${it.javaClass.name}_${it.dateMillis}_${it.title}" },
                    ) { item ->
                        when (item) {
                            is FeedItem.NoticeItem -> NoticeItemRow(
                                item = item,
                                onClick = { onIntent(FeedIntent.ClickFeedItem(item)) },
                            )
                            is FeedItem.VideoItem -> VideoItemRow(
                                item = item,
                                onClick = { onIntent(FeedIntent.ClickFeedItem(item)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelProfileCard(
    profile: ChannelProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 원형 아바타
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(profile.avatarUrl)
                    .crossfade(300)
                    .transformations(CircleCropTransformation())
                    .build(),
                contentDescription = profile.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                // 구독자 수 AnimatedContent
                AnimatedContent(
                    targetState = profile.subscriberCount,
                    transitionSpec = {
                        slideInVertically { height -> height } togetherWith
                            slideOutVertically { height -> -height }
                    },
                    label = "subscriberCount",
                ) { count ->
                    Text(
                        text = "구독자 ${formatSubscriberCount(count)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStatusCard(
    liveStatus: LiveStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentPrimary = StreamLauncherTheme.colors.accentPrimary
    val infiniteTransition = rememberInfiniteTransition(label = "liveBreathing")
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathAlpha",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // LIVE 배지 (Breathing 네온 글로우)
            Box(
                modifier = Modifier
                    .drawBehind {
                        drawCircle(
                            color = accentPrimary.copy(alpha = 0.15f * breathAlpha),
                            radius = size.minDimension * 0.9f,
                        )
                        drawCircle(
                            color = accentPrimary.copy(alpha = 0.30f * breathAlpha),
                            radius = size.minDimension * 0.65f,
                        )
                        drawCircle(
                            color = accentPrimary.copy(alpha = 0.80f * breathAlpha),
                            radius = size.minDimension * 0.42f,
                        )
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "라이브",
                    tint = accentPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentPrimary,
                )
                Text(
                    text = liveStatus.title.ifEmpty { "방송 중" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = "${liveStatus.viewerCount}명",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoticeItemRow(
    item: FeedItem.NoticeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = item.source,
                    fontSize = 11.sp,
                    color = StreamLauncherTheme.colors.accentPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDate(item.dateMillis),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VideoItemRow(
    item: FeedItem.VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(item.dateMillis),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    if (millis == 0L) return ""
    return try {
        SimpleDateFormat("MM.dd", Locale.KOREAN).format(Date(millis))
    } catch (_: Exception) {
        ""
    }
}

private fun formatSubscriberCount(count: Long): String {
    return when {
        count >= 10_000 -> "${count / 10_000}만명"
        count >= 1_000 -> "${count / 1_000}천명"
        else -> "${count}명"
    }
}
