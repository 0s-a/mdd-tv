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
     * Retrieves recently added items.
     */
    suspend fun getLatestItems(limit: Int = 16, parentId: String? = null): Result<List<MediaItemSummary>>
}
