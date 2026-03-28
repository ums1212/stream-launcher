package org.comon.streamlauncher.service

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.FileObserver
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.core.graphics.withTranslation
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 동영상/GIF 파일을 시스템 라이브 배경화면으로 재생하는 WallpaperService.
 *
 * - GIF: AnimatedImageDrawable + Choreographer → SurfaceHolder Canvas 직접 렌더링
 * - 동영상(mp4/webm): ExoPlayer → setVideoSurface()
 *
 * android:process=":wallpaper" 로 런처와 완전히 분리된 별도 프로세스에서 실행.
 * URI는 filesDir/live_wallpaper_uri.txt 파일을 통해 전달받으며,
 * FileObserver로 파일 변경을 감지해 실시간 반영한다.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VideoLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var player: ExoPlayer? = null
        private var gifDrawable: AnimatedImageDrawable? = null
        private var frameCallback: Choreographer.FrameCallback? = null
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private var currentUri: String? = null
        private var surfaceHolder: SurfaceHolder? = null

        private val uriFile get() = File(filesDir, "live_wallpaper_uri.txt")

        /**
         * filesDir를 감시해 live_wallpaper_uri.txt 변경 시 새 URI를 적용한다.
         * onEvent는 inotify 스레드에서 호출되므로 scope.launch로 Main으로 전환한다.
         */
        @Suppress("DEPRECATION") // API 29 미만 호환 — minSdk 28이므로 String 경로 생성자 사용
        private val fileObserver = object : FileObserver(filesDir.absolutePath, CLOSE_WRITE) {
            override fun onEvent(event: Int, path: String?) {
                if (path != "live_wallpaper_uri.txt") return
                val newUri = runCatching { uriFile.readText().trim() }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: return
                if (newUri == currentUri) return
                scope.launch {
                    currentUri = newUri
                    surfaceHolder?.let { applyUri(it, newUri) }
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceHolder = holder
            fileObserver.startWatching()
            loadAndApplyUri(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                player?.play()
                if (gifDrawable?.isRunning == false) gifDrawable?.start()
            } else {
                player?.pause()
                gifDrawable?.stop()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            stopCurrent()
            fileObserver.stopWatching()
            surfaceHolder = null
        }

        override fun onDestroy() {
            super.onDestroy()
            stopCurrent()
            fileObserver.stopWatching()
            surfaceHolder = null
            scope.cancel()
        }

        private fun loadAndApplyUri(holder: SurfaceHolder) {
            scope.launch {
                val uri = withContext(Dispatchers.IO) {
                    runCatching { uriFile.readText().trim() }.getOrNull()?.takeIf { it.isNotBlank() }
                } ?: return@launch
                currentUri = uri
                applyUri(holder, uri)
            }
        }

        private fun stopCurrent() {
            player?.release()
            player = null

            frameCallback?.let { Choreographer.getInstance().removeFrameCallback(it) }
            frameCallback = null
            gifDrawable?.stop()
            gifDrawable = null
        }

        private fun applyUri(holder: SurfaceHolder, uri: String) {
            stopCurrent()
            if (uri.endsWith(".gif", ignoreCase = true)) {
                startGifRendering(uri)
            } else {
                startVideoRendering(holder, uri)
            }
        }

        private fun startGifRendering(uri: String) {
            scope.launch {
                val drawable = withContext(Dispatchers.IO) {
                    try {
                        val source = ImageDecoder.createSource(File(uri))
                        ImageDecoder.decodeDrawable(source) as? AnimatedImageDrawable
                    } catch (_: Exception) {
                        null
                    }
                } ?: return@launch

                drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                gifDrawable = drawable

                val cb = object : Choreographer.FrameCallback {
                    override fun doFrame(frameTimeNanos: Long) {
                        val h = surfaceHolder ?: return
                        if (!h.surface.isValid) return
                        val canvas = h.lockCanvas() ?: return
                        try {
                            val frame = h.surfaceFrame
                            val sw = frame.width()
                            val sh = frame.height()
                            val dw = drawable.intrinsicWidth.takeIf { it > 0 } ?: sw
                            val dh = drawable.intrinsicHeight.takeIf { it > 0 } ?: sh
                            val scale = maxOf(sw.toFloat() / dw, sh.toFloat() / dh)
                            val offsetX = (sw - dw * scale) / 2f
                            val offsetY = (sh - dh * scale) / 2f
                            canvas.drawColor(android.graphics.Color.BLACK)
                            canvas.withTranslation(offsetX, offsetY) {
                                canvas.scale(scale, scale)
                                drawable.setBounds(0, 0, dw, dh)
                                drawable.draw(canvas)
                            }
                        } finally {
                            h.unlockCanvasAndPost(canvas)
                        }
                        Choreographer.getInstance().postFrameCallback(this)
                    }
                }
                frameCallback = cb
                drawable.start()
                Choreographer.getInstance().postFrameCallback(cb)
            }
        }

        /**
         * ExoPlayer를 메인 스레드에서 생성·설정·재생.
         * ExoPlayer는 생성한 Looper(기본: 메인 스레드)에서만 접근 가능하므로
         * withContext(Dispatchers.IO)를 사용하지 않는다.
         */
        private fun startVideoRendering(holder: SurfaceHolder, uri: String) {
            scope.launch {
                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        /* minBufferMs    */ 500,
                        /* maxBufferMs    */ 2_000,
                        /* bufferForPlaybackMs           */ 250,
                        /* bufferForPlaybackAfterRebuffer */ 500,
                    )
                    .build()

                val builtPlayer = ExoPlayer.Builder(this@VideoLiveWallpaperService)
                    .setLoadControl(loadControl)
                    .build()
                    .apply {
                        repeatMode = Player.REPEAT_MODE_ALL
                        volume = 0f
                        setMediaItem(MediaItem.fromUri(uri))
                        prepare()
                    }

                val surface = holder.surface
                if (!surface.isValid) {
                    builtPlayer.release()
                    return@launch
                }
                builtPlayer.setVideoSurface(surface)
                player = builtPlayer
                builtPlayer.play()
            }
        }
    }
}
