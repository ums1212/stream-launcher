package org.comon.streamlauncher.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import org.comon.streamlauncher.data.datasource.MarketStorageDataSource
import org.comon.streamlauncher.data.util.ImageCompressor
import org.comon.streamlauncher.domain.repository.SuggestionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val storageDataSource: MarketStorageDataSource,
) : SuggestionRepository {

    override suspend fun submitSuggestion(
        email: String,
        body: String,
        imageUrl: String?,
        appVersion: String,
        deviceInfo: String,
    ): Result<Unit> = runCatching {
        val data = buildMap<String, Any> {
            put("email", email)
            put("body", body)
            put("appVersion", appVersion)
            put("deviceInfo", deviceInfo)
            put("createdAt", FieldValue.serverTimestamp())
            put("status", "pending")
            imageUrl?.let { put("imageUrl", it) }
        }
        firestore.collection("suggestions").document().set(data).await()
    }

    override suspend fun uploadSuggestionImage(localUri: String, storagePath: String): Result<String> =
        runCatching {
            val bytes = ImageCompressor.compressToWebPWithMaxSize(
                context = context,
                uri = localUri.toUri(),
                maxWidth = 1080,
                quality = 80,
                maxSizeBytes = 10L * 1024 * 1024,
            )
            storageDataSource.uploadBytesGetPath(bytes, storagePath)
        }
}
