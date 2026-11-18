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
    val id: String = "",
    val name: String = "",
    val indexNumber: Int? = null,
    val episodeCount: Int = 0,
    val watchedCount: Int = 0
) {
    constructor(
        seasonNumber: Int,
        seasonName: String,
        episodeCount: Int,
        watchedCount: Int = 0,
        id: String = seasonNumber.toString()
    ) : this(
        id = id,
        name = seasonName,
        indexNumber = seasonNumber,
        episodeCount = episodeCount,
        watchedCount = watchedCount
    )

    val seasonNumber: Int get() = indexNumber ?: 1
    val seasonName: String get() = name
}

typealias SeasonInfo = SeasonSummary

data class EpisodeSummary(
    val id: String,
    val name: String,
    val seasonId: String? = null,
    val seasonNumber: Int = 1,
    val indexNumber: Int? = null,
    val overview: String? = null,
    val runTimeTicks: Long? = null,
    val playbackPositionPercentage: Float = 0f,
    val isPlayed: Boolean = false,
    val imageUrl: String? = null,
    val isDownloaded: Boolean = false
) {
    val title: String get() = name
    val episodeNumber: Int get() = indexNumber ?: 1
    val runtimeMs: Long get() = (runTimeTicks ?: 0L) / 10_000L
    val thumbnailUrl: String? get() = imageUrl
    val isWatched: Boolean get() = isPlayed
    val progressPercentage: Float get() = (playbackPositionPercentage / 100f).coerceIn(0f, 1f)
    val runtimeFormatted: String? get() = durationText
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

typealias EpisodeDomainModel = EpisodeSummary

fun EpisodeSummary.toEpisodeDomainModel(seasonNumber: Int = 1): EpisodeSummary =
    this.copy(seasonNumber = seasonNumber)

data class MediaTechBadges(
    val resolution: String = "1080p",
    val videoCodec: String = "H.264",
    val dynamicRange: String = "SDR",
    val audioCodec: String = "AAC",
    val audioChannels: String = "2.0",
    val container: String? = null,
    val bitrateMbps: Float? = null,
    val fileSizeBytes: Long? = null,
    val isDirectPlay: Boolean = true
)
