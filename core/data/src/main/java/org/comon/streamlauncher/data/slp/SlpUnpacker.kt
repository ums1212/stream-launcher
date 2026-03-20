package org.comon.streamlauncher.data.slp

import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipInputStream

private const val MAX_ENTRIES = 50
private const val MAX_TOTAL_SIZE = 50L * 1024 * 1024  // 50 MB

/**
 * .slp 파일(ZIP) 추출 + manifest.json 파싱
 */
object SlpUnpacker {

    private val lenientJson = Json { ignoreUnknownKeys = true }

    /**
     * @param slpFile   추출할 .slp 파일
     * @param targetDir 이미지를 저장할 디렉토리 (보통 filesDir/market_presets/<id>)
     * @return manifest 와 "ZIP 내 상대경로 → 로컬 절대경로" 맵
     */
    fun unpack(slpFile: File, targetDir: File): Pair<SlpManifest, Map<String, String>> {
        targetDir.mkdirs()

        val extractedPaths = mutableMapOf<String, String>()
        var manifestBytes: ByteArray? = null
        var entryCount = 0
        var totalSize = 0L

        ZipInputStream(slpFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > MAX_ENTRIES) {
                    throw SecurityException("ZIP entry count exceeds limit ($MAX_ENTRIES)")
                }

                val sanitized = if (!entry.isDirectory) sanitizePath(entry.name) else null
                if (sanitized != null) {
                    if (sanitized == "manifest.json") {
                        val bytes = zis.readBytes()
                        totalSize += bytes.size
                        if (totalSize > MAX_TOTAL_SIZE) {
                            throw SecurityException("ZIP total size exceeds limit (${MAX_TOTAL_SIZE / 1024 / 1024} MB)")
                        }
                        manifestBytes = bytes
                    } else {
                        val outFile = File(targetDir, sanitized)
                        outFile.parentFile?.mkdirs()
                        var written = 0L
                        outFile.outputStream().buffered().use { out ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (zis.read(buffer).also { read = it } != -1) {
                                written += read
                                totalSize += read
                                if (totalSize > MAX_TOTAL_SIZE) {
                                    out.close()
                                    outFile.delete()
                                    extractedPaths.values.forEach { File(it).delete() }
                                    throw SecurityException("ZIP total size exceeds limit (${MAX_TOTAL_SIZE / 1024 / 1024} MB)")
                                }
                                out.write(buffer, 0, read)
                            }
                        }
                        extractedPaths[sanitized] = outFile.absolutePath
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val manifest = lenientJson.decodeFromString<SlpManifest>(
            manifestBytes?.toString(Charsets.UTF_8)
                ?: error("manifest.json이 .slp 파일에 없습니다")
        )

        return manifest to extractedPaths
    }

    /**
     * 경로 순회 공격(../) 차단.
     * null 반환 시 해당 엔트리를 건너뜀.
     */
    internal fun sanitizePath(entryName: String): String? {
        val normalized = entryName.replace('\\', '/')
        // 절대 경로 또는 .. 포함 시 거부
        if (normalized.startsWith("/")) return null
        if (normalized.split("/").any { it == ".." }) return null
        return normalized
    }
}
