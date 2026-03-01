package org.comon.streamlauncher.preset_market

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.comon.streamlauncher.data.paging.MarketPresetPagingSource
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.usecase.GetTopDownloadPresetsUseCase
import org.comon.streamlauncher.domain.usecase.GetTopLikePresetsUseCase
import org.comon.streamlauncher.domain.usecase.ObserveAuthStateUseCase
import org.comon.streamlauncher.domain.usecase.SignInWithGoogleUseCase
import org.comon.streamlauncher.domain.usecase.SignOutUseCase
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class PresetMarketViewModel @Inject constructor(
    private val getTopDownloadPresetsUseCase: GetTopDownloadPresetsUseCase,
    private val getTopLikePresetsUseCase: GetTopLikePresetsUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val firestore: FirebaseFirestore,
) : BaseViewModel<PresetMarketState, PresetMarketIntent, PresetMarketSideEffect>(PresetMarketState()) {

    val recentPresetsPaging: Flow<PagingData<MarketPreset>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = { MarketPresetPagingSource(firestore) },
    ).flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                updateState { copy(currentUser = user) }
            }
        }
        handleIntent(PresetMarketIntent.LoadTopPresets)
    }

    override fun handleIntent(intent: PresetMarketIntent) {
        when (intent) {
            is PresetMarketIntent.LoadTopPresets -> loadTopPresets()
            is PresetMarketIntent.SelectTab -> updateState { copy(selectedTab = intent.tab) }
            is PresetMarketIntent.ClickPreset -> sendEffect(PresetMarketSideEffect.NavigateToDetail(intent.presetId))
            is PresetMarketIntent.SignInWithGoogle -> signIn(intent.idToken)
            is PresetMarketIntent.SignOut -> signOut()
            is PresetMarketIntent.NavigateToSearch -> sendEffect(PresetMarketSideEffect.NavigateToSearch(intent.query))
            is PresetMarketIntent.DismissError -> updateState { copy(error = null) }
        }
    }

    private fun loadTopPresets() {
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            val downloadsResult = getTopDownloadPresetsUseCase()
            val likesResult = getTopLikePresetsUseCase()
            updateState {
                copy(
                    isLoading = false,
                    topDownloadPresets = downloadsResult.getOrDefault(emptyList()),
                    topLikePresets = likesResult.getOrDefault(emptyList()),
                    error = downloadsResult.exceptionOrNull()?.message
                        ?: likesResult.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun signIn(idToken: String) {
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess { user ->
                    updateState { copy(currentUser = user) }
                    sendEffect(PresetMarketSideEffect.SignInSuccess)
                }
                .onFailure { e ->
                    sendEffect(PresetMarketSideEffect.ShowError(e.message ?: "로그인 실패"))
                }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            updateState { copy(currentUser = null) }
        }
    }
}
