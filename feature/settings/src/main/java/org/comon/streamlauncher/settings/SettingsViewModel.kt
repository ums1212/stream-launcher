package org.comon.streamlauncher.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.usecase.CheckNoticeUseCase
import org.comon.streamlauncher.domain.usecase.DismissNoticeUseCase
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SignInWithGoogleUseCase
import org.comon.streamlauncher.domain.util.WallpaperHelper
import org.comon.streamlauncher.network.error.getErrorMessage
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getLauncherSettingsUseCase: GetLauncherSettingsUseCase,
    private val checkNoticeUseCase: CheckNoticeUseCase,
    private val dismissNoticeUseCase: DismissNoticeUseCase,
    private val wallpaperHelper: WallpaperHelper,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
) : BaseViewModel<SettingsState, SettingsIntent, SettingsSideEffect>(SettingsState()) {

    private var currentNoticeVersion: String = ""

    init {
        viewModelScope.launch {
            getLauncherSettingsUseCase().collect { settings ->
                updateState {
                    copy(
                        colorPresetIndex = settings.colorPresetIndex,
                        staticWallpaperPortraitUri = settings.staticWallpaperPortraitUri,
                        staticWallpaperLandscapeUri = settings.staticWallpaperLandscapeUri,
                    )
                }
            }
        }
    }

    override fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ShowNotice -> updateState { copy(showNoticeDialog = true) }
            is SettingsIntent.DismissNotice -> dismissNotice()
            is SettingsIntent.ResetTab -> sendEffect(SettingsSideEffect.NavigateToMain)
            is SettingsIntent.ApplyStaticWallpaperForOrientation -> applyStaticWallpaperForOrientation(intent.isLandscape)
            is SettingsIntent.SignInWithGoogle -> signIn(intent.idToken)
        }
    }

    fun checkNotice(version: String) {
        currentNoticeVersion = version
        viewModelScope.launch {
            if (checkNoticeUseCase(version)) {
                updateState { copy(showNoticeDialog = true) }
            }
        }
    }

    private fun dismissNotice() {
        updateState { copy(showNoticeDialog = false) }
        viewModelScope.launch {
            dismissNoticeUseCase(currentNoticeVersion)
        }
    }

    private fun applyStaticWallpaperForOrientation(isLandscape: Boolean) {
        if (wallpaperHelper.isLiveWallpaperServiceActive()) return
        val state = currentState
        val filePath = if (isLandscape) {
            state.staticWallpaperLandscapeUri ?: state.staticWallpaperPortraitUri
        } else {
            state.staticWallpaperPortraitUri
        } ?: return
        viewModelScope.launch {
            runCatching { wallpaperHelper.setWallpaperFromPreset(filePath) }
        }
    }

    private fun signIn(idToken: String) {
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onFailure { error ->
                    sendEffect(SettingsSideEffect.ShowError(error.getErrorMessage("로그인")))
                }
        }
    }
}
