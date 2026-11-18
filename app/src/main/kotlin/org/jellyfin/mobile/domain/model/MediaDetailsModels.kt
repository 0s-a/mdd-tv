package org.jellyfin.mobile.domain.model

/**
 * Immutable domain models for media details.
 * Pure Kotlin with zero android.* dependencies.
 */

enum class ContentType {
    Movie,
    Series,
    Episode
}

data class CastMember(
    val id: String,
    val name: String,
    val role: String? = null,
    val type: String? = null,
    val imageUrl: String? = null
) {
    // Backward-compatibility property for UI components referencing avatarUrl
    val avatarUrl: String? get() = imageUrl
}

data class SeasonSummary(
    val id: String,
    val name: String,
    val indexNumber: Int? = null,
    val episodeCount: Int = 0
)

data class EpisodeSummary(
    val id: String,
    val name: String,
    val seasonId: String? = null,
    val indexNumber: Int? = null,
    val overview: String? = null,
    val runTimeTicks: Long? = null,
    val playbackPositionPercentage: Float = 0f,
    val isPlayed: Boolean = false,
    val imageUrl: String? = null
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

data class MediaTechBadges(
    val resolution: String = "1080p",
    val videoCodec: String = "H.264",
    val dynamicRange: String = "SDR",
    val audioCodec: String = "AAC",
    val audioChannels: String = "2.0",
    val isDirectPlay: Boolean = true
)

data class SeasonInfo(
    val seasonNumber: Int,
    val seasonName: String,
    val episodeCount: Int,
    val watchedCount: Int = 0
)

data class EpisodeDomainModel(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String = "",
    val runtimeMs: Long = 0L,
    val runtimeFormatted: String? = null,
    val thumbnailUrl: String? = null,
    val progressPercentage: Float = 0f, // 0.0 to 1.0
    val isWatched: Boolean = false,
    val isDownloaded: Boolean = false
)

fun EpisodeSummary.toEpisodeDomainModel(seasonNumber: Int = 1): EpisodeDomainModel {
    val totalMs = (runTimeTicks ?: 0L) / 10_000L
    return EpisodeDomainModel(
        id = id,
        seasonNumber = seasonNumber,
        episodeNumber = indexNumber ?: 1,
        title = name,
        overview = overview.orEmpty(),
        runtimeMs = totalMs,
        runtimeFormatted = durationText,
        thumbnailUrl = imageUrl,
        progressPercentage = (playbackPositionPercentage / 100f).coerceIn(0f, 1f),
        isWatched = isPlayed,
        isDownloaded = false
    )
}
