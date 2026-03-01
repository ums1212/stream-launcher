package org.comon.streamlauncher.preset_market

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.comon.streamlauncher.data.paging.SearchMarketPresetPagingSource
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class MarketSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val firestore: FirebaseFirestore,
) : BaseViewModel<MarketSearchState, MarketSearchIntent, MarketSearchSideEffect>(
    MarketSearchState(query = savedStateHandle.get<String>("query") ?: "")
) {
    private val queryFlow = MutableStateFlow(currentState.query)

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultsPaging: Flow<PagingData<MarketPreset>> = queryFlow
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { SearchMarketPresetPagingSource(firestore, query) },
            ).flow
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
