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
    val overview: String = "",
    val tagline: String? = null,
    val productionYear: Int? = null,
    val communityRating: Float? = null,
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
    val isFavorite: Boolean = false
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
}

/**
 * Extension bridge to MediaDetailDomainModel for existing UI components.
 */
fun MediaDetails.toMediaDetailDomainModel(): MediaDetailDomainModel {
    val totalMs = (runTimeTicks ?: 0L) / 10_000L
    val resumeMs = (totalMs * (playbackPositionPercentage / 100.0)).toLong()
    return MediaDetailDomainModel(
        id = id,
        title = title,
        overview = overview,
        tagline = tagline,
        productionYear = productionYear,
        communityRating = communityRating,
        genres = genres,
        backdropUrl = backdropUrl,
        posterUrl = posterUrl,
        runtimeMs = totalMs,
        runtimeFormatted = durationText,
        isFavorite = isFavorite,
        isWatched = playbackPositionPercentage >= 95f,
        resumePositionMs = resumeMs,
        totalDurationMs = totalMs,
        contentType = if (isMovie) ContentType.Movie else ContentType.Series,
        videoCodec = techBadges.videoCodec,
        videoResolution = techBadges.resolution,
        audioCodec = techBadges.audioCodec,
        audioChannels = techBadges.audioChannels,
        isDirectPlay = techBadges.isDirectPlay
    )
}
