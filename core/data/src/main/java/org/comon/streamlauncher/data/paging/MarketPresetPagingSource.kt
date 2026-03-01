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

class MarketPresetPagingSource(
    private val firestore: FirebaseFirestore,
) : PagingSource<DocumentSnapshot, MarketPreset>() {

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, MarketPreset> {
        return try {
            val collection = firestore.collection("presets")
            var query = collection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())

            params.key?.let { lastDoc ->
                query = query.startAfter(lastDoc)
            }

            val snapshot = query.get().await()
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
