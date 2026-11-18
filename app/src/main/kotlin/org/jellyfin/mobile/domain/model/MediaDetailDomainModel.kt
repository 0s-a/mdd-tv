package org.jellyfin.mobile.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable domain model representing rich media details for Movie or Series.
 * Strictly adheres to docs/screens/03_MEDIA_DETAILS.md.
 */
data class MediaDetailDomainModel(
    val id: String,
    val title: String,
    val originalTitle: String? = null,
    val overview: String = "",
    val tagline: String? = null,
    val productionYear: Int? = null,
    val communityRating: Float? = null,
    val criticRating: Float? = null,
    val officialRating: String? = null,
    val genres: ImmutableList<String> = persistentListOf(),
    val backdropUrl: String? = null,
    val posterUrl: String? = null,
    val runtimeMs: Long = 0L,
    val runtimeFormatted: String? = null,
    val isFavorite: Boolean = false,
    val isWatched: Boolean = false,
    val resumePositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val contentType: ContentType = ContentType.Movie,
    val qualityBadges: ImmutableList<String> = persistentListOf(),
    // Technical Media Info (Infuse Inspector)
    val videoCodec: String? = null,
    val videoResolution: String? = null,
    val audioCodec: String? = null,
    val audioChannels: String? = null,
    val container: String? = null,
    val bitrateMbps: Float? = null,
    val fileSizeBytes: Long? = null,
    val isDirectPlay: Boolean = true
)
