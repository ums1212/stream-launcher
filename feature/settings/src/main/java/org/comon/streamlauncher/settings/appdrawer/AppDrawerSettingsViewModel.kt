package org.comon.streamlauncher.settings.appdrawer

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.usecase.GetLauncherSettingsUseCase
import org.comon.streamlauncher.domain.usecase.SaveAppDrawerSettingsUseCase
import org.comon.streamlauncher.network.connectivity.NetworkConnectivityChecker
import org.comon.streamlauncher.network.error.getErrorMessage
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class AppDrawerSettingsViewModel @Inject constructor(
    private val connectivityChecker: NetworkConnectivityChecker,
    private val getLauncherSettingsUseCase: GetLauncherSettingsUseCase,
    private val saveAppDrawerSettingsUseCase: SaveAppDrawerSettingsUseCase,
) : BaseViewModel<AppDrawerSettingsState, AppDrawerSettingsIntent, AppDrawerSettingsSideEffect>(AppDrawerSettingsState()) {

    init {
        viewModelScope.launch {
            getLauncherSettingsUseCase().collect { settings ->
                updateState {
                    copy(
                        appDrawerGridColumns = settings.appDrawerGridColumns,
                        appDrawerGridRows = settings.appDrawerGridRows,
                        appDrawerIconSizeRatio = settings.appDrawerIconSizeRatio,
                    )
                }
            }
        }
    }

    override fun handleIntent(intent: AppDrawerSettingsIntent) {
        when (intent) {
            is AppDrawerSettingsIntent.SaveAppDrawerSettings -> saveAppDrawerSettings(
                intent.columns,
                intent.rows,
                intent.iconSizeRatio,
            )
        }
    }

    private fun saveAppDrawerSettings(columns: Int, rows: Int, iconSizeRatio: Float) {
        updateState {
            copy(
                appDrawerGridColumns = columns,
                appDrawerGridRows = rows,
                appDrawerIconSizeRatio = iconSizeRatio,
            )
        }
        viewModelScope.launch {
            runCatching {
                saveAppDrawerSettingsUseCase(columns, rows, iconSizeRatio)
            }.onFailure { error ->
                if (connectivityChecker.isUnavailable()) sendEffect(AppDrawerSettingsSideEffect.ShowNetworkError)
                else sendEffect(AppDrawerSettingsSideEffect.ShowError(error.getErrorMessage("앱 서랍 설정 저장")))
            }
        }
    }
}
