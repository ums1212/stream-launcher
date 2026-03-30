package org.comon.streamlauncher.settings.ui

import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.view.TextureView
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import java.io.File
import org.comon.streamlauncher.domain.model.LiveWallpaper
import org.comon.streamlauncher.domain.model.WallpaperOrientation
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
    // 시스템 배경화면 피커에서 돌아올 때 실제 적용 여부를 재확인
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onIntent(SettingsIntent.CheckActiveWallpaper)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val context = LocalContext.current
    val containerSize = LocalWindowInfo.current.containerSize
    val screenWidthPx = containerSize.width
    val screenHeightPx = containerSize.height
    var showNameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<LiveWallpaper?>(null) }

    val isLandscapeTab = state.selectedOrientationTab == WallpaperOrientation.LANDSCAPE
    // 시스템 서비스가 실제로 활성 상태일 때만 세로 배경화면 ID를 유효한 것으로 간주
    val isLandscapeUnlocked = state.activePortraitWallpaperId != null && state.isLiveWallpaperServiceActive
    val currentUri = if (isLandscapeTab) state.selectedLiveWallpaperLandscapeUri else state.selectedLiveWallpaperUri
    val currentId = if (isLandscapeTab) state.selectedLiveWallpaperLandscapeId else state.selectedLiveWallpaperId
    // 실제로 시스템에 적용된 배경화면 ID (서비스 비활성 시 null 취급)
    val activeId = if (state.isLiveWallpaperServiceActive) {
        if (isLandscapeTab) state.activeLandscapeWallpaperId else state.activePortraitWallpaperId
    } else null

    // 동영상/GIF/정적 이미지 파일 선택 런처
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
        // 1. 세로/가로 탭
        PrimaryTabRow(
            selectedTabIndex = if (isLandscapeTab) 1 else 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Tab(
                selected = !isLandscapeTab,
                onClick = { onIntent(SettingsIntent.SwitchOrientationTab(WallpaperOrientation.PORTRAIT)) },
                text = { Text(stringResource(R.string.wallpaper_orientation_portrait)) },
            )
            Tab(
                selected = isLandscapeTab,
                enabled = isLandscapeUnlocked,
                onClick = {
                    if (isLandscapeUnlocked) {
                        onIntent(SettingsIntent.SwitchOrientationTab(WallpaperOrientation.LANDSCAPE))
                    }
                },
                text = {
                    Text(
                        text = stringResource(R.string.wallpaper_orientation_landscape),
                        color = if (isLandscapeUnlocked) Color.Unspecified
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                },
            )
        }

        // 탭 아래 안내 문구
        when {
            !isLandscapeUnlocked -> Text(
                text = stringResource(R.string.wallpaper_landscape_locked_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            isLandscapeTab -> Text(
                text = stringResource(R.string.wallpaper_landscape_fallback_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 단말기 해상도 안내
        Text(
            text = "현재 귀하의 단말기 해상도는 $screenWidthPx × $screenHeightPx 입니다",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // 2. 상단 버튼 Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { fileLauncher.launch(arrayOf("video/*", "image/*")) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.live_wallpaper_load))
            }
            Button(
                onClick = { showNameDialog = true },
                enabled = currentUri != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.live_wallpaper_create))
            }
        }

        // 3. 저장된 라이브 배경화면 목록
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
                        isSelected = lw.id == currentId,
                        onClick = { onIntent(SettingsIntent.SelectLiveWallpaper(lw.id, lw.fileUri)) },
                        onLongClick = { showDeleteDialog = lw },
                    )
                }
            }
        }

        // 4. 미리보기 (중앙)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (currentUri != null) {
                VideoPreview(uri = currentUri)
            } else {
                Text(
                    text = stringResource(R.string.live_wallpaper_empty),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 5. 하단 버튼들
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (activeId != null) {
                TextButton(
                    onClick = { onIntent(SettingsIntent.ClearActiveLiveWallpaper(state.selectedOrientationTab)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.live_wallpaper_clear))
                }
            }
            Button(
                onClick = {
                    val id = currentId ?: return@Button
                    val uri = currentUri ?: return@Button
                    onIntent(SettingsIntent.SetActiveLiveWallpaper(id, uri, state.selectedOrientationTab))
                },
                enabled = currentId != null && currentUri != null,
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
    val isStaticImage = remember(uri) {
        if (uri.startsWith("content://")) {
            val mime = context.contentResolver.getType(uri.toUri()) ?: ""
            mime.startsWith("image/") && mime != "image/gif"
        } else {
            val lower = uri.lowercase()
            lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".png") || lower.endsWith(".webp")
        }
    }
    when {
        isGif -> GifPreview(uri = uri, modifier = modifier)
        isStaticImage -> AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
        )
        else -> ExoPlayerPreview(uri = uri, modifier = modifier)
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
    var aspectRatio by remember(uri) { mutableStateOf<Float?>(null) }

    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            play()
        }
    }

    // 리스너 등록과 릴리즈를 하나의 DisposableEffect에서 관리
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspectRatio = videoSize.width.toFloat() / videoSize.height * videoSize.pixelWidthHeightRatio
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // SurfaceView는 AndroidView.update가 Compose measure 단계에서 실행될 때
    // 아직 layout이 완료되지 않아 frame(width/height)이 0이므로
    // SurfaceView.updateSurface()가 "has no frame"으로 실패한다.
    // TextureView는 surface가 준비되지 않은 상태에서 setVideoTextureView()를 호출해도
    // 내부적으로 SurfaceTextureListener를 등록해 준비되는 즉시 자동으로 연결된다.
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { textureView ->
            player.setVideoTextureView(textureView)
        },
        modifier = modifier.then(
            // 비율을 알기 전까지는 fillMaxSize, 이후 원본 비율 유지
            if (aspectRatio != null) Modifier.aspectRatio(aspectRatio!!) else Modifier.fillMaxSize()
        ),
    )
}
