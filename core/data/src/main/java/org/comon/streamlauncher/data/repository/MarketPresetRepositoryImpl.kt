package org.comon.streamlauncher.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.comon.streamlauncher.data.remote.firestore.MarketPresetDto
import org.comon.streamlauncher.data.remote.firestore.toDomain
import org.comon.streamlauncher.data.remote.firestore.toDto
import org.comon.streamlauncher.data.util.ImageCompressor
import org.comon.streamlauncher.domain.model.preset.MarketPreset
import org.comon.streamlauncher.domain.model.preset.MarketUser
import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class MarketPresetRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : MarketPresetRepository {

    private val presetsCollection get() = firestore.collection("presets")

    override fun authStateChanges(): Flow<MarketUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser?.let {
                MarketUser(
                    uid = it.uid,
                    displayName = it.displayName ?: "",
                    email = it.email,
                    photoUrl = it.photoUrl?.toString(),
                )
            }
            trySend(user)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun getCurrentUser(): MarketUser? {
        val user = auth.currentUser ?: return null
        return MarketUser(
            uid = user.uid,
            displayName = user.displayName ?: "",
            email = user.email,
            photoUrl = user.photoUrl?.toString(),
        )
    }

    override suspend fun signInWithGoogle(idToken: String): Result<MarketUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: error("로그인 실패: 유저 정보 없음")
        MarketUser(
            uid = user.uid,
            displayName = user.displayName ?: "",
            email = user.email,
            photoUrl = user.photoUrl?.toString(),
        )
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun getTopByDownloads(limit: Int): Result<List<MarketPreset>> = runCatching {
        presetsCollection
            .orderBy("downloadCount", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(MarketPresetDto::class.java)?.toDomain() }
    }

    override suspend fun getTopByLikes(limit: Int): Result<List<MarketPreset>> = runCatching {
        presetsCollection
            .orderBy("likeCount", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(MarketPresetDto::class.java)?.toDomain() }
    }

    override suspend fun getPresetDetail(presetId: String): Result<MarketPreset> = runCatching {
        val doc = presetsCollection.document(presetId).get().await()
        doc.toObject(MarketPresetDto::class.java)?.toDomain()
            ?: error("프리셋을 찾을 수 없습니다: $presetId")
    }

    override suspend fun isLikedByCurrentUser(presetId: String): Result<Boolean> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching false
        val likeDoc = presetsCollection
            .document(presetId)
            .collection("likes")
            .document(uid)
            .get()
            .await()
        likeDoc.exists()
    }

    override suspend fun searchPresets(
        query: String,
        pageSize: Int,
        lastDocId: String?,
    ): Result<List<MarketPreset>> = runCatching {
        val firstKeyword = query.lowercase().trim().split(" ").firstOrNull() ?: ""
        var q = presetsCollection
            .whereArrayContains("searchKeywords", firstKeyword)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (lastDocId != null) {
            val lastDoc = presetsCollection.document(lastDocId).get().await()
            q = q.startAfter(lastDoc)
        }

        val results = q.get().await().documents
            .mapNotNull { it.toObject(MarketPresetDto::class.java)?.toDomain() }

        // 복수 키워드 클라이언트 필터링
        val extraKeywords = query.lowercase().trim().split(" ").drop(1).filter { it.isNotBlank() }
        if (extraKeywords.isEmpty()) {
            results
        } else {
            results.filter { preset ->
                extraKeywords.all { kw ->
                    preset.name.lowercase().contains(kw) ||
                        preset.tags.any { tag -> tag.lowercase().contains(kw) }
                }
            }
        }
    }

    override suspend fun uploadPreset(preset: MarketPreset): Result<String> = runCatching {
        val dto = preset.toDto()
        val docRef = if (preset.id.isEmpty()) {
            presetsCollection.document()
        } else {
            presetsCollection.document(preset.id)
        }
        docRef.set(dto).await()
        docRef.id
    }

    override suspend fun uploadImage(localUri: String, storagePath: String): Result<String> =
        runCatching {
            // 절대 파일 경로(/data/...): BitmapFactory.decodeFile()로 직접 읽기
            // content:// URI: ContentResolver를 통해 읽기
            val bytes = if (localUri.startsWith("/")) {
                ImageCompressor.compressToWebP(File(localUri))
            } else {
                ImageCompressor.compressToWebP(context, localUri.toUri())
            }
            val ref = storage.reference.child(storagePath)
            ref.putBytes(bytes).await()
            ref.downloadUrl.await().toString()
        }

    override suspend fun downloadImageToLocal(
        storageUrl: String,
        localPath: String,
    ): Result<String> = runCatching {
        val file = File(localPath)
        file.parentFile?.mkdirs()
        val ref = storage.getReferenceFromUrl(storageUrl)
        ref.getFile(file).await()
        file.absolutePath
    }

    override suspend fun toggleLike(presetId: String): Result<Boolean> = runCatching {
        val uid = auth.currentUser?.uid ?: error("로그인이 필요합니다")
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
                transaction.set(likeRef, mapOf("createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                transaction.update(presetRef, "likeCount", current + 1)
            }
        }.await()
        !exists // 좋아요 상태 반환 (true = 좋아요 추가됨)
    }

    override suspend fun incrementDownloadCount(presetId: String): Result<Unit> = runCatching {
        presetsCollection.document(presetId)
            .update("downloadCount", com.google.firebase.firestore.FieldValue.increment(1))
            .await()
    }
}
