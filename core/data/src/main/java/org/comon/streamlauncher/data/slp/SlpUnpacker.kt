package org.comon.streamlauncher.data.slp

import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipInputStream

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

        ZipInputStream(slpFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val sanitized = sanitizePath(entry.name)
                        ?: run { zis.closeEntry(); entry = zis.nextEntry; return@use }

                    if (sanitized == "manifest.json") {
                        manifestBytes = zis.readBytes()
                    } else {
                        val outFile = File(targetDir, sanitized)
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().buffered().use { zis.copyTo(it) }
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
