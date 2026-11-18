package org.jellyfin.mobile.presentation.home

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import org.jellyfin.mobile.domain.model.ContinueWatchingItem
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.MediaItemSummary

/**
 * MVI Contracts for Home Dashboard Screen.
 * Strictly adheres to 01_HOME_DASHBOARD.md Section 5.1.
 */

sealed interface HomeIntent {
    data object LoadDashboardData : HomeIntent
    data object RefreshDashboard : HomeIntent
    data class SelectLibrary(val libraryId: String, val title: String = "") : HomeIntent
    data class QuickPlay(val mediaId: String, val resumePositionMs: Long = 0L) : HomeIntent
    data class OpenMediaDetails(val mediaId: String) : HomeIntent
    data class DismissFromContinueWatching(val mediaId: String) : HomeIntent
    data class MarkItemWatched(val mediaId: String) : HomeIntent
    data class SelectHeroIndex(val index: Int) : HomeIntent
}

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val heroItems: ImmutableList<MediaItemSummary>,
        val currentHeroIndex: Int = 0,
        val ambientColor: Color = Color.Unspecified,
        val libraries: ImmutableList<LibraryFolder>,
        val continueWatchingItems: ImmutableList<ContinueWatchingItem>,
        val recentlyAddedItems: ImmutableList<MediaItemSummary>,
        val userDisplayName: String = "",
        val isRefreshing: Boolean = false
    ) : HomeUiState

    data class Error(
        val message: String,
        val canRetryOffline: Boolean = false
    ) : HomeUiState
}

sealed interface HomeEffect {
    data class NavigateToDetails(val mediaId: String) : HomeEffect
    data class LaunchPlayer(val mediaId: String, val resumePositionMs: Long) : HomeEffect
    data class NavigateToLibraryBrowser(val libraryId: String, val title: String) : HomeEffect
    data class ShowToast(val message: String) : HomeEffect
}
