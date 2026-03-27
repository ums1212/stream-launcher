package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Inject

class UploadReportImageUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    suspend operator fun invoke(localUri: String, reporterUid: String): Result<String> {
        val timestamp = System.currentTimeMillis()
        val storagePath = "report-images/${reporterUid}_$timestamp"
        return repository.uploadImageGetPath(localUri, storagePath)
    }
}
