package org.jellyfin.mobile.presentation.details

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.domain.model.EpisodeSummary
import org.jellyfin.mobile.domain.model.MediaDetails
import org.jellyfin.mobile.domain.model.SeasonSummary

/**
 * MVI Contract for the Media Details Screen.
 * Strictly adheres to docs/screens/03_MEDIA_DETAILS.md Section 7.
 */

sealed interface MediaDetailsIntent {
    data class LoadMedia(val mediaId: String) : MediaDetailsIntent
    data class SelectSeason(val seasonNumber: Int) : MediaDetailsIntent
    data class SelectSeasonById(val seasonId: String) : MediaDetailsIntent
    data object ToggleFavorite : MediaDetailsIntent
    data object ToggleWatched : MediaDetailsIntent
    data object PlayMedia : MediaDetailsIntent
    data object ResumeMedia : MediaDetailsIntent
    data object PlayFromStart : MediaDetailsIntent
    data class PlayEpisode(val episodeId: String) : MediaDetailsIntent
    data class ToggleEpisodeWatched(val episodeId: String) : MediaDetailsIntent
    data class DownloadEpisode(val episodeId: String) : MediaDetailsIntent
    data class OpenActorFilmography(val personId: String, val personName: String) : MediaDetailsIntent
    data object OpenTechInspector : MediaDetailsIntent
    data object DismissTechInspector : MediaDetailsIntent
    data object OpenTrailer : MediaDetailsIntent
    data object RetryLoad : MediaDetailsIntent
}

sealed interface MediaDetailsUiState {
    data object Loading : MediaDetailsUiState

    data class Success(
        val mediaDetails: MediaDetails,
        val nextUpEpisode: EpisodeSummary? = null,
        val seasons: ImmutableList<SeasonSummary> = persistentListOf(),
        val selectedSeasonNumber: Int = 1,
        val selectedSeasonId: String? = null,
        val episodesForSelectedSeason: ImmutableList<EpisodeSummary> = persistentListOf(),
        val castList: ImmutableList<CastMember> = mediaDetails.castMembers,
        val isInspectorOpen: Boolean = false,
        val isFavorite: Boolean = mediaDetails.isFavorite,
        val isWatched: Boolean = false,
        val isLoadingEpisodes: Boolean = false,
        val mediaItem: MediaDetails = mediaDetails,
        val episodesForSeason: ImmutableList<EpisodeSummary> = episodesForSelectedSeason
    ) : MediaDetailsUiState

    data class Error(
        val message: String,
        val isNetworkError: Boolean = false
    ) : MediaDetailsUiState
}

sealed interface MediaDetailsEffect {
    data class LaunchPlayer(val mediaId: String, val resumePositionMs: Long = 0L) : MediaDetailsEffect
    data class NavigateToDetails(val mediaId: String) : MediaDetailsEffect
    data class NavigateToActorFilmography(val personId: String, val name: String) : MediaDetailsEffect
    data class ShowSnackbar(val message: String) : MediaDetailsEffect
    data class ShowToast(val message: String) : MediaDetailsEffect
    data class OpenTrailerPlayer(val trailerUrl: String) : MediaDetailsEffect
    data object ShareMedia : MediaDetailsEffect
}
