package org.jellyfin.mobile.presentation.details.movie

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.domain.model.MediaDetails
import org.jellyfin.mobile.domain.model.MediaItemSummary

/**
 * MVI Contract for the Dedicated Mobile Movie Details Screen.
 * Strictly adheres to docs/current/screens/media_details/02_movie_details_screen.md.
 * Pure Kotlin data structures with zero emojis.
 */

@Immutable
data class MovieChapter(
    val id: String,
    val name: String,
    val startPositionMs: Long,
    val formattedTime: String,
    val imageUrl: String? = null
)

@Immutable
data class MovieAudioTrack(
    val index: Int,
    val title: String,
    val language: String? = null,
    val codec: String? = null,
    val channels: String? = null,
    val isDefault: Boolean = false,
    val isSelected: Boolean = false
)

@Immutable
data class MovieSubtitleTrack(
    val index: Int,
    val title: String,
    val language: String? = null,
    val isForced: Boolean = false,
    val isDefault: Boolean = false,
    val isSelected: Boolean = false
)

@Immutable
sealed interface MovieDownloadStatus {
    data object Idle : MovieDownloadStatus
    data class Downloading(val progress: Float) : MovieDownloadStatus
    data object Downloaded : MovieDownloadStatus
}

sealed interface MovieIntent {
    data class LoadMovie(val movieId: String) : MovieIntent
    data object RetryLoad : MovieIntent
    data object RefreshPlaybackState : MovieIntent
    data object Play : MovieIntent
    data object Resume : MovieIntent
    data object PlayFromStart : MovieIntent
    data class PlayChapter(val chapter: MovieChapter) : MovieIntent
    data object ToggleFavorite : MovieIntent
    data object ToggleWatched : MovieIntent
    data object OpenTrailer : MovieIntent
    data object ShareMovie : MovieIntent
    data object ToggleDownload : MovieIntent
    data object OpenAudioSubtitleSheet : MovieIntent
    data object DismissAudioSubtitleSheet : MovieIntent
    data class SelectAudioTrack(val track: MovieAudioTrack) : MovieIntent
    data class SelectSubtitleTrack(val track: MovieSubtitleTrack?) : MovieIntent
    data class OpenActorFilmography(val personId: String, val personName: String) : MovieIntent
}

sealed interface MovieUiState {
    data object Loading : MovieUiState

    data class Success(
        val movie: MediaDetails,
        val isFavorite: Boolean = movie.isFavorite,
        val isWatched: Boolean = false,
        val castList: ImmutableList<CastMember> = movie.castMembers,
        val chapters: ImmutableList<MovieChapter> = persistentListOf(),
        val audioTracks: ImmutableList<MovieAudioTrack> = persistentListOf(),
        val subtitleTracks: ImmutableList<MovieSubtitleTrack> = persistentListOf(),
        val selectedAudioTrack: MovieAudioTrack? = null,
        val selectedSubtitleTrack: MovieSubtitleTrack? = null,
        val downloadStatus: MovieDownloadStatus = MovieDownloadStatus.Idle,
        val isAudioSubtitleSheetVisible: Boolean = false,
        val similarMovies: ImmutableList<MediaItemSummary> = movie.similarItems
    ) : MovieUiState

    data class Error(
        val message: String,
        val isNetworkError: Boolean = false
    ) : MovieUiState
}

sealed interface MovieEffect {
    data class LaunchPlayer(val mediaId: String, val resumePositionMs: Long = 0L) : MovieEffect
    data class OpenTrailerPlayer(val trailerUrl: String) : MovieEffect
    data class NavigateToActorFilmography(val personId: String, val name: String) : MovieEffect
    data class ShowToast(val message: String) : MovieEffect
    data class ShowSnackbar(val message: String) : MovieEffect
    data class ShareMovie(val title: String, val shareUrl: String) : MovieEffect
    data class NavigateToMovieDetails(val movieId: String) : MovieEffect
}

fun formatChapterTimestamp(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3600L
    return if (hours > 0L) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
