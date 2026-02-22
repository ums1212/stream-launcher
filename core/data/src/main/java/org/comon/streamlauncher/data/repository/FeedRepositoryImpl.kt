package org.comon.streamlauncher.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.comon.streamlauncher.data.util.DateParser
import org.comon.streamlauncher.domain.model.FeedItem
import org.comon.streamlauncher.domain.model.LiveStatus
import org.comon.streamlauncher.domain.repository.FeedRepository
import org.comon.streamlauncher.network.NetworkConstants
import org.comon.streamlauncher.network.api.ChzzkService
import org.comon.streamlauncher.network.api.RssFeedApi
import org.comon.streamlauncher.network.api.YouTubeService
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Serializable
private data class FeedItemDto(
    val type: String,
    val title: String,
    val dateMillis: Long,
    val link: String = "",
    val source: String = "",
    val thumbnailUrl: String = "",
    val videoLink: String = "",
)

@Singleton
class FeedRepositoryImpl @Inject constructor(
    private val chzzkService: ChzzkService,
    private val youTubeService: YouTubeService,
    private val rssFeedApi: RssFeedApi,
    @Named("feed_cache") private val cacheDataStore: DataStore<Preferences>,
) : FeedRepository {

    private val feedCacheKey = stringPreferencesKey("feed_cache_json")
    private val handleToChannelIdCache = HashMap<String, String>()
    private val json = Json { ignoreUnknownKeys = true }

    override fun getLiveStatus(channelId: String): Flow<Result<LiveStatus>> = flow {
        if (channelId.isEmpty()) {
            emit(Result.success(LiveStatus(false, "", 0, "", "")))
            return@flow
        }
        val url = "https://api.chzzk.naver.com/service/v3/channels/$channelId/live-detail"
        val response = chzzkService.getLiveDetail(url)
        val content = response.content
        val status = LiveStatus(
            isLive = content?.status == "OPEN",
            title = content?.liveTitle ?: "",
            viewerCount = content?.concurrentUserCount ?: 0,
            thumbnailUrl = content?.liveImageUrl ?: "",
            channelId = channelId,
        )
        emit(Result.success(status))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(Dispatchers.IO)

    override fun getIntegratedFeed(
        rssUrl: String,
        youtubeChannelId: String,
    ): Flow<Result<List<FeedItem>>> = flow {
        // 1) 캐시 먼저 emit
        val cachedJson = cacheDataStore.data.first()[feedCacheKey]
        if (!cachedJson.isNullOrEmpty()) {
            emit(Result.success(parseFeedItemsJson(cachedJson)))
        }

        // 2) 병렬 네트워크 호출
        val (rssItems, youtubeItems) = coroutineScope {
            val rssDeferred = async {
                runCatching { fetchRssItems(rssUrl) }.getOrElse { emptyList() }
            }
            val ytDeferred = async {
                runCatching { fetchYoutubeItems(youtubeChannelId) }.getOrElse { emptyList() }
            }
            rssDeferred.await() to ytDeferred.await()
        }

        val combined = (rssItems + youtubeItems).sortedByDescending { it.dateMillis }

        // 3) 캐시 갱신
        cacheDataStore.edit { prefs ->
            prefs[feedCacheKey] = encodeFeedItemsJson(combined)
        }

        emit(Result.success(combined))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchRssItems(rssUrl: String): List<FeedItem.NoticeItem> {
        if (rssUrl.isEmpty()) return emptyList()
        val response = rssFeedApi.getRssFeed(rssUrl)
        return response.channel.items.map { item ->
            FeedItem.NoticeItem(
                title = item.title,
                dateMillis = DateParser.parseRfc822(item.pubDate),
                link = item.link,
                source = response.channel.title,
            )
        }
    }

    private suspend fun fetchYoutubeItems(youtubeChannelId: String): List<FeedItem.VideoItem> {
        if (youtubeChannelId.isEmpty()) return emptyList()

        val resolvedId = if (youtubeChannelId.startsWith("@")) {
            handleToChannelIdCache.getOrPut(youtubeChannelId) {
                val channelResponse = youTubeService.getChannelByHandle(
                    url = "https://www.googleapis.com/youtube/v3/channels",
                    part = "id",
                    handle = youtubeChannelId,
                    apiKey = NetworkConstants.YOUTUBE_API_KEY,
                )
                channelResponse.items.firstOrNull()?.id ?: return emptyList()
            }
        } else {
            youtubeChannelId
        }

        val searchResponse = youTubeService.searchVideos(
            url = "https://www.googleapis.com/youtube/v3/search",
            part = "snippet",
            channelId = resolvedId,
            order = "date",
            type = "video",
            maxResults = 10,
            apiKey = NetworkConstants.YOUTUBE_API_KEY,
        )

        return searchResponse.items.map { item ->
            FeedItem.VideoItem(
                title = item.snippet.title,
                dateMillis = DateParser.parseIso8601(item.snippet.publishedAt),
                thumbnailUrl = item.snippet.thumbnails.high?.url
                    ?: item.snippet.thumbnails.default?.url
                    ?: "",
                videoLink = "https://www.youtube.com/watch?v=${item.id.videoId}",
            )
        }
    }

    private fun parseFeedItemsJson(jsonStr: String): List<FeedItem> {
        return try {
            val dtos: List<FeedItemDto> = json.decodeFromString(jsonStr)
            dtos.mapNotNull { dto ->
                when (dto.type) {
                    "notice" -> FeedItem.NoticeItem(dto.title, dto.dateMillis, dto.link, dto.source)
                    "video" -> FeedItem.VideoItem(dto.title, dto.dateMillis, dto.thumbnailUrl, dto.videoLink)
                    else -> null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeFeedItemsJson(items: List<FeedItem>): String {
        val dtos = items.map { item ->
            when (item) {
                is FeedItem.NoticeItem -> FeedItemDto(
                    type = "notice",
                    title = item.title,
                    dateMillis = item.dateMillis,
                    link = item.link,
                    source = item.source,
                )
                is FeedItem.VideoItem -> FeedItemDto(
                    type = "video",
                    title = item.title,
                    dateMillis = item.dateMillis,
                    thumbnailUrl = item.thumbnailUrl,
                    videoLink = item.videoLink,
                )
            }
        }
        return json.encodeToString(dtos)
    }
}
