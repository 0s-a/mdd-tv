package org.jellyfin.mobile.data.repository

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.mapper.toMediaItemSummary
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.model.LibraryFilter
import org.jellyfin.mobile.domain.model.LibrarySortBy
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.LibraryPage
import org.jellyfin.mobile.domain.repository.LibraryRepository

/**
 * Concrete implementation of LibraryRepository interfacing with Jellyfin REST API.
 */
class LibraryRepositoryImpl(
    private val apiService: JellyfinApiService,
    private val sessionManager: SessionManager,
    private val handshakeRepository: HandshakeRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LibraryRepository {

    private suspend fun requireUserId(): String {
        val current = sessionManager.getUserId() ?: handshakeRepository.currentSession.value?.userId
        if (!current.isNullOrBlank()) return current

        val handshakeResult = handshakeRepository.performHandshake()
        return handshakeResult.getOrThrow().userId
    }

    override suspend fun getLibraryItems(
        parentId: String,
        filter: LibraryFilter,
        letter: Char?,
        startIndex: Int,
        limit: Int
    ): Result<LibraryPage> = withContext(ioDispatcher) {
        runCatching {
            val userId = requireUserId()

            val (sortBy, sortOrder) = when (filter.sortBy) {
                LibrarySortBy.DATE_ADDED -> "DateCreated,SortName" to "Descending"
                LibrarySortBy.NAME -> "SortName" to "Ascending"
                LibrarySortBy.RATING -> "CommunityRating,SortName" to "Descending"
                LibrarySortBy.PREMIERE_DATE -> "PremiereDate,SortName" to "Descending"
            }

            val filters = if (filter.isUnwatchedOnly) "IsUnplayed" else null
            val nameStartsWith = letter?.toString()

            val response = apiService.getLibraryItems(
                userId = userId,
                parentId = parentId,
                sortBy = sortBy,
                sortOrder = sortOrder,
                includeItemTypes = "Movie,Series",
                filters = filters,
                startIndex = startIndex,
                limit = limit,
                nameStartsWith = nameStartsWith,
                recursive = true
            )

            var domainItems = response.items?.map { it.toMediaItemSummary() } ?: emptyList()

            // Client-side quality/rating filter refinements
            if (filter.is4kOnly) {
                domainItems = domainItems.filter { item ->
                    item.qualityBadges.any { badge -> badge.contains("4K", ignoreCase = true) }
                }
            }

            if (filter.minRating != null) {
                domainItems = domainItems.filter { item ->
                    (item.communityRating ?: 0f) >= filter.minRating
                }
            }

            LibraryPage(
                items = domainItems.toImmutableList(),
                totalCount = response.totalRecordCount ?: domainItems.size,
                startIndex = response.startIndex ?: startIndex
            )
        }
    }
}
