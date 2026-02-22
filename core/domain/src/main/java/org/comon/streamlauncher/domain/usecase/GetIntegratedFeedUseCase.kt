package org.comon.streamlauncher.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.comon.streamlauncher.domain.model.FeedItem
import org.comon.streamlauncher.domain.repository.FeedRepository
import javax.inject.Inject

class GetIntegratedFeedUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
) {
    operator fun invoke(
        rssUrl: String,
        youtubeChannelId: String,
    ): Flow<Result<List<FeedItem>>> =
        feedRepository.getIntegratedFeed(rssUrl, youtubeChannelId)
}
