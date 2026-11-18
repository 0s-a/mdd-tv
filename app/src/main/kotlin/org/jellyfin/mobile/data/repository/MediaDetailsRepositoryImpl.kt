package org.jellyfin.mobile.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.mapper.toEpisodeSummary
import org.jellyfin.mobile.data.mapper.toMediaDetails
import org.jellyfin.mobile.data.mapper.toMediaItemSummary
import org.jellyfin.mobile.data.mapper.toSeasonSummary
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.model.EpisodeSummary
import org.jellyfin.mobile.domain.model.MediaDetails
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.MediaDetailsRepository

/**
 * Concrete implementation of MediaDetailsRepository.
 * Coordinates fetching item metadata, seasons, episodes, and similar recommendations concurrently.
 */
class MediaDetailsRepositoryImpl(
    private val apiService: JellyfinApiService,
    private val sessionManager: SessionManager,
    private val handshakeRepository: HandshakeRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MediaDetailsRepository {

    private suspend fun requireUserId(): String {
        val current = sessionManager.getUserId() ?: handshakeRepository.currentSession.value?.userId
        if (!current.isNullOrBlank()) return current

        val handshakeResult = handshakeRepository.performHandshake()
        return handshakeResult.getOrThrow().userId
    }

    override suspend fun getMediaDetails(itemId: String): Result<MediaDetails> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()

            // 1. Fetch base media item details
            val itemDto = apiService.getItemDetails(userId = userId, itemId = itemId)
            val isSeries = itemDto.type.equals("Series", ignoreCase = true)

            // 2. Concurrently fetch seasons/episodes (if series) and similar media recommendations
            val seasonsDeferred = async {
                if (isSeries) {
                    runCatching {
                        apiService.getSeasons(seriesId = itemId, userId = userId).items
                            ?.map { it.toSeasonSummary() }
                            ?: emptyList()
                    }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
            }

            val episodesDeferred = async {
                if (isSeries) {
                    runCatching {
                        apiService.getEpisodes(seriesId = itemId, userId = userId).items
                            ?.map { it.toEpisodeSummary() }
                            ?: emptyList()
                    }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
            }

            val similarDeferred = async {
                runCatching {
                    apiService.getSimilarItems(itemId = itemId, userId = userId, limit = 12).items
                        ?.map { it.toMediaItemSummary() }
                        ?: emptyList()
                }.getOrDefault(emptyList())
            }

            val seasons = seasonsDeferred.await()
            val episodes = episodesDeferred.await()
            val similar = similarDeferred.await()

            itemDto.toMediaDetails(
                seasons = seasons,
                episodes = episodes,
                similarItems = similar
            )
        }
    }

    override suspend fun getEpisodesForSeason(
        seriesId: String,
        seasonId: String
    ): Result<List<EpisodeSummary>> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            val response = apiService.getEpisodes(
                seriesId = seriesId,
                userId = userId,
                seasonId = seasonId
            )
            response.items?.map { it.toEpisodeSummary() } ?: emptyList()
        }
    }

    override suspend fun toggleFavorite(itemId: String, isFavorite: Boolean): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            if (isFavorite) {
                apiService.markFavorite(userId = userId, itemId = itemId)
                true
            } else {
                apiService.removeFavorite(userId = userId, itemId = itemId)
                false
            }
        }
    }

    override suspend fun markAsWatched(itemId: String, isWatched: Boolean): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            if (isWatched) {
                apiService.markPlayed(userId = userId, itemId = itemId)
                true
            } else {
                apiService.markUnplayed(userId = userId, itemId = itemId)
                false
            }
        }
    }
}
