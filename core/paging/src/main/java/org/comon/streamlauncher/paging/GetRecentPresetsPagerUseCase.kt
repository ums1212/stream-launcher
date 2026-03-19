package org.comon.streamlauncher.paging

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import javax.inject.Inject

class GetRecentPresetsPagerUseCase @Inject constructor(
    private val provider: RecentPresetsPagerProvider,
) {
    operator fun invoke(): Flow<PagingData<MarketPreset>> = provider.provide()
}
