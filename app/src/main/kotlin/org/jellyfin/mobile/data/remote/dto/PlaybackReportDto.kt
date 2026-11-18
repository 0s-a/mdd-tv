package org.jellyfin.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data transfer objects for Jellyfin playback telemetry and session reporting.
 * Adheres strictly to docs/screens/04_ADVANCED_PLAYER.md and docs/SERVER_FEATURES_AND_PROCEDURES.md.
 */

@Serializable
data class PlaybackStartDto(
    @SerialName("ItemId") val itemId: String,
    @SerialName("PositionTicks") val positionTicks: Long? = 0L,
    @SerialName("MediaSourceId") val mediaSourceId: String? = null,
    @SerialName("AudioStreamIndex") val audioStreamIndex: Int? = null,
    @SerialName("SubtitleStreamIndex") val subtitleStreamIndex: Int? = null,
    @SerialName("PlayMethod") val playMethod: String = "DirectPlay",
    @SerialName("CanSeek") val canSeek: Boolean = true,
    @SerialName("IsPaused") val isPaused: Boolean = false
)

@Serializable
data class PlaybackProgressDto(
    @SerialName("ItemId") val itemId: String,
    @SerialName("PositionTicks") val positionTicks: Long? = 0L,
    @SerialName("IsPaused") val isPaused: Boolean = false,
    @SerialName("CanSeek") val canSeek: Boolean = true,
    @SerialName("PlayMethod") val playMethod: String = "DirectPlay",
    @SerialName("EventName") val eventName: String? = null,
    @SerialName("MediaSourceId") val mediaSourceId: String? = null,
    @SerialName("AudioStreamIndex") val audioStreamIndex: Int? = null,
    @SerialName("SubtitleStreamIndex") val subtitleStreamIndex: Int? = null
)

@Serializable
data class PlaybackStopDto(
    @SerialName("ItemId") val itemId: String,
    @SerialName("PositionTicks") val positionTicks: Long? = 0L,
    @SerialName("MediaSourceId") val mediaSourceId: String? = null
)
