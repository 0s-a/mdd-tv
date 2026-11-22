package org.jellyfin.mobile.domain.repository

import kotlinx.collections.immutable.ImmutableList
import org.jellyfin.mobile.domain.model.LibraryFilter
import org.jellyfin.mobile.domain.model.MediaItemSummary

/**
 * Paginated library item result container.
 * Pure Kotlin with zero Android dependencies.
 */
data class LibraryPage(
    val items: ImmutableList<MediaItemSummary>,
    val totalCount: Int,
    val startIndex: Int
)

/**
 * Pure Kotlin repository contract for querying library contents with filtering,
 * alphabetical sorting, and pagination.
 */
interface LibraryRepository {

    suspend fun getLibraryItems(
        parentId: String,
        filter: LibraryFilter = LibraryFilter(),
        letter: Char? = null,
        startIndex: Int = 0,
        limit: Int = 100,
        isHierarchical: Boolean = true
    ): Result<LibraryPage>

    suspend fun getLibraryItems(
        parentId: String,
        filter: LibraryFilter = LibraryFilter(),
        letter: Char? = null,
        startIndex: Int = 0,
        limit: Int = 100,
        isHierarchical: Boolean = true,
        forceRefresh: Boolean = false
    ): Result<LibraryPage> = getLibraryItems(
        parentId = parentId,
        filter = filter,
        letter = letter,
        startIndex = startIndex,
        limit = limit,
        isHierarchical = isHierarchical
    )

    fun clearCache() {}
}
