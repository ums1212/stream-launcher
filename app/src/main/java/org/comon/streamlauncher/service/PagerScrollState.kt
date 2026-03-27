package org.comon.streamlauncher.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pager 스크롤 상태를 VideoLiveWallpaperService에 전달하는 싱글톤.
 *
 * CrossPagerNavigation이 스크롤 시작/종료 시 [setScrolling]을 호출하면
 * VideoLiveWallpaperService가 [isScrolling]을 구독하여 렌더링을 일시 중지/재개한다.
 *
 * WallpaperService와 Compose UI가 같은 프로세스에서 동작하므로
 * in-memory StateFlow로 즉각 전달 가능.
 */
object PagerScrollState {
    private val _isScrolling = MutableStateFlow(false)
    val isScrolling: StateFlow<Boolean> = _isScrolling.asStateFlow()

    fun setScrolling(scrolling: Boolean) {
        _isScrolling.value = scrolling
    }
}
