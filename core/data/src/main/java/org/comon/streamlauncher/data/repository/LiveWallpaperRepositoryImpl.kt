package org.comon.streamlauncher.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.comon.streamlauncher.data.local.room.livewallpaper.LiveWallpaperDao
import org.comon.streamlauncher.data.local.room.livewallpaper.LiveWallpaperEntity
import org.comon.streamlauncher.data.local.room.livewallpaper.toDomain
import org.comon.streamlauncher.domain.model.LiveWallpaper
import org.comon.streamlauncher.domain.repository.LiveWallpaperRepository
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap

@Singleton
class LiveWallpaperRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: LiveWallpaperDao,
) : LiveWallpaperRepository {

    override fun getAllLiveWallpapers(): Flow<List<LiveWallpaper>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getLiveWallpaperById(id: Int): LiveWallpaper? =
        withContext(Dispatchers.IO) { dao.getById(id)?.toDomain() }

    override suspend fun saveLiveWallpaper(name: String, sourceUri: String): LiveWallpaper =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "live_wallpapers").also { it.mkdirs() }
            val ext = resolveExtension(sourceUri)
            val fileName = "lw_${System.currentTimeMillis()}.$ext"
            val destFile = File(dir, fileName)

            // 파일 복사
            copyUriToFile(sourceUri, destFile)

            // 썸네일 생성
            val thumbnailUri = generateThumbnail(destFile, ext)

            val entity = LiveWallpaperEntity(
                name = name,
                fileUri = destFile.absolutePath,
                thumbnailUri = thumbnailUri,
                createdAt = System.currentTimeMillis(),
            )
            val insertedId = dao.insert(entity).toInt()
            entity.copy(id = insertedId).toDomain()  // DAO is non-suspend, called inside withContext(IO)
        }

    override suspend fun deleteLiveWallpaper(id: Int): Unit =
        withContext(Dispatchers.IO) {
            val entity = dao.getById(id)
            dao.deleteById(id)  // non-suspend DAO method
            entity?.let {
                File(it.fileUri).takeIf { f -> f.exists() }?.delete()
                it.thumbnailUri?.let { t -> File(t).takeIf { f -> f.exists() }?.delete() }
            }
        }

    private fun resolveExtension(sourceUri: String): String {
        if (!sourceUri.startsWith("content://")) {
            val candidate = sourceUri.substringAfterLast('.', "").lowercase()
            if (candidate.isNotEmpty() && !candidate.contains('/')) return candidate
        }
        return when (context.contentResolver.getType(sourceUri.toUri())) {
            "image/gif" -> "gif"
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "video/webm" -> "webm"
            "video/mp4" -> "mp4"
            else -> "mp4"
        }
    }

    private fun copyUriToFile(sourceUri: String, destFile: File) {
        if (sourceUri.startsWith("/")) {
            File(sourceUri).copyTo(destFile, overwrite = true)
        } else {
            val uri = sourceUri.toUri()
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
        }
    }

    private val staticImageExtensions = setOf("jpg", "jpeg", "png", "webp")

    private fun generateThumbnail(videoFile: File, ext: String): String? {
        val thumbDir = File(context.filesDir, "live_wallpapers/thumbnails").also { it.mkdirs() }
        val thumbFile = File(thumbDir, "${videoFile.nameWithoutExtension}_thumb.webp")
        return try {
            val bitmap: Bitmap? = when {
                ext == "gif" -> {
                    val source = ImageDecoder.createSource(videoFile)
                    val drawable = ImageDecoder.decodeDrawable(source)
                    val bmp = createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                    )
                    val canvas = android.graphics.Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp
                }
                ext in staticImageExtensions -> {
                    val source = ImageDecoder.createSource(videoFile)
                    ImageDecoder.decodeBitmap(source)
                }
                else -> {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(videoFile.absolutePath)
                        retriever.getFrameAtTime(0)
                    } finally {
                        retriever.release()
                    }
                }
            }
            bitmap?.let {
                FileOutputStream(thumbFile).use { out ->
                    val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                    it.compress(format, 80, out)
                }
                it.recycle()
                thumbFile.absolutePath
            }
        } catch (_: Exception) {
            null
        }
    }
}
