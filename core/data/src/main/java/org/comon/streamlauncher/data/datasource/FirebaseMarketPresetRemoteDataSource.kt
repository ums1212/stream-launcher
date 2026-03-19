package org.comon.streamlauncher.data.datasource

import androidx.paging.PagingSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import org.comon.streamlauncher.data.paging.MarketPresetPagingSource
import org.comon.streamlauncher.data.paging.SearchMarketPresetPagingSource
import org.comon.streamlauncher.data.remote.firestore.MarketPresetDto
import org.comon.streamlauncher.data.remote.firestore.toDomain
import org.comon.streamlauncher.data.remote.firestore.toDto
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMarketPresetRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : MarketPresetRemoteDataSource {

    private val presetsCollection get() = firestore.collection("presets")

    override suspend fun getPresetsOrderedBy(field: String, limit: Int): List<MarketPreset> =
        presetsCollection
            .orderBy(field, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(MarketPresetDto::class.java)?.toDomain() }

    override suspend fun getPresetById(presetId: String): MarketPreset? {
        val doc = presetsCollection.document(presetId).get().await()
        return doc.toObject(MarketPresetDto::class.java)?.toDomain()
    }

    override suspend fun getPresetsByAuthor(uid: String): List<MarketPreset> =
        presetsCollection
            .whereEqualTo("authorUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(MarketPresetDto::class.java)?.toDomain() }

    override suspend fun savePreset(preset: MarketPreset): String {
        val dto = preset.toDto()
        val docRef = if (preset.id.isEmpty()) {
            presetsCollection.document()
        } else {
            presetsCollection.document(preset.id)
        }
        docRef.set(dto).await()
        return docRef.id
    }

    override suspend fun isLikedByUser(presetId: String, uid: String): Boolean {
        val likeDoc = presetsCollection
            .document(presetId)
            .collection("likes")
            .document(uid)
            .get()
            .await()
        return likeDoc.exists()
    }

    override suspend fun toggleLike(presetId: String, uid: String): Boolean {
        val likeRef = presetsCollection.document(presetId).collection("likes").document(uid)
        val presetRef = presetsCollection.document(presetId)

        val exists = likeRef.get().await().exists()
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(presetRef)
            val current = snapshot.getLong("likeCount") ?: 0L
            if (exists) {
                transaction.delete(likeRef)
                transaction.update(presetRef, "likeCount", maxOf(0L, current - 1))
            } else {
                transaction.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
                transaction.update(presetRef, "likeCount", current + 1)
            }
        }.await()
        return !exists
    }

    override suspend fun incrementDownloadCount(presetId: String) {
        presetsCollection.document(presetId)
            .update("downloadCount", FieldValue.increment(1))
            .await()
    }

    override fun createRecentPresetsPagingSource(): PagingSource<*, MarketPreset> =
        MarketPresetPagingSource(firestore)

    override fun createSearchPresetsPagingSource(query: String): PagingSource<*, MarketPreset> =
        SearchMarketPresetPagingSource(firestore, query)
}
