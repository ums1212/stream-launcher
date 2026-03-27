package org.comon.streamlauncher.data.datasource

interface MarketStorageDataSource {
    suspend fun uploadBytes(bytes: ByteArray, storagePath: String, contentType: String = "image/webp"): String
    suspend fun uploadFile(localFilePath: String, storagePath: String): String
    suspend fun downloadToFile(storageUrl: String, localPath: String): String
}
