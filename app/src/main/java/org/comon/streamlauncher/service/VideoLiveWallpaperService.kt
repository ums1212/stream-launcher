package org.comon.streamlauncher.service

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.graphics.withTranslation

/**
 * 동영상/GIF 파일을 시스템 라이브 배경화면으로 재생하는 WallpaperService.
 *
 * - GIF: AnimatedImageDrawable + Choreographer → SurfaceHolder Canvas 직접 렌더링
 * - 동영상(mp4/webm): ExoPlayer → setVideoSurface()
 *
 * WallpaperService는 @AndroidEntryPoint Hilt 주입이 지원되지 않으므로
 * EntryPointAccessors를 통해 SettingsRepository에 접근합니다.
 *
 * 최적화:
 * - ExoPlayer 생성/설정/재생은 메인 스레드에서만 (스레드 위반 방지)
 * - DefaultLoadControl 버퍼를 최소화해 첫 프레임 지연 단축
 * - liveWallpaperUri만 읽는 경량 Flow로 불필요한 JSON 파싱 제거
 * - PagerScrollState로 스와이프 중 렌더링 일시 중지
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
        private var scrollObserverJob: Job? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceHolder = holder
            startObservingUri(holder)
            startObservingScrollState()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible && !PagerScrollState.isScrolling.value) {
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
            scrollObserverJob?.cancel()
            scrollObserverJob = null
            surfaceHolder = null
        }

        override fun onDestroy() {
            super.onDestroy()
            stopCurrent()
            scrollObserverJob?.cancel()
            scrollObserverJob = null
            surfaceHolder = null
            scope.cancel()
        }

        private fun startObservingUri(holder: SurfaceHolder) {
            scope.launch {
                getLiveWallpaperUriFlow()
                    .distinctUntilChanged()
                    .collect { uri ->
                        if (uri != null && uri != currentUri) {
                            currentUri = uri
                            applyUri(holder, uri)
                        }
                    }
            }
        }

        private fun startObservingScrollState() {
            scrollObserverJob?.cancel()
            scrollObserverJob = scope.launch {
                PagerScrollState.isScrolling.collect { scrolling ->
                    if (scrolling) pauseRendering() else resumeRendering()
                }
            }
        }

        private fun pauseRendering() {
            player?.pause()
            frameCallback?.let { Choreographer.getInstance().removeFrameCallback(it) }
        }

        private fun resumeRendering() {
            if (!isVisible) return
            player?.play()
            frameCallback?.let { Choreographer.getInstance().postFrameCallback(it) }
        }

        /**
         * liveWallpaperUri만 읽는 경량 Flow.
         * getSettings() 전체 파싱(JSON) 없이 단일 키만 조회한다.
         */
        private fun getLiveWallpaperUriFlow() = try {
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                applicationContext,
                LiveWallpaperEntryPoint::class.java,
            )
            entryPoint.settingsRepository().getLiveWallpaperUri()
        } catch (_: Exception) {
            flowOf(null)
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
                if (!PagerScrollState.isScrolling.value) {
                    Choreographer.getInstance().postFrameCallback(cb)
                }
            }
        }

        /**
         * ExoPlayer를 메인 스레드에서 생성·설정·재생.
         *
         * ExoPlayer는 생성한 Looper(기본: 메인 스레드)에서만 접근 가능하므로
         * withContext(Dispatchers.IO) 를 사용하지 않는다.
         * DefaultLoadControl 버퍼를 라이브 배경화면 특성에 맞게 최소화:
         * 짧은 루프 영상이므로 대용량 선버퍼가 불필요하고 첫 프레임 지연만 늘린다.
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

                // ExoPlayer는 생성·설정·재생 모두 메인 스레드(Dispatchers.Main)에서 해야 함.
                // withContext(Dispatchers.IO) 에서 호출하면 wrong-thread IllegalStateException 발생.
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
                if (!PagerScrollState.isScrolling.value) builtPlayer.play()
            }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface LiveWallpaperEntryPoint {
    fun settingsRepository(): org.comon.streamlauncher.domain.repository.SettingsRepository
}
