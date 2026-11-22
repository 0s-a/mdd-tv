package org.jellyfin.mobile.data.repository

import androidx.collection.LruCache
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

    private val pageCache = LruCache<String, LibraryPage>(50)

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
        limit: Int,
        isHierarchical: Boolean
    ): Result<LibraryPage> = getLibraryItems(
        parentId = parentId,
        filter = filter,
        letter = letter,
        startIndex = startIndex,
        limit = limit,
        isHierarchical = isHierarchical,
        forceRefresh = false
    )

    override suspend fun getLibraryItems(
        parentId: String,
        filter: LibraryFilter,
        letter: Char?,
        startIndex: Int,
        limit: Int,
        isHierarchical: Boolean,
        forceRefresh: Boolean
    ): Result<LibraryPage> = withContext(ioDispatcher) {
        val cacheKey = "$parentId-${filter.sortBy}-$letter-$startIndex-$limit-$isHierarchical"
        if (!forceRefresh) {
            val cached = pageCache[cacheKey]
            if (cached != null) {
                return@withContext Result.success(cached)
            }
        }

        runCatching {
            val userId = requireUserId()

            val (sortBy, sortOrder) = when (filter.sortBy) {
                LibrarySortBy.DATE_ADDED -> "DateCreated,SortName" to "Descending"
                LibrarySortBy.NAME -> "SortName" to "Ascending"
                LibrarySortBy.RATING -> "CommunityRating,SortName" to "Descending"
                LibrarySortBy.PREMIERE_DATE -> "PremiereDate,SortName" to "Descending"
            }

            val nameStartsWith = letter?.toString()
            val shouldRecurse = if (isHierarchical) false else true

            val itemTypes = if (shouldRecurse) {
                "Movie,Series,Episode,Video,Audio"
            } else {
                "Folder,Series,Movie,Episode,Video,Audio"
            }

            val response = apiService.getLibraryItems(
                userId = userId,
                parentId = parentId,
                sortBy = sortBy,
                sortOrder = sortOrder,
                includeItemTypes = itemTypes,
                filters = null,
                startIndex = startIndex,
                limit = limit,
                nameStartsWith = nameStartsWith,
                recursive = shouldRecurse
            )

            val domainItems = response.items?.map { it.toMediaItemSummary() } ?: emptyList()
            val totalCount = response.totalRecordCount ?: domainItems.size

            val page = LibraryPage(
                items = domainItems.toImmutableList(),
                totalCount = totalCount,
                startIndex = response.startIndex ?: startIndex
            )
            pageCache.put(cacheKey, page)
            page
        }
    }

    override fun clearCache() {
        pageCache.evictAll()
    }
}
