package org.comon.streamlauncher.data.datasource

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMarketStorageDataSource @Inject constructor(
    private val storage: FirebaseStorage,
) : MarketStorageDataSource {

    override suspend fun uploadBytes(bytes: ByteArray, storagePath: String, contentType: String): String {
        val ref = storage.reference.child(storagePath)
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .build()
        ref.putBytes(bytes, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    override suspend fun uploadBytesGetPath(bytes: ByteArray, storagePath: String, contentType: String): String {
        val ref = storage.reference.child(storagePath)
        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .build()
        ref.putBytes(bytes, metadata).await()
        return "gs://${storage.reference.bucket}/$storagePath"
    }

    override suspend fun uploadFile(localFilePath: String, storagePath: String): String {
        val file = File(localFilePath)
        val ref = storage.reference.child(storagePath)
        val metadata = StorageMetadata.Builder()
            .setContentType("application/octet-stream")
            .build()
        ref.putFile(Uri.fromFile(file), metadata).await()
        return ref.downloadUrl.await().toString()
    }

    override suspend fun downloadToFile(storageUrl: String, localPath: String): String {
        validateStorageUrl(storageUrl)
        val file = File(localPath)
        file.parentFile?.mkdirs()
        val ref = storage.getReferenceFromUrl(storageUrl)
        ref.getFile(file).await()
        return file.absolutePath
    }

    private fun validateStorageUrl(url: String) {
        val bucket = storage.reference.bucket
        val expectedPrefixes = listOf(
            "gs://$bucket/",
            "https://firebasestorage.googleapis.com/v0/b/$bucket/",
        )
        require(expectedPrefixes.any { url.startsWith(it) }) {
            "Storage URL does not belong to the expected bucket"
        }
    }
}
