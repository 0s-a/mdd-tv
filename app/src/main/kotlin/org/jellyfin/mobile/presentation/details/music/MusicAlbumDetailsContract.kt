package org.jellyfin.mobile.presentation.details.music

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * MVI Contract for the Mobile Music Album Details Screen.
 * Strictly adheres to docs/current/screens/media_details/05_music_album_details_screen.md.
 * Pure Kotlin data structures with absolute zero emojis.
 */

@Immutable
data class MusicTrackUiModel(
    val id: String,
    val trackNumber: Int,
    val title: String,
    val artist: String? = null,
    val durationTicks: Long? = null,
    val durationFormatted: String = "00:00",
    val runTimeMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val audioQualityBadge: String? = null
)

@Immutable
data class MusicAlbumUiModel(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val year: Int? = null,
    val coverUrl: String? = null,
    val genres: ImmutableList<String> = persistentListOf(),
    val trackCount: Int = 0,
    val totalDurationText: String = "",
    val qualityBadge: String = "Hi-Res",
    val overview: String? = null,
    val isFavorite: Boolean = false
)

@Immutable
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val isActive: Boolean = false
)

@Immutable
data class LyricsUiModel(
    val trackId: String,
    val trackTitle: String,
    val artist: String = "",
    val lines: ImmutableList<LyricLine> = persistentListOf(),
    val plainLyrics: String = "",
    val isSynced: Boolean = false
)

@Immutable
data class MiniPlayerUiModel(
    val trackId: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val progressFraction: Float = 0f
)

sealed interface MusicAlbumIntent {
    data class LoadAlbum(val albumId: String) : MusicAlbumIntent
    data object RetryLoad : MusicAlbumIntent
    data object PlayAll : MusicAlbumIntent
    data object ShuffleAll : MusicAlbumIntent
    data class PlayTrack(val trackId: String) : MusicAlbumIntent
    data object TogglePlayPause : MusicAlbumIntent
    data object NextTrack : MusicAlbumIntent
    data object PreviousTrack : MusicAlbumIntent
    data class SeekTo(val positionMs: Long) : MusicAlbumIntent
    data object ToggleFavoriteAlbum : MusicAlbumIntent
    data class ToggleFavoriteTrack(val trackId: String) : MusicAlbumIntent
    data class OpenLyrics(val trackId: String? = null) : MusicAlbumIntent
    data object DismissLyrics : MusicAlbumIntent
    data class OpenTrackOptions(val trackId: String) : MusicAlbumIntent
    data object DismissTrackOptions : MusicAlbumIntent
    data object RefreshPlaybackState : MusicAlbumIntent
}

sealed interface MusicAlbumUiState {
    data object Loading : MusicAlbumUiState

    data class Success(
        val album: MusicAlbumUiModel,
        val tracks: ImmutableList<MusicTrackUiModel> = persistentListOf(),
        val currentPlayingTrackId: String? = null,
        val isPlaying: Boolean = false,
        val playbackPositionMs: Long = 0L,
        val miniPlayer: MiniPlayerUiModel? = null,
        val isFavorite: Boolean = album.isFavorite,
        val showLyricsSheet: Boolean = false,
        val lyrics: LyricsUiModel? = null,
        val isLoadingLyrics: Boolean = false,
        val selectedTrackForOptions: MusicTrackUiModel? = null,
        val isShuffled: Boolean = false
    ) : MusicAlbumUiState

    data class Error(
        val message: String,
        val isNetworkError: Boolean = false
    ) : MusicAlbumUiState
}

sealed interface MusicAlbumEffect {
    data class LaunchAudioPlayer(
        val trackId: String,
        val albumId: String,
        val positionMs: Long = 0L
    ) : MusicAlbumEffect
    data class ShowToast(val message: String) : MusicAlbumEffect
    data class ShowSnackbar(val message: String) : MusicAlbumEffect
    data object NavigateBack : MusicAlbumEffect
    data class OpenArtistDetails(val artistId: String, val artistName: String) : MusicAlbumEffect
    data class ShareAlbum(val title: String, val artist: String) : MusicAlbumEffect
}
