package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.LiveStatus
import org.comon.streamlauncher.domain.repository.FeedRepository
import javax.inject.Inject

class GetYoutubeLiveStatusUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
) {
    operator fun invoke(channelId: String): Flow<Result<LiveStatus>> =
        feedRepository.getYoutubeLiveStatus(channelId)
}
