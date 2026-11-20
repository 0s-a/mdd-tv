package org.jellyfin.mobile.data.repository

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.mapper.toMediaItemSummary
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.domain.model.GenreCategory
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.domain.model.SearchResultSummary
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.SearchRepository

/**
 * Concrete implementation of SearchRepository.
 * Handles server-side multi-domain querying, dynamic session token fallback,
 * and maintains recent queries history.
 */
class SearchRepositoryImpl(
    private val apiService: JellyfinApiService,
    private val sessionManager: SessionManager,
    private val handshakeRepository: HandshakeRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SearchRepository {

    private val _recentQueries = MutableStateFlow<List<String>>(emptyList())

    private suspend fun requireUserId(): String {
        val current = sessionManager.getUserId() ?: handshakeRepository.currentSession.value?.userId
        if (!current.isNullOrBlank()) return current

        val handshakeResult = handshakeRepository.performHandshake()
        return handshakeResult.getOrThrow().userId
    }

    override suspend fun search(query: String, genres: Set<String>): Result<SearchResultSummary> = withContext(ioDispatcher) {
        runCatching {
            val trimmed = query.trim()
            if (trimmed.isBlank() && genres.isEmpty()) {
                return@runCatching SearchResultSummary(query = "")
            }

            val userId = requireUserId()
            val genresParam = if (genres.isNotEmpty()) genres.joinToString(",") else null
            val response = apiService.searchMedia(
                userId = userId,
                searchTerm = trimmed.ifBlank { null },
                genres = genresParam,
                includeItemTypes = "Movie,Series,Episode,Person",
                recursive = true,
                limit = 30
            )

            val rawItems = response.items ?: emptyList()

            // Separate media and people items
            val mediaItems = mutableListOf<MediaItemSummary>()
            val peopleItems = mutableListOf<CastMember>()

            rawItems.forEach { itemDto ->
                if (itemDto.type.equals("Person", ignoreCase = true)) {
                    val personId = itemDto.id.orEmpty()
                    val img = if (personId.isNotBlank()) {
                        JellyfinApiService.buildPrimaryImageUrl(personId, maxWidth = 300, maxHeight = 300)
                    } else null

                    peopleItems.add(
                        CastMember(
                            id = personId,
                            name = itemDto.name ?: "شخصية",
                            role = "طاقم العمل",
                            type = "Person",
                            imageUrl = img
                        )
                    )
                } else {
                    mediaItems.add(itemDto.toMediaItemSummary())
                }
            }

            // Identify top match only when a search query is present
            val topMatch = if (trimmed.isNotBlank()) {
                mediaItems.firstOrNull {
                    it.name.equals(trimmed, ignoreCase = true)
                } ?: mediaItems.firstOrNull()
            } else {
                null
            }

            val remainingMedia = if (topMatch != null) {
                mediaItems.filterNot { it.id == topMatch.id }
            } else {
                mediaItems
            }

            SearchResultSummary(
                query = trimmed,
                topMatch = topMatch,
                mediaResults = remainingMedia.toImmutableList(),
                peopleResults = peopleItems.toImmutableList(),
                totalCount = response.totalRecordCount ?: (mediaItems.size + peopleItems.size)
            )
        }
    }

    override suspend fun getAvailableGenres(): Result<List<String>> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            val response = apiService.getGenres(userId = userId, recursive = true)
            response.items?.mapNotNull { it.name }?.filter { it.isNotBlank() } ?: emptyList()
        }
    }

    override suspend fun getRandomSpotlight(): Result<MediaItemSummary?> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            val response = apiService.getSpotlightItems(userId = userId, limit = 1)
            response.items?.firstOrNull()?.toMediaItemSummary()
        }
    }

    override suspend fun getItemsByGenre(genreName: String): Result<List<MediaItemSummary>> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()
            val response = apiService.getItemsByGenre(
                userId = userId,
                genres = genreName,
                includeItemTypes = "Movie,Series",
                limit = 50
            )
            response.items?.map { it.toMediaItemSummary() } ?: emptyList()
        }
    }

    override fun getRecentQueries(): Flow<List<String>> = _recentQueries.asStateFlow()

    override suspend fun addRecentQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _recentQueries.value.filterNot { it.equals(trimmed, ignoreCase = true) }
        _recentQueries.value = listOf(trimmed) + current.take(9)
    }

    override suspend fun removeRecentQuery(query: String) {
        _recentQueries.value = _recentQueries.value.filterNot { it.equals(query, ignoreCase = true) }
    }

    override suspend fun clearRecentQueries() {
        _recentQueries.value = emptyList()
    }

    override fun getExploreGenres(): List<GenreCategory> {
        return GenreCategory.getDefaultExploreGenres()
    }
}
