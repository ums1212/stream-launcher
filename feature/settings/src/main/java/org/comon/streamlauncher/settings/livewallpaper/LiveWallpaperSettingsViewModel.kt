package org.comon.streamlauncher.settings.livewallpaper

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.domain.usecase.DeleteLiveWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.GetAllLiveWallpapersUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveLiveWallpaperUseCase
import org.comon.streamlauncher.domain.usecase.SetLiveWallpaperUseCase
import org.comon.streamlauncher.domain.util.WallpaperHelper
import org.comon.streamlauncher.network.error.getErrorMessage
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class LiveWallpaperSettingsViewModel @Inject constructor(
    private val getLauncherSettingsUseCase: GetLauncherSettingsUseCase,
    private val getAllLiveWallpapersUseCase: GetAllLiveWallpapersUseCase,
    private val saveLiveWallpaperUseCase: SaveLiveWallpaperUseCase,
    private val deleteLiveWallpaperUseCase: DeleteLiveWallpaperUseCase,
    private val setLiveWallpaperUseCase: SetLiveWallpaperUseCase,
    private val wallpaperHelper: WallpaperHelper,
) : BaseViewModel<LiveWallpaperSettingsState, LiveWallpaperSettingsIntent, LiveWallpaperSettingsSideEffect>(LiveWallpaperSettingsState()) {

    init {
        viewModelScope.launch {
            getLauncherSettingsUseCase().collect { settings ->
                updateState {
                    copy(
                        selectedLiveWallpaperId = settings.liveWallpaperId,
                        selectedLiveWallpaperUri = settings.liveWallpaperUri,
                        selectedLiveWallpaperLandscapeId = settings.liveWallpaperLandscapeId,
                        selectedLiveWallpaperLandscapeUri = settings.liveWallpaperLandscapeUri,
                        activePortraitWallpaperId = settings.liveWallpaperId,
                        activeLandscapeWallpaperId = settings.liveWallpaperLandscapeId,
                    )
                }
            }
        }
        viewModelScope.launch {
            getAllLiveWallpapersUseCase().collect { list ->
                updateState { copy(liveWallpapers = list) }
            }
        }
        viewModelScope.launch {
            updateState { copy(isLiveWallpaperServiceActive = wallpaperHelper.isLiveWallpaperServiceActive()) }
        }
    }

    override fun handleIntent(intent: LiveWallpaperSettingsIntent) {
        when (intent) {
            is LiveWallpaperSettingsIntent.CheckActiveWallpaper -> {
                updateState { copy(isLiveWallpaperServiceActive = wallpaperHelper.isLiveWallpaperServiceActive()) }
            }
            is LiveWallpaperSettingsIntent.LoadLiveWallpaperFile -> {
                val isLandscape = currentState.selectedOrientationTab == WallpaperOrientation.LANDSCAPE
                if (isLandscape) {
                    updateState { copy(selectedLiveWallpaperLandscapeUri = intent.uri, selectedLiveWallpaperLandscapeId = null) }
                } else {
                    updateState { copy(selectedLiveWallpaperUri = intent.uri, selectedLiveWallpaperId = null) }
                }
            }
            is LiveWallpaperSettingsIntent.CreateLiveWallpaper -> createLiveWallpaper(intent.name)
            is LiveWallpaperSettingsIntent.SelectLiveWallpaper -> {
                val isLandscape = currentState.selectedOrientationTab == WallpaperOrientation.LANDSCAPE
                if (isLandscape) {
                    updateState { copy(selectedLiveWallpaperLandscapeId = intent.id, selectedLiveWallpaperLandscapeUri = intent.uri) }
                } else {
                    updateState { copy(selectedLiveWallpaperId = intent.id, selectedLiveWallpaperUri = intent.uri) }
                }
            }
            is LiveWallpaperSettingsIntent.SetActiveLiveWallpaper -> setActiveLiveWallpaper(intent.id, intent.uri, intent.orientation)
            is LiveWallpaperSettingsIntent.DeleteLiveWallpaper -> deleteLiveWallpaper(intent.id)
            is LiveWallpaperSettingsIntent.ClearActiveLiveWallpaper -> clearActiveLiveWallpaper(intent.orientation)
            is LiveWallpaperSettingsIntent.SwitchOrientationTab -> updateState { copy(selectedOrientationTab = intent.orientation) }
            is LiveWallpaperSettingsIntent.ConfirmLandscapeWallpaper -> {
                viewModelScope.launch {
                    runCatching { setLiveWallpaperUseCase(intent.id, intent.uri, WallpaperOrientation.LANDSCAPE) }
                        .onFailure { error ->
                            sendEffect(LiveWallpaperSettingsSideEffect.ShowError(error.getErrorMessage("라이브 배경화면 설정")))
                        }
                }
            }
        }
    }

    private fun createLiveWallpaper(name: String) {
        val state = currentState
        val isLandscape = state.selectedOrientationTab == WallpaperOrientation.LANDSCAPE
        val uri = if (isLandscape) state.selectedLiveWallpaperLandscapeUri else state.selectedLiveWallpaperUri
        uri ?: return
        viewModelScope.launch {
            runCatching {
                val lw = saveLiveWallpaperUseCase(name, uri)
                if (isLandscape) {
                    updateState { copy(selectedLiveWallpaperLandscapeId = lw.id, selectedLiveWallpaperLandscapeUri = lw.fileUri) }
                } else {
                    updateState { copy(selectedLiveWallpaperId = lw.id, selectedLiveWallpaperUri = lw.fileUri) }
                }
            }.onFailure { error ->
                sendEffect(LiveWallpaperSettingsSideEffect.ShowError(error.getErrorMessage("라이브 배경화면 저장")))
            }
        }
    }

    private fun setActiveLiveWallpaper(id: Int, uri: String, orientation: WallpaperOrientation) {
        viewModelScope.launch {
            runCatching {
                if (orientation == WallpaperOrientation.LANDSCAPE) {
                    // 가로 배경화면: DataStore 기록을 피커 확정 후로 미룬다.
                    // 피커 미리보기를 위해 세로 파일을 임시 교체하고, onResume에서 ConfirmLandscapeWallpaper로 확정한다.
                    sendEffect(LiveWallpaperSettingsSideEffect.LaunchLiveWallpaperPicker(landscapeNewId = id, landscapeNewUri = uri))
                } else {
                    // 세로 배경화면: 세로 URI 저장 + 가로 URI 초기화 후 피커 실행.
                    // 가로 기록이 남아 있으면 화면 회전 시 서비스가 가로 URI를 우선 적용하므로 함께 지운다.
                    setLiveWallpaperUseCase(id, uri, orientation)
                    setLiveWallpaperUseCase(null, null, WallpaperOrientation.LANDSCAPE)
                    sendEffect(LiveWallpaperSettingsSideEffect.LaunchLiveWallpaperPicker())
                }
            }.onFailure { error ->
                sendEffect(LiveWallpaperSettingsSideEffect.ShowError(error.getErrorMessage("라이브 배경화면 설정")))
            }
        }
    }

    private fun deleteLiveWallpaper(id: Int) {
        viewModelScope.launch {
            runCatching {
                deleteLiveWallpaperUseCase(id)
                val state = currentState
                if (state.selectedLiveWallpaperId == id) {
                    updateState { copy(selectedLiveWallpaperId = null, selectedLiveWallpaperUri = null) }
                }
            }.onFailure { error ->
                sendEffect(LiveWallpaperSettingsSideEffect.ShowError(error.getErrorMessage("라이브 배경화면 삭제")))
            }
        }
    }

    private fun clearActiveLiveWallpaper(orientation: WallpaperOrientation) {
        viewModelScope.launch {
            runCatching {
                if (orientation == WallpaperOrientation.PORTRAIT) {
                    // 세로 해제: 세로+가로 모두 삭제 (가로만 남은 상태는 지원하지 않음)
                    setLiveWallpaperUseCase(null, null, WallpaperOrientation.PORTRAIT)
                    setLiveWallpaperUseCase(null, null, WallpaperOrientation.LANDSCAPE)
                } else {
                    // 가로 해제: 가로만 삭제, 세로 유지
                    setLiveWallpaperUseCase(null, null, WallpaperOrientation.LANDSCAPE)
                }
            }
            .onSuccess {
                if (orientation == WallpaperOrientation.LANDSCAPE) {
                    // 가로만 해제 → 서비스 유지, 화면 회전 시 세로 배경화면 fallback
                    // broadcast는 setLiveWallpaperUseCase 내부에서 이미 전송됨
                    updateState { copy(activeLandscapeWallpaperId = null) }
                } else {
                    // 세로 해제 → 서비스 비활성화 (WallpaperManager.clear() 포함)
                    updateState {
                        copy(
                            activePortraitWallpaperId = null,
                            activeLandscapeWallpaperId = null,
                            isLiveWallpaperServiceActive = false,
                        )
                    }
                    sendEffect(LiveWallpaperSettingsSideEffect.ReloadWallpaper)
                }
            }
            .onFailure { error ->
                sendEffect(LiveWallpaperSettingsSideEffect.ShowError(error.getErrorMessage("라이브 배경화면 해제")))
            }
        }
    }
}
