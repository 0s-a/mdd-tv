package org.jellyfin.mobile.core.player.external

import android.net.Uri

/**
 * Data structures for external video player integration.
 * Strictly adheres to docs/thinking/planning/07_external_player_integration.md and ADR 0001.
 */
data class ExternalSubtitle(
    val url: String,
    val name: String,
    val language: String = "ar",
    val isDefault: Boolean = false
)

data class ExternalPlayerPayload(
    val mediaId: String,
    val title: String,
    val streamUrl: String,
    val resumePositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val subtitles: List<ExternalSubtitle> = emptyList()
)

data class ExternalPlaybackResult(
    val mediaId: String,
    val positionMs: Long,
    val durationMs: Long,
    val isCompleted: Boolean
)
