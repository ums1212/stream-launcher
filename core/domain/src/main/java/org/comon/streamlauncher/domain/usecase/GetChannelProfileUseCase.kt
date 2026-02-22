package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.ChannelProfile
import org.comon.streamlauncher.domain.repository.FeedRepository
import javax.inject.Inject

class GetChannelProfileUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
) {
    operator fun invoke(youtubeChannelId: String): Flow<Result<ChannelProfile>> =
        feedRepository.getChannelProfile(youtubeChannelId)
}
