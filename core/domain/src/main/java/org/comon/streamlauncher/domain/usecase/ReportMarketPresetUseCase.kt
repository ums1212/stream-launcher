package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.MarketPresetRepository
import javax.inject.Inject

class ReportMarketPresetUseCase @Inject constructor(
    private val repository: MarketPresetRepository,
) {
    suspend operator fun invoke(
        reporterUid: String,
        reporterDisplayName: String,
        presetId: String,
        presetName: String,
        presetAuthorUid: String,
        presetAuthorDisplayName: String,
        reason: String,
        imageUrl: String? = null,
    ): Result<Unit> = repository.reportPreset(
        reporterUid = reporterUid,
        reporterDisplayName = reporterDisplayName,
        presetId = presetId,
        presetName = presetName,
        presetAuthorUid = presetAuthorUid,
        presetAuthorDisplayName = presetAuthorDisplayName,
        reason = reason,
        imageUrl = imageUrl,
    )
}
