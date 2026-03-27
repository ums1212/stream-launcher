package org.comon.streamlauncher.data.datasource

interface MarketStorageDataSource {
    suspend fun uploadBytes(bytes: ByteArray, storagePath: String, contentType: String = "image/webp"): String
    /** getDownloadUrl 없이 업로드만 수행하고 gs:// 경로를 반환 (신고 이미지 등 앱에서 표시 불필요한 경우) */
    suspend fun uploadBytesGetPath(bytes: ByteArray, storagePath: String, contentType: String = "image/webp"): String
    suspend fun uploadFile(localFilePath: String, storagePath: String): String
    suspend fun downloadToFile(storageUrl: String, localPath: String): String
}
