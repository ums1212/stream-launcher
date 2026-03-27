package org.comon.streamlauncher.data.datasource

import androidx.paging.PagingSource
import org.comon.streamlauncher.domain.model.preset.MarketPreset

interface MarketPresetRemoteDataSource {
    suspend fun getPresetsOrderedBy(field: String, limit: Int): List<MarketPreset>
    suspend fun getPresetById(presetId: String): MarketPreset?
    suspend fun getPresetsByAuthor(uid: String): List<MarketPreset>
    suspend fun savePreset(preset: MarketPreset): String
    suspend fun isLikedByUser(presetId: String, uid: String): Boolean
    suspend fun toggleLike(presetId: String, uid: String): Boolean
    suspend fun incrementDownloadCount(presetId: String)
    suspend fun softDeletePreset(presetId: String)
    fun createRecentPresetsPagingSource(): PagingSource<*, MarketPreset>
    fun createSearchPresetsPagingSource(query: String): PagingSource<*, MarketPreset>
    suspend fun reportPreset(data: Map<String, Any>)
}
