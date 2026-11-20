package org.jellyfin.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.domain.model.GenreCategory
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.domain.model.SearchResultSummary

/**
 * Domain repository contract for instant multi-domain search and discovery.
 * Pure Kotlin with zero android.* dependencies.
 */
interface SearchRepository {

    /**
     * Executes universal search on the Jellyfin server across movies, series, episodes, and persons,
     * with optional multi-genre filtering.
     */
    suspend fun search(query: String, genres: Set<String> = emptySet()): Result<SearchResultSummary>

    /**
     * Dynamically retrieves all available genres from the Jellyfin server.
     */
    suspend fun getAvailableGenres(): Result<List<String>>

    /**
     * Dynamically retrieves a random spotlight media item from the Jellyfin server for idle exploration.
     */
    suspend fun getRandomSpotlight(): Result<MediaItemSummary?>

    /**
     * Retrieves media items filtered by a specific genre.
     */
    suspend fun getItemsByGenre(genreName: String): Result<List<MediaItemSummary>>

    /**
     * Flow of recent search queries ordered by most recent first.
     */
    fun getRecentQueries(): Flow<List<String>>

    /**
     * Records a new query into the search history.
     */
    suspend fun addRecentQuery(query: String)

    /**
     * Removes an individual query from the search history.
     */
    suspend fun removeRecentQuery(query: String)

    /**
     * Clears all search history.
     */
    suspend fun clearRecentQueries()

    /**
     * Returns curated genres for the idle exploration grid. Deprecated in favor of getAvailableGenres().
     */
    fun getExploreGenres(): List<GenreCategory> = emptyList()
}
