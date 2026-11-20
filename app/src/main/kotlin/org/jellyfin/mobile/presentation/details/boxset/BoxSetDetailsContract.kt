package org.jellyfin.mobile.presentation.details.boxset

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jellyfin.mobile.domain.model.MediaDetails

/**
 * Domain representation of an individual movie part within a BoxSet / Collection.
 * Strict zero-emoji compliance.
 */
data class BoxSetPartItem(
    val id: String,
    val title: String,
    val partIndex: Int,
    val productionYear: Int? = null,
    val communityRating: Float? = null,
    val runTimeTicks: Long? = null,
    val durationText: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val isPlayed: Boolean = false,
    val playbackPositionPercentage: Double = 0.0,
    val overview: String? = null,
    val isFavorite: Boolean = false
) {
    val runtimeMs: Long get() = (runTimeTicks ?: 0L) / 10_000L
    val isWatched: Boolean get() = isPlayed || playbackPositionPercentage >= 95.0
    val resumePositionMs: Long
        get() = (runtimeMs * (playbackPositionPercentage / 100.0)).toLong()
}

/**
 * MVI Intent contract for the BoxSets / Collections details screen.
 */
sealed interface BoxSetIntent {
    data class LoadBoxSet(val boxSetId: String) : BoxSetIntent
    data object RetryLoad : BoxSetIntent
    data object Refresh : BoxSetIntent
    data class PlayMovie(val movieId: String, val resumePositionMs: Long = 0L) : BoxSetIntent
    data object ResumeFranchise : BoxSetIntent
    data object PlayFromBeginning : BoxSetIntent
    data object ToggleFavorite : BoxSetIntent
    data class ToggleMovieWatched(val movieId: String) : BoxSetIntent
    data class OpenMovieDetails(val movieId: String) : BoxSetIntent
    data object ShareFranchise : BoxSetIntent
}

/**
 * MVI State contract for the BoxSets / Collections details screen.
 */
sealed interface BoxSetUiState {
    data object Loading : BoxSetUiState

    data class Success(
        val boxSetDetails: MediaDetails,
        val parts: ImmutableList<BoxSetPartItem> = persistentListOf(),
        val totalPartsCount: Int = parts.size,
        val totalRuntimeTicks: Long = 0L,
        val formattedTotalRuntime: String = "",
        val nextUnwatchedPart: BoxSetPartItem? = null,
        val nextUnwatchedIndex: Int? = null,
        val isFavorite: Boolean = boxSetDetails.isFavorite,
        val isRefreshing: Boolean = false
    ) : BoxSetUiState

    data class Error(
        val message: String,
        val isNetworkError: Boolean = false
    ) : BoxSetUiState
}

/**
 * One-shot UI effects contract for the BoxSets / Collections details screen.
 */
sealed interface BoxSetEffect {
    data class LaunchPlayer(val mediaId: String, val resumePositionMs: Long = 0L) : BoxSetEffect
    data class NavigateToMovieDetails(val movieId: String) : BoxSetEffect
    data class ShowSnackbar(val message: String) : BoxSetEffect
    data class ShowToast(val message: String) : BoxSetEffect
    data class ShareText(val text: String) : BoxSetEffect
}
