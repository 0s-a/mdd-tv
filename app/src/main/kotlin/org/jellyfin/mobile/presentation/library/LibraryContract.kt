package org.jellyfin.mobile.presentation.library

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jellyfin.mobile.domain.model.BreadcrumbItem
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
    data class OpenMediaDetails(val mediaId: String) : LibraryIntent
    data class QuickPlay(val mediaId: String) : LibraryIntent
    data object Refresh : LibraryIntent
    data object LoadNextPage : LibraryIntent
    data class NavigateIntoFolder(val folderId: String, val folderName: String) : LibraryIntent
    data object NavigateBackInHierarchy : LibraryIntent
    data class JumpToBreadcrumb(val targetIndex: Int) : LibraryIntent
}

typealias LibraryBrowserIntent = LibraryIntent

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Success(
        val items: ImmutableList<MediaItemSummary> = persistentListOf(),
        val totalCount: Int = 0,
        val currentLibraryId: String = "",
        val currentLibraryName: String = "المكتبة",
        val availableLibraries: ImmutableList<LibraryFolder> = persistentListOf(),
        val selectedLibraryId: String? = null,
        val selectedSort: LibrarySortBy = LibrarySortBy.DATE_ADDED,
        val isSortAscending: Boolean = false,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMoreItems: Boolean = true,
        val breadcrumbs: ImmutableList<BreadcrumbItem> = persistentListOf(),
        val isRoot: Boolean = false
    ) : LibraryUiState {
        val totalItemCount: Int get() = totalCount
        val isAtRoot: Boolean get() = isRoot || breadcrumbs.size <= 1
        val currentFolderTitle: String get() = if (isAtRoot) "المكتبة" else breadcrumbs.lastOrNull()?.name ?: currentLibraryName
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
