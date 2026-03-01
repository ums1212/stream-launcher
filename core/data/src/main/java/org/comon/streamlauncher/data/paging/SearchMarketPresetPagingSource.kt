package org.comon.streamlauncher.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import org.comon.streamlauncher.data.remote.firestore.MarketPresetDto
import org.comon.streamlauncher.data.remote.firestore.toDomain
import org.comon.streamlauncher.domain.model.preset.MarketPreset

class SearchMarketPresetPagingSource(
    private val firestore: FirebaseFirestore,
    private val query: String,
) : PagingSource<DocumentSnapshot, MarketPreset>() {

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, MarketPreset> {
        return try {
            val firstKeyword = query.lowercase().trim().split(" ").firstOrNull() ?: ""
            if (firstKeyword.isEmpty()) {
                return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }

            val collection = firestore.collection("presets")
            var firestoreQuery = collection
                .whereArrayContains("searchKeywords", firstKeyword)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())

            params.key?.let { lastDoc ->
                firestoreQuery = firestoreQuery.startAfter(lastDoc)
            }

            val snapshot = firestoreQuery.get().await()
            val allPresets = snapshot.documents.mapNotNull {
                it.toObject(MarketPresetDto::class.java)?.toDomain()
            }

            // 복수 키워드 클라이언트 필터링
            val extraKeywords = query.lowercase().trim().split(" ").drop(1).filter { it.isNotBlank() }
            val filtered = if (extraKeywords.isEmpty()) {
                allPresets
            } else {
                allPresets.filter { preset ->
                    extraKeywords.all { kw ->
                        preset.name.lowercase().contains(kw) ||
                            preset.tags.any { tag -> tag.lowercase().contains(kw) }
                    }
                }
            }

            val nextKey = if (snapshot.documents.size < params.loadSize) null
            else snapshot.documents.lastOrNull()

            LoadResult.Page(
                data = filtered,
                prevKey = null,
                nextKey = nextKey,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, MarketPreset>): DocumentSnapshot? = null
}
