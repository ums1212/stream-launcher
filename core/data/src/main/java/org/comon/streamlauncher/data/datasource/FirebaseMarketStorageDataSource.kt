package org.comon.streamlauncher.data.datasource

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMarketStorageDataSource @Inject constructor(
    private val storage: FirebaseStorage,
) : MarketStorageDataSource {

    override suspend fun uploadBytes(bytes: ByteArray, storagePath: String): String {
        val ref = storage.reference.child(storagePath)
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    override suspend fun uploadFile(localFilePath: String, storagePath: String): String {
        val file = File(localFilePath)
        val ref = storage.reference.child(storagePath)
        ref.putFile(Uri.fromFile(file)).await()
        return ref.downloadUrl.await().toString()
    }

    override suspend fun downloadToFile(storageUrl: String, localPath: String): String {
        val file = File(localPath)
        file.parentFile?.mkdirs()
        val ref = storage.getReferenceFromUrl(storageUrl)
        ref.getFile(file).await()
        return file.absolutePath
    }
}
