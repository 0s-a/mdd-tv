package org.jellyfin.mobile.presentation.search

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.domain.model.MediaItemSummary

/**
 * MVI Contract for the Search & Explore Transparent Modal Window.
 * 100% dynamic server data, multi-genre selection, and mobile-first gestures.
 */

sealed interface SearchIntent {
    data class UpdateQuery(val query: String) : SearchIntent
    data object ClearQuery : SearchIntent
    data class ToggleGenre(val genre: String) : SearchIntent
    data class RemoveGenre(val genre: String) : SearchIntent
    data object ClearGenres : SearchIntent
    data object OpenGenrePicker : SearchIntent
    data object CloseGenrePicker : SearchIntent
    data class SelectRecentQuery(val query: String) : SearchIntent
    data class DeleteRecentQuery(val query: String) : SearchIntent
    data object ClearHistory : SearchIntent
    data object ClearSearchHistory : SearchIntent
    data class SelectGenre(val genre: String) : SearchIntent
    data class QuickPlay(val mediaId: String) : SearchIntent
    data class OpenDetails(val mediaId: String) : SearchIntent
    data class OpenMediaDetails(val mediaId: String) : SearchIntent
    data class OpenPersonProfile(val personId: String, val name: String) : SearchIntent
    data object DismissSearch : SearchIntent
}

sealed interface SearchUiState {
    val selectedGenres: ImmutableSet<String>
    val isGenrePickerOpen: Boolean

    data class Idle(
        val recentQueries: ImmutableList<String> = persistentListOf(),
        val availableGenres: ImmutableList<String> = persistentListOf(),
        override val selectedGenres: ImmutableSet<String> = persistentSetOf(),
        val spotlightItem: MediaItemSummary? = null,
        override val isGenrePickerOpen: Boolean = false
    ) : SearchUiState

    data class Searching(
        val query: String = "",
        override val selectedGenres: ImmutableSet<String> = persistentSetOf(),
        override val isGenrePickerOpen: Boolean = false
    ) : SearchUiState

    data class Success(
        val query: String,
        override val selectedGenres: ImmutableSet<String> = persistentSetOf(),
        val topMatch: MediaItemSummary? = null,
        val mediaResults: ImmutableList<MediaItemSummary> = persistentListOf(),
        val peopleResults: ImmutableList<CastMember> = persistentListOf(),
        val totalCount: Int = 0,
        val availableGenres: ImmutableList<String> = persistentListOf(),
        override val isGenrePickerOpen: Boolean = false
    ) : SearchUiState

    data class Empty(
        val query: String,
        override val selectedGenres: ImmutableSet<String> = persistentSetOf(),
        override val isGenrePickerOpen: Boolean = false
    ) : SearchUiState

    data class Error(
        val message: String,
        override val selectedGenres: ImmutableSet<String> = persistentSetOf(),
        override val isGenrePickerOpen: Boolean = false
    ) : SearchUiState
}

sealed interface SearchEffect {
    data class NavigateToDetails(val mediaId: String) : SearchEffect
    data class LaunchPlayer(val mediaId: String, val resumePositionMs: Long = 0L) : SearchEffect
    data class NavigateToPersonFilmography(val personId: String, val name: String) : SearchEffect
    data class ShowToast(val message: String) : SearchEffect
    data object Dismiss : SearchEffect
}
