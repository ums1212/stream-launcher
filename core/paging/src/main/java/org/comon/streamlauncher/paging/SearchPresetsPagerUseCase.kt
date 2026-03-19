package org.comon.streamlauncher.paging

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import javax.inject.Inject

class SearchPresetsPagerUseCase @Inject constructor(
    private val provider: SearchPresetsPagerProvider,
) {
    operator fun invoke(query: String): Flow<PagingData<MarketPreset>> = provider.provide(query)
}
