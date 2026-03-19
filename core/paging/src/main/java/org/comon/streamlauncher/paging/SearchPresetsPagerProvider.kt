package org.comon.streamlauncher.paging

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.preset.MarketPreset

interface SearchPresetsPagerProvider {
    fun provide(query: String): Flow<PagingData<MarketPreset>>
}
