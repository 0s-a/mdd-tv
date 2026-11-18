package org.jellyfin.mobile.domain.model

/**
 * Immutable domain model for resume / continue watching items.
 * Contains playback position, remaining duration, and thumbnail data.
 */
data class ContinueWatchingItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val seriesName: String? = null,
    val episodeTitle: String? = null,
    val backdropUrl: String? = null,
    val posterUrl: String? = null,
    val progressPercentage: Float = 0f, // 0.0f to 1.0f
    val remainingMinutes: Int = 0,
    val resumePositionMs: Long = 0L,
    val totalDurationMs: Long = 0L
)
