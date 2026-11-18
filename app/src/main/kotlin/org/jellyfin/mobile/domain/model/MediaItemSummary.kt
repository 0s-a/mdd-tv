package org.jellyfin.mobile.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable domain representation of a media item summary (movies, series, episodes).
 * Pure Kotlin with zero Android dependencies for domain layer purity.
 */
data class MediaItemSummary(
    val id: String,
    val name: String,
    val type: String,
    val productionYear: Int? = null,
    val communityRating: Float? = null,
    val runTimeTicks: Long? = null,
    val playbackPositionPercentage: Double = 0.0,
    val backdropImageUrl: String? = null,
    val posterImageUrl: String? = null,
    val isPlayed: Boolean = false,
    val overview: String? = null,
    val durationText: String? = null,
    val genres: ImmutableList<String> = persistentListOf(),
    val qualityBadges: ImmutableList<String> = persistentListOf(),
    val isFavorite: Boolean = false
) {
    // Convenience accessors for existing UI components
    val title: String get() = name
    val backdropUrl: String? get() = backdropImageUrl
    val posterUrl: String? get() = posterImageUrl
    val isWatched: Boolean get() = isPlayed
}
