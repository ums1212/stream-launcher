package org.comon.streamlauncher.preset_market

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.paging.SearchPresetsPagerUseCase
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class MarketSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchPresetsPagerUseCase: SearchPresetsPagerUseCase,
) : BaseViewModel<MarketSearchState, MarketSearchIntent, MarketSearchSideEffect>(
    MarketSearchState(query = savedStateHandle.get<String>("query") ?: "")
) {
    private val queryFlow = MutableStateFlow(currentState.query)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultsPaging: Flow<PagingData<MarketPreset>> = queryFlow
        .flatMapLatest { query ->
            searchPresetsPagerUseCase(query)
        }
        .cachedIn(viewModelScope)

    override fun handleIntent(intent: MarketSearchIntent) {
        when (intent) {
            is MarketSearchIntent.Search -> {
                updateState { copy(query = intent.query) }
                queryFlow.value = intent.query
            }
            is MarketSearchIntent.ClickPreset ->
                sendEffect(MarketSearchSideEffect.NavigateToDetail(intent.presetId))
        }
    }
}
