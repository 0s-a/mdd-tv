package org.jellyfin.mobile.presentation.player

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jellyfin.mobile.core.player.external.ExternalPlayerPayload

/**
 * MVI Contract for Screen 04 - Advanced Video Player.
 * Strictly implements docs/screens/04_ADVANCED_PLAYER.md and docs/NON_FUNCTIONAL_REQUIREMENTS.md.
 */

enum class AspectRatioMode {
    Fit,
    Fill,
    Stretch,
    Original
}

enum class PlaybackMethod {
    DirectPlay,
    DirectStream,
    Transcode
}

@Immutable
data class SubtitleOption(
    val index: Int,
    val name: String,
    val language: String? = null,
    val isDefault: Boolean = false,
    val trackGroupIndex: Int = -1,
    val trackIndex: Int = -1
)

@Immutable
data class AudioOption(
    val index: Int,
    val name: String,
    val language: String? = null,
    val codec: String? = null,
    val trackGroupIndex: Int = -1,
    val trackIndex: Int = -1
)

@Immutable
data class MediaChapter(
    val name: String,
    val startPositionMs: Long
)

// ══════════════════════════════════════════════════════════════════
// 1. INTENTS (Events dispatched from UI gestures and controls)
// ══════════════════════════════════════════════════════════════════

sealed interface PlayerIntent {
    data class InitializePlayer(val mediaId: String, val startPositionMs: Long = 0L) : PlayerIntent
    data object Play : PlayerIntent
    data object Pause : PlayerIntent
    data object TogglePlayPause : PlayerIntent
    data class SeekTo(val positionMs: Long) : PlayerIntent
    data class FastSeekBy(val deltaMs: Long) : PlayerIntent
    data class SetVolume(val volume: Float) : PlayerIntent            // 0.0f → 2.0f (with boost)
    data class SetBrightness(val brightness: Float) : PlayerIntent    // 0.0f → 1.0f
    data class SetPlaybackSpeed(val speed: Float) : PlayerIntent      // 0.5f → 2.0f
    data class SetAspectRatio(val mode: AspectRatioMode) : PlayerIntent
    data class SelectSubtitleTrack(val track: SubtitleOption?) : PlayerIntent
    data class AdjustSubtitleOffset(val offsetMs: Long) : PlayerIntent
    data class SelectAudioTrack(val track: AudioOption) : PlayerIntent
    data object ToggleControlsLock : PlayerIntent
    data object ToggleHudVisibility : PlayerIntent
    data object ToggleActionsModal : PlayerIntent
    data class SetActionsModalVisibility(val visible: Boolean) : PlayerIntent
    data class SetEqualizerBand(val bandIndex: Int, val gainMb: Int) : PlayerIntent
    data object ApplyVocalBoost : PlayerIntent
    data class SetBitstreamPassthrough(val enabled: Boolean) : PlayerIntent
    data class SetAudioDelay(val delayMs: Long) : PlayerIntent
    data object PlayNextEpisode : PlayerIntent
    data object EnterPictureInPicture : PlayerIntent
    data object LaunchExternalPlayer : PlayerIntent
    data object ReleasePlayer : PlayerIntent
}

// ══════════════════════════════════════════════════════════════════
// 2. UI STATE (Unified immutable snapshot for Compose)
// ══════════════════════════════════════════════════════════════════

@Immutable
data class PlayerUiState(
    val mediaId: String = "",
    val mediaTitle: String = "",
    val mediaSubtitle: String = "",
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val isBuffering: Boolean = true,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val volume: Float = 1.0f,               // 0.0f to 2.0f (software boost > 1.0f)
    val brightness: Float = 0.5f,           // 0.0f to 1.0f
    val playbackSpeed: Float = 1.0f,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.Fit,
    val isControlsLocked: Boolean = false,
    val isHudVisible: Boolean = true,
    val playbackMethod: PlaybackMethod = PlaybackMethod.DirectPlay,
    val videoCodecInfo: String = "HEVC 4K",
    val audioCodecInfo: String = "TrueHD 7.1",
    val availableSubtitles: ImmutableList<SubtitleOption> = persistentListOf(),
    val selectedSubtitle: SubtitleOption? = null,
    val subtitleOffsetMs: Long = 0L,
    val availableAudioTracks: ImmutableList<AudioOption> = persistentListOf(),
    val selectedAudioTrack: AudioOption? = null,
    val chapters: ImmutableList<MediaChapter> = persistentListOf(),
    val currentChapter: MediaChapter? = null,
    val nextEpisodeId: String? = null,
    val errorMessage: String? = null,
    val currentSubtitleText: String? = null,
    val isInPictureInPicture: Boolean = false,
    val isActionsModalVisible: Boolean = false,
    val isBitstreamPassthrough: Boolean = false,
    val audioBands: ImmutableList<Float> = persistentListOf(0f, 0f, 0f, 0f, 0f),
    val audioDelayMs: Long = 0L
) {
    val bufferedPercentage: Float
        get() = if (totalDurationMs > 0) (bufferedPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f

    val progressPercentage: Float
        get() = if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) else 0f

    val formattedCurrentTime: String
        get() = formatPlayerTime(currentPositionMs)

    val formattedTotalTime: String
        get() = formatPlayerTime(totalDurationMs)

    val formattedRemainingTime: String
        get() = "-" + formatPlayerTime((totalDurationMs - currentPositionMs).coerceAtLeast(0L))
}

/**
 * Format milliseconds into HH:MM:SS or MM:SS timestamp string.
 */
fun formatPlayerTime(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

// ══════════════════════════════════════════════════════════════════
// 3. SIDE EFFECTS (One-time single-shot events)
// ══════════════════════════════════════════════════════════════════

sealed interface PlayerEffect {
    data class ShowToast(val message: String) : PlayerEffect
    data object TriggerHapticTick : PlayerEffect
    data object TriggerHapticHeavy : PlayerEffect
    data object TriggerHapticDouble : PlayerEffect
    data object ClosePlayerScreen : PlayerEffect
    data object EnterPictureInPicture : PlayerEffect
    data class LaunchExternalPlayer(val payload: ExternalPlayerPayload) : PlayerEffect
}
