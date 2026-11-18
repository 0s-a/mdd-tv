package org.jellyfin.mobile.domain.repository

import org.jellyfin.mobile.domain.model.EpisodeSummary
import org.jellyfin.mobile.domain.model.MediaDetails

/**
 * Domain repository contract for fetching media details, season episodes, and playback status.
 * Pure Kotlin with zero android.* dependencies.
 */
interface MediaDetailsRepository {

    /**
     * Fetches comprehensive details for a movie or series by ID, including
     * metadata, cast members, seasons/episodes (for series), and similar items.
     */
    suspend fun getMediaDetails(itemId: String): Result<MediaDetails>

    /**
     * Fetches episodes for a specific series season.
     */
    suspend fun getEpisodesForSeason(seriesId: String, seasonId: String): Result<List<EpisodeSummary>>

    /**
     * Toggles favorite status for an item on the server.
     */
    suspend fun toggleFavorite(itemId: String, isFavorite: Boolean): Result<Boolean>

    /**
     * Marks an item as played or unplayed on the server.
     */
    suspend fun markAsWatched(itemId: String, isWatched: Boolean): Result<Boolean>
}
