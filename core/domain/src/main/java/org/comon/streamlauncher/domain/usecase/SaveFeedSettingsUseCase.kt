package org.comon.streamlauncher.domain.usecase

import org.comon.streamlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

class SaveFeedSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(
        chzzkChannelId: String,
        youtubeChannelId: String,
    ) {
        settingsRepository.setChzzkChannelId(chzzkChannelId)
        settingsRepository.setYoutubeChannelId(youtubeChannelId)
    }
}
