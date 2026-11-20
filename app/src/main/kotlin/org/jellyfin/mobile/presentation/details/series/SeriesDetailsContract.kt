package org.jellyfin.mobile.presentation.details.series

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.domain.model.EpisodeSummary
import org.jellyfin.mobile.domain.model.MediaDetails
import org.jellyfin.mobile.domain.model.SeasonSummary

/**
 * MVI Contract for the dedicated Series Details Screen.
 * Strictly adheres to 03_series_details_screen.md and Obsidian 2.2.0 tokens.
 */

sealed interface SeriesIntent {
    data class LoadSeries(val seriesId: String) : SeriesIntent
    data object RetryLoad : SeriesIntent
    data class SelectSeason(val seasonNumber: Int) : SeriesIntent
    data class SelectSeasonById(val seasonId: String) : SeriesIntent
    data class PlayEpisode(val episodeId: String) : SeriesIntent
    data object ResumeNextUp : SeriesIntent
    data object PlayFromStart : SeriesIntent
    data class ToggleEpisodeWatched(val episodeId: String) : SeriesIntent
    data object ToggleSeriesFavorite : SeriesIntent
    data object ToggleSeriesWatched : SeriesIntent
    data class OpenActorFilmography(val personId: String, val personName: String) : SeriesIntent
    data object RefreshPlaybackState : SeriesIntent
    data object ShareSeries : SeriesIntent
}

typealias SeriesDetailsIntent = SeriesIntent

sealed interface SeriesUiState {
    data object Loading : SeriesUiState

    data class Success(
        val seriesDetails: MediaDetails,
        val nextUpEpisode: EpisodeSummary? = null,
        val seasons: ImmutableList<SeasonSummary> = persistentListOf(),
        val selectedSeasonNumber: Int = 1,
        val selectedSeasonId: String? = null,
        val episodesForSelectedSeason: ImmutableList<EpisodeSummary> = persistentListOf(),
        val castList: ImmutableList<CastMember> = seriesDetails.castMembers,
        val isFavorite: Boolean = seriesDetails.isFavorite,
        val isWatched: Boolean = false,
        val isLoadingEpisodes: Boolean = false
    ) : SeriesUiState {
        val series: MediaDetails get() = seriesDetails
        val allEpisodesWatched: Boolean
            get() = episodesForSelectedSeason.isNotEmpty() && episodesForSelectedSeason.all { it.isWatched }
    }

    data class Error(
        val message: String,
        val isNetworkError: Boolean = false
    ) : SeriesUiState
}

typealias SeriesDetailsUiState = SeriesUiState

sealed interface SeriesEffect {
    data class LaunchPlayer(val mediaId: String, val resumePositionMs: Long = 0L) : SeriesEffect
    data class ShowToast(val message: String) : SeriesEffect
    data class ShowSnackbar(val message: String) : SeriesEffect
    data class NavigateToActorFilmography(val personId: String, val personName: String) : SeriesEffect
    data class ShareSeriesContent(val title: String, val summary: String) : SeriesEffect
}

typealias SeriesDetailsEffect = SeriesEffect
