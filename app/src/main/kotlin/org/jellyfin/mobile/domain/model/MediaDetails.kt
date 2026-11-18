package org.jellyfin.mobile.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable domain model representing comprehensive media details for a Movie or Series.
 * Strictly adheres to docs/screens/03_MEDIA_DETAILS.md.
 * Pure Kotlin with zero android.* dependencies.
 */
data class MediaDetails(
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
    val runTimeTicks: Long? = null,
    val backdropUrl: String? = null,
    val posterUrl: String? = null,
    val isMovie: Boolean = true,
    val techBadges: MediaTechBadges = MediaTechBadges(),
    val castMembers: ImmutableList<CastMember> = persistentListOf(),
    val seasons: ImmutableList<SeasonSummary> = persistentListOf(),
    val episodes: ImmutableList<EpisodeSummary> = persistentListOf(),
    val similarItems: ImmutableList<MediaItemSummary> = persistentListOf(),
    val playbackPositionPercentage: Float = 0f,
    val isFavorite: Boolean = false,
    val qualityBadges: ImmutableList<String> = persistentListOf()
) {
    val durationText: String?
        get() = runTimeTicks?.let { ticks ->
            val totalMinutes = (ticks / 10_000_000L / 60L).toInt()
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            when {
                hours > 0 -> "${hours}س ${mins}د"
                mins > 0 -> "${mins}د"
                else -> null
            }
        }

    val runtimeMs: Long get() = (runTimeTicks ?: 0L) / 10_000L
    val totalDurationMs: Long get() = runtimeMs
    val resumePositionMs: Long get() = (runtimeMs * (playbackPositionPercentage / 100.0)).toLong()
    val isWatched: Boolean get() = playbackPositionPercentage >= 95f
    val runtimeFormatted: String? get() = durationText
    val contentType: ContentType get() = if (isMovie) ContentType.Movie else ContentType.Series
    val videoCodec: String? get() = techBadges.videoCodec
    val videoResolution: String? get() = techBadges.resolution
    val audioCodec: String? get() = techBadges.audioCodec
    val audioChannels: String? get() = techBadges.audioChannels
    val container: String? get() = techBadges.container
    val bitrateMbps: Float? get() = techBadges.bitrateMbps
    val fileSizeBytes: Long? get() = techBadges.fileSizeBytes
    val isDirectPlay: Boolean get() = techBadges.isDirectPlay
}

typealias MediaDetailDomainModel = MediaDetails

/**
 * Extension bridge returning self for unified domain model.
 */
fun MediaDetails.toMediaDetailDomainModel(): MediaDetails = this
