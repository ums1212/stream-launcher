package org.comon.streamlauncher.service

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 동영상/GIF 파일을 시스템 라이브 배경화면으로 재생하는 WallpaperService.
 *
 * - GIF: AnimatedImageDrawable + Choreographer → SurfaceHolder Canvas 직접 렌더링
 * - 동영상(mp4/webm): ExoPlayer → setVideoSurface()
 *
 * WallpaperService는 @AndroidEntryPoint Hilt 주입이 지원되지 않으므로
 * EntryPointAccessors를 통해 SettingsRepository에 접근합니다.
 */
class VideoLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var player: ExoPlayer? = null
        private var gifDrawable: AnimatedImageDrawable? = null
        private var frameCallback: Choreographer.FrameCallback? = null
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private var currentUri: String? = null
        private var surfaceHolder: SurfaceHolder? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceHolder = holder
            startObservingUri(holder)
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
            surfaceHolder = null
        }

        override fun onDestroy() {
            super.onDestroy()
            scope.cancel()
        }

        private fun startObservingUri(holder: SurfaceHolder) {
            scope.launch {
                getSettingsFlow()
                    .distinctUntilChanged()
                    .collect { uri ->
                        if (uri != null && uri != currentUri) {
                            currentUri = uri
                            applyUri(holder, uri)
                        }
                    }
            }
        }

        private fun getSettingsFlow() = try {
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                applicationContext,
                LiveWallpaperEntryPoint::class.java,
            )
            entryPoint.settingsRepository().getSettings().map { it.liveWallpaperUri }
        } catch (_: Exception) {
            flowOf(null)
        }

        private fun stopCurrent() {
            // 비디오 해제
            player?.release()
            player = null

            // GIF 해제
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
                            // CENTER_CROP: canvas 자체를 변환해 AnimatedImageDrawable 스케일 적용
                            val scale = maxOf(sw.toFloat() / dw, sh.toFloat() / dh)
                            val offsetX = (sw - dw * scale) / 2f
                            val offsetY = (sh - dh * scale) / 2f
                            canvas.drawColor(android.graphics.Color.BLACK)
                            val saveCount = canvas.save()
                            canvas.translate(offsetX, offsetY)
                            canvas.scale(scale, scale)
                            drawable.setBounds(0, 0, dw, dh)
                            drawable.draw(canvas)
                            canvas.restoreToCount(saveCount)
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

        private fun startVideoRendering(holder: SurfaceHolder, uri: String) {
            val surface = holder.surface
            if (!surface.isValid) return

            player = ExoPlayer.Builder(this@VideoLiveWallpaperService).build().apply {
                setVideoSurface(surface)
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                play()
            }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface LiveWallpaperEntryPoint {
    fun settingsRepository(): org.comon.streamlauncher.domain.repository.SettingsRepository
}
