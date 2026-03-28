package org.comon.streamlauncher.settings.ui

import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import java.io.File
import org.comon.streamlauncher.domain.model.LiveWallpaper
import org.comon.streamlauncher.settings.R
import org.comon.streamlauncher.settings.SettingsIntent
import org.comon.streamlauncher.settings.SettingsState
import androidx.core.net.toUri

@Composable
internal fun LiveWallpaperSettingsContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showNameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<LiveWallpaper?>(null) }

    // 동영상/GIF 파일 선택 런처
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            onIntent(SettingsIntent.LoadLiveWallpaperFile(uri.toString()))
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. 상단 버튼 Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { fileLauncher.launch(arrayOf("video/*", "image/gif")) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.live_wallpaper_load))
            }
            Button(
                onClick = { showNameDialog = true },
                enabled = state.selectedLiveWallpaperUri != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.live_wallpaper_create))
            }
        }

        // 2. 저장된 라이브 배경화면 목록
        if (state.liveWallpapers.isEmpty()) {
            Text(
                text = stringResource(R.string.live_wallpaper_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(state.liveWallpapers, key = { it.id }) { lw ->
                    LiveWallpaperThumbnailItem(
                        liveWallpaper = lw,
                        isSelected = lw.id == state.selectedLiveWallpaperId,
                        onClick = { onIntent(SettingsIntent.SelectLiveWallpaper(lw.id, lw.fileUri)) },
                        onLongClick = { showDeleteDialog = lw },
                    )
                }
            }
        }

        // 3. 미리보기 (중앙)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            val previewUri = state.selectedLiveWallpaperUri
            if (previewUri != null) {
                VideoPreview(uri = previewUri)
            } else {
                Text(
                    text = stringResource(R.string.live_wallpaper_empty),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 4. 하단 버튼들
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.selectedLiveWallpaperId != null) {
                TextButton(
                    onClick = { onIntent(SettingsIntent.ClearActiveLiveWallpaper) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.live_wallpaper_clear))
                }
            }
            Button(
                onClick = {
                    val id = state.selectedLiveWallpaperId ?: return@Button
                    val uri = state.selectedLiveWallpaperUri ?: return@Button
                    onIntent(SettingsIntent.SetActiveLiveWallpaper(id, uri))
                },
                enabled = state.selectedLiveWallpaperId != null && state.selectedLiveWallpaperUri != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.live_wallpaper_set_as_bg))
            }
        }
    }

    // 이름 입력 다이얼로그
    if (showNameDialog) {
        var nameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.live_wallpaper_name_label)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text(stringResource(R.string.live_wallpaper_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNameDialog = false
                        onIntent(SettingsIntent.CreateLiveWallpaper(
                            nameInput.ifEmpty { "라이브 배경화면 ${System.currentTimeMillis() % 1000}" }
                        ))
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 삭제 확인 다이얼로그
    showDeleteDialog?.let { lw ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(lw.name) },
            text = { Text(stringResource(R.string.live_wallpaper_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = null
                    onIntent(SettingsIntent.DeleteLiveWallpaper(lw.id))
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun LiveWallpaperThumbnailItem(
    liveWallpaper: LiveWallpaper,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(80.dp),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
                .then(
                    if (isSelected) Modifier.border(2.dp, Color.Cyan, RoundedCornerShape(8.dp))
                    else Modifier
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            if (liveWallpaper.thumbnailUri != null) {
                AsyncImage(
                    model = liveWallpaper.thumbnailUri,
                    contentDescription = liveWallpaper.name,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = liveWallpaper.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VideoPreview(
    uri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isGif = remember(uri) {
        if (uri.startsWith("content://")) {
            context.contentResolver.getType(uri.toUri()) == "image/gif"
        } else {
            uri.endsWith(".gif", ignoreCase = true)
        }
    }
    if (isGif) {
        GifPreview(uri = uri, modifier = modifier)
    } else {
        ExoPlayerPreview(uri = uri, modifier = modifier)
    }
}

@Composable
private fun GifPreview(
    uri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    AndroidView(
        factory = {
            ImageView(it).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { imageView ->
            val source = if (uri.startsWith("content://")) {
                ImageDecoder.createSource(context.contentResolver, uri.toUri())
            } else {
                ImageDecoder.createSource(File(uri))
            }
            val drawable = ImageDecoder.decodeDrawable(source)
            imageView.setImageDrawable(drawable)
            (drawable as? AnimatedImageDrawable)?.start()
        },
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun ExoPlayerPreview(
    uri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            play()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = {
            androidx.media3.ui.PlayerView(it).apply {
                useController = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { playerView ->
            // uri 변경 시 remember(uri)가 새 player를 생성하므로 update에서 반영해야 함.
            // factory는 최초 1회만 실행되어 update 없이는 PlayerView가 이전 player를 참조.
            playerView.player = player
        },
        modifier = modifier.fillMaxSize(),
    )
}
