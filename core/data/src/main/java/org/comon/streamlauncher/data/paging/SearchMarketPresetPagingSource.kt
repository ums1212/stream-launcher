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
            val searchTerm = query.lowercase().trim().replace(" ", "")
            if (searchTerm.isEmpty()) {
                return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }

            val collection = firestore.collection("presets")
            var firestoreQuery = collection
                .whereEqualTo("isDeleted", false)
                .whereArrayContains("searchKeywords", searchTerm)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())

            params.key?.let { lastDoc ->
                firestoreQuery = firestoreQuery.startAfter(lastDoc)
            }

            val snapshot = firestoreQuery.get().await()
            val presets = snapshot.documents.mapNotNull {
                it.toObject(MarketPresetDto::class.java)?.toDomain()
            }

            val nextKey = if (snapshot.documents.size < params.loadSize) null
            else snapshot.documents.lastOrNull()

            LoadResult.Page(
                data = presets,
                prevKey = null,
                nextKey = nextKey,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, MarketPreset>): DocumentSnapshot? = null
}
