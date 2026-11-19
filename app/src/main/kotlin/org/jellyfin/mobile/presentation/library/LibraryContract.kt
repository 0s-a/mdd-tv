package org.jellyfin.mobile.presentation.library

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.jellyfin.mobile.domain.model.LibraryFilter
import org.jellyfin.mobile.domain.model.LibraryFilterChip
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.LibrarySortBy
import org.jellyfin.mobile.domain.model.MediaItemSummary

/**
 * MVI Contract for Library Browser Screen.
 * Strictly adheres to 02_LIBRARY_BROWSER.md.
 */

sealed interface LibraryIntent {
    data class LoadLibrary(val parentId: String, val libraryName: String = "") : LibraryIntent
    data class SwitchLibrary(val libraryId: String, val libraryName: String = "") : LibraryIntent
    data class ChangeSort(val sortBy: LibrarySortBy, val isAscending: Boolean = false) : LibraryIntent
    data object ToggleUnwatchedOnly : LibraryIntent
    data object Toggle4kOnly : LibraryIntent
    data class FilterByLetter(val letter: Char?) : LibraryIntent
    data class JumpToLetter(val character: Char) : LibraryIntent
    data class ToggleFilter(val filter: LibraryFilterChip) : LibraryIntent
    data class OpenMediaDetails(val mediaId: String) : LibraryIntent
    data class QuickPlay(val mediaId: String) : LibraryIntent
    data class QuickPlayMedia(val mediaId: String) : LibraryIntent
    data object ResetFilters : LibraryIntent
    data object Refresh : LibraryIntent
}

typealias LibraryBrowserIntent = LibraryIntent

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Success(
        val items: ImmutableList<MediaItemSummary> = persistentListOf(),
        val totalCount: Int = 0,
        val activeFilter: LibraryFilter = LibraryFilter(),
        val currentLibraryId: String = "",
        val currentLibraryName: String = "المكتبة",
        val availableLibraries: ImmutableList<LibraryFolder> = persistentListOf(),
        val selectedLibraryId: String? = null,
        val selectedSort: LibrarySortBy = LibrarySortBy.DATE_ADDED,
        val isSortAscending: Boolean = false,
        val activeFilters: ImmutableSet<LibraryFilterChip> = persistentSetOf(),
        val selectedLetter: Char? = null,
        val isRefreshing: Boolean = false
    ) : LibraryUiState {
        val totalItemCount: Int get() = totalCount
    }

    data class Error(
        val message: String
    ) : LibraryUiState

    data class Empty(
        val message: String = "هذه المكتبة فارغة حالياً على الخادم"
    ) : LibraryUiState
}

sealed interface LibraryEffect {
    data class NavigateToDetails(val mediaId: String) : LibraryEffect
    data class LaunchPlayer(val mediaId: String, val resumePositionMs: Long = 0L) : LibraryEffect
    data class ScrollToIndex(val index: Int) : LibraryEffect
    data class ShowToast(val message: String) : LibraryEffect
}
