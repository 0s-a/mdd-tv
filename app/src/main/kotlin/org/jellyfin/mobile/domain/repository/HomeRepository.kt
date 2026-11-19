package org.jellyfin.mobile.domain.repository

import org.jellyfin.mobile.domain.model.HomeFeed
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.MediaItemSummary

/**
 * Pure Kotlin repository contract for Home Dashboard data aggregation.
 */
interface HomeRepository {

    /**
     * Aggregates and returns the full HomeFeed (libraries, hero spotlight, resume items, and latest items).
     */
    suspend fun getHomeFeed(forceRefresh: Boolean = false): Result<HomeFeed>

    /**
     * Retrieves user libraries/views dynamically from the Jellyfin server.
     */
    suspend fun getLibraries(): Result<List<LibraryFolder>>

    /**
     * Retrieves items in progress for resume / continue watching.
     */
    suspend fun getResumeItems(limit: Int = 12): Result<List<MediaItemSummary>>

    /**
     * Retrieves next up episodes for in-progress series.
     */
    suspend fun getNextUpItems(limit: Int = 16): Result<List<MediaItemSummary>> = Result.success(emptyList())

    /**
     * Retrieves recently added items.
     */
    suspend fun getLatestItems(limit: Int = 16, parentId: String? = null): Result<List<MediaItemSummary>>

    /**
     * Marks an item as watched on the Jellyfin server.
     */
    suspend fun markItemWatched(itemId: String): Result<Unit>

    /**
     * Dismisses an item from Continue Watching by resetting its playback position on the server.
     */
    suspend fun dismissContinueWatching(itemId: String): Result<Unit>
}
