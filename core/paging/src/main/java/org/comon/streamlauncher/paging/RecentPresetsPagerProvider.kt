package org.comon.streamlauncher.paging

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.preset.MarketPreset

interface RecentPresetsPagerProvider {
    fun provide(): Flow<PagingData<MarketPreset>>
}
