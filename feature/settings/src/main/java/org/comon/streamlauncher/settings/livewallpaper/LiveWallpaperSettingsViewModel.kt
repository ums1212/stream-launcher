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
                setLiveWallpaperUseCase(id, uri, orientation)
                sendEffect(LiveWallpaperSettingsSideEffect.LaunchLiveWallpaperPicker)
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
            runCatching { setLiveWallpaperUseCase(null, null, orientation) }
                .onSuccess {
                    if (orientation == WallpaperOrientation.LANDSCAPE) {
                        updateState { copy(activeLandscapeWallpaperId = null) }
                    } else {
                        updateState { copy(activePortraitWallpaperId = null, isLiveWallpaperServiceActive = false) }
                    }
                    sendEffect(LiveWallpaperSettingsSideEffect.ReloadWallpaper)
                }
                .onFailure { error ->
                    sendEffect(LiveWallpaperSettingsSideEffect.ShowError(error.getErrorMessage("라이브 배경화면 해제")))
                }
        }
    }
}
