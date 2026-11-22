package org.jellyfin.mobile.data.repository

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.mapper.toDomain
import org.jellyfin.mobile.data.mapper.toMediaItemSummary
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.model.HomeFeed
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.HomeRepository

/**
 * Concrete implementation of HomeRepository. Aggregates libraries, resume items,
 * and latest additions concurrently from Jellyfin REST API.
 */
class HomeRepositoryImpl(
    private val apiService: JellyfinApiService,
    private val sessionManager: SessionManager,
    private val handshakeRepository: HandshakeRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HomeRepository {

    private suspend fun requireUserId(): String {
        val current = sessionManager.getUserId() ?: handshakeRepository.currentSession.value?.userId
        if (!current.isNullOrBlank()) return current

        val handshakeResult = handshakeRepository.performHandshake()
        return handshakeResult.getOrThrow().userId
    }

    override suspend fun getLibraries(): Result<List<LibraryFolder>> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            val response = apiService.getUserViews(userId)
            response.toDomain()
        }
    }

    override suspend fun getResumeItems(limit: Int): Result<List<MediaItemSummary>> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            val response = apiService.getResumeItems(userId = userId, limit = limit)
            response.items?.map { it.toMediaItemSummary() } ?: emptyList()
        }
    }

    override suspend fun getNextUpItems(limit: Int): Result<List<MediaItemSummary>> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            val response = apiService.getNextUpEpisodes(userId = userId, limit = limit)
            response.items?.map { it.toMediaItemSummary() } ?: emptyList()
        }
    }

    override suspend fun getLatestItems(limit: Int, parentId: String?): Result<List<MediaItemSummary>> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            val response = apiService.getLatestItems(userId = userId, parentId = parentId, limit = limit)
            response.map { it.toMediaItemSummary() }
        }
    }

    @Volatile
    private var cachedHomeFeed: HomeFeed? = null
    @Volatile
    private var lastFeedFetchTime: Long = 0L
    private val cacheTtlMs: Long = 5 * 60 * 1000L

    override suspend fun getHomeFeed(forceRefresh: Boolean): Result<HomeFeed> = withContext(ioDispatcher) {
        val cached = cachedHomeFeed
        if (!forceRefresh && cached != null && (System.currentTimeMillis() - lastFeedFetchTime < cacheTtlMs)) {
            return@withContext Result.success(cached)
        }

        runCatching {
            val userId = requireUserId()

            coroutineScope {
                val librariesDeferred = async { apiService.getUserViews(userId).toDomain() }
                val spotlightDeferred = async {
                    try {
                        withTimeoutOrNull(8_000L) {
                            apiService.getSpotlightItems(userId = userId, limit = 5)
                                .items?.map { it.toMediaItemSummary() }
                        } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val resumeDeferred = async {
                    apiService.getResumeItems(userId = userId, limit = 12).items?.map { it.toMediaItemSummary() } ?: emptyList()
                }
                val nextUpDeferred = async {
                    try {
                        withTimeoutOrNull(10_000L) {
                            apiService.getNextUpEpisodes(userId = userId, limit = 16)
                                .items?.map { it.toMediaItemSummary() }
                        } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val latestDeferred = async {
                    apiService.getLatestItems(userId = userId, limit = 16).map { it.toMediaItemSummary() }
                }

                val libraries = librariesDeferred.await()
                val resumeItems = resumeDeferred.await()
                val nextUpItems = nextUpDeferred.await()
                val latestItems = latestDeferred.await()
                val spotlight = spotlightDeferred.await().ifEmpty {
                    val fallback = resumeItems.take(5).ifEmpty { latestItems.take(5) }
                    fallback
                }

                val feed = HomeFeed(
                    libraries = libraries.toImmutableList(),
                    heroItems = spotlight.toImmutableList(),
                    resumeItems = resumeItems.toImmutableList(),
                    nextUpItems = nextUpItems.toImmutableList(),
                    latestItems = latestItems.toImmutableList()
                )
                cachedHomeFeed = feed
                lastFeedFetchTime = System.currentTimeMillis()
                feed
            }
        }
    }

    override suspend fun markItemWatched(itemId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            apiService.markPlayed(userId = userId, itemId = itemId)
            Unit
        }
    }

    override suspend fun dismissContinueWatching(itemId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            apiService.markUnplayed(userId = userId, itemId = itemId)
            Unit
        }
    }
}
