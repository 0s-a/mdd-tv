package org.jellyfin.mobile.presentation.search

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.domain.model.GenreCategory
import org.jellyfin.mobile.domain.model.MediaItemSummary

/**
 * MVI Contract for the Search & Explore Screen.
 * Strictly adheres to docs/screens/05_SEARCH_AND_EXPLORE.md Section 5.
 */

sealed interface SearchIntent {
    data class UpdateQuery(val query: String) : SearchIntent
    data object ClearQuery : SearchIntent
    data class SelectRecentQuery(val query: String) : SearchIntent
    data class DeleteRecentQuery(val query: String) : SearchIntent
    data object ClearHistory : SearchIntent
    data object ClearSearchHistory : SearchIntent
    data class SelectGenre(val genre: String) : SearchIntent
    data class QuickPlay(val mediaId: String) : SearchIntent
    data class OpenDetails(val mediaId: String) : SearchIntent
    data class OpenMediaDetails(val mediaId: String) : SearchIntent
    data class OpenPersonProfile(val personId: String, val name: String) : SearchIntent
}

sealed interface SearchUiState {
    data class Idle(
        val recentQueries: ImmutableList<String> = persistentListOf(),
        val genres: ImmutableList<GenreCategory> = persistentListOf()
    ) : SearchUiState

    data object Searching : SearchUiState

    data class Success(
        val query: String,
        val topMatch: MediaItemSummary? = null,
        val mediaResults: ImmutableList<MediaItemSummary> = persistentListOf(),
        val peopleResults: ImmutableList<CastMember> = persistentListOf(),
        val totalCount: Int = 0
    ) : SearchUiState

    data class Empty(val query: String) : SearchUiState

    data class Error(val message: String) : SearchUiState
}

sealed interface SearchEffect {
    data class NavigateToDetails(val mediaId: String) : SearchEffect
    data class LaunchPlayer(val mediaId: String, val resumePositionMs: Long = 0L) : SearchEffect
    data class NavigateToPersonFilmography(val personId: String, val name: String) : SearchEffect
    data class ShowToast(val message: String) : SearchEffect
}
