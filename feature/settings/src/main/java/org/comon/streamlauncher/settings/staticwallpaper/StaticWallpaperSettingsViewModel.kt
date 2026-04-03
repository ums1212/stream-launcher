package org.comon.streamlauncher.settings.staticwallpaper

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.model.WallpaperOrientation
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SetStaticWallpaperUseCase
import org.comon.streamlauncher.domain.util.WallpaperHelper
import org.comon.streamlauncher.network.error.getErrorMessage
import org.comon.streamlauncher.network.error.isNetworkDisconnected
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class StaticWallpaperSettingsViewModel @Inject constructor(
    private val getLauncherSettingsUseCase: GetLauncherSettingsUseCase,
    private val setStaticWallpaperUseCase: SetStaticWallpaperUseCase,
    private val wallpaperHelper: WallpaperHelper,
) : BaseViewModel<StaticWallpaperSettingsState, StaticWallpaperSettingsIntent, StaticWallpaperSettingsSideEffect>(StaticWallpaperSettingsState()) {

    init {
        viewModelScope.launch {
            getLauncherSettingsUseCase().collect { settings ->
                updateState {
                    copy(
                        staticWallpaperPortraitUri = settings.staticWallpaperPortraitUri,
                        staticWallpaperLandscapeUri = settings.staticWallpaperLandscapeUri,
                    )
                }
            }
        }
    }

    override fun handleIntent(intent: StaticWallpaperSettingsIntent) {
        when (intent) {
            is StaticWallpaperSettingsIntent.SetStaticWallpaper -> setStaticWallpaper(
                intent.uri,
                intent.orientation,
                intent.isCurrentLandscape,
            )
            is StaticWallpaperSettingsIntent.ClearStaticWallpaper -> clearStaticWallpaper(intent.orientation)
            is StaticWallpaperSettingsIntent.SwitchStaticWallpaperTab -> updateState { copy(selectedStaticWallpaperTab = intent.orientation) }
        }
    }

    private fun setStaticWallpaper(uri: String, orientation: WallpaperOrientation, isCurrentLandscape: Boolean) {
        viewModelScope.launch {
            runCatching {
                val filePath = setStaticWallpaperUseCase(uri, orientation) ?: return@runCatching
                val savedIsLandscape = orientation == WallpaperOrientation.LANDSCAPE
                if (filePath.isNotEmpty() && savedIsLandscape == isCurrentLandscape) {
                    wallpaperHelper.setWallpaperFromPreset(filePath)
                }
            }.onFailure { error ->
                if (error.isNetworkDisconnected()) sendEffect(StaticWallpaperSettingsSideEffect.ShowNetworkError)
                else sendEffect(StaticWallpaperSettingsSideEffect.ShowError(error.getErrorMessage("배경화면 설정")))
            }
        }
    }

    private fun clearStaticWallpaper(orientation: WallpaperOrientation) {
        viewModelScope.launch {
            runCatching {
                setStaticWallpaperUseCase(null, orientation)
            }.onFailure { error ->
                if (error.isNetworkDisconnected()) sendEffect(StaticWallpaperSettingsSideEffect.ShowNetworkError)
                else sendEffect(StaticWallpaperSettingsSideEffect.ShowError(error.getErrorMessage("배경화면 초기화")))
            }
        }
    }
}
