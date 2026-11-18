package org.jellyfin.mobile.presentation.downloads

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jellyfin.mobile.domain.model.DownloadTaskProgress
import org.jellyfin.mobile.domain.model.DownloadedMediaItem
import org.jellyfin.mobile.domain.model.DownloadsTab
import org.jellyfin.mobile.domain.model.StorageBreakdown

/**
 * MVI Contract for Downloads & Offline Vault Screen.
 * Strictly adheres to docs/screens/06_DOWNLOADS_OFFLINE.md Section 5.
 */

sealed interface DownloadsIntent {
    data object LoadDownloadsData : DownloadsIntent
    data class PauseDownload(val mediaId: String) : DownloadsIntent
    data class ResumeDownload(val mediaId: String) : DownloadsIntent
    data class CancelDownload(val mediaId: String) : DownloadsIntent
    data class DeleteDownloadedItem(val mediaId: String) : DownloadsIntent
    data class PlayOfflineMedia(val mediaId: String, val filePath: String = "") : DownloadsIntent
    data class SetFilterTab(val tab: DownloadsTab) : DownloadsIntent
    data object DeleteAllWatched : DownloadsIntent
    data object DeleteAllDownloads : DownloadsIntent
    data object ToggleWifiOnly : DownloadsIntent
    data object RefreshStorage : DownloadsIntent
}

data class DownloadsUiState(
    val activeDownloads: ImmutableList<DownloadTaskProgress> = persistentListOf(),
    val downloadedMovies: ImmutableList<DownloadedMediaItem> = persistentListOf(),
    val downloadedSeries: ImmutableList<DownloadedMediaItem> = persistentListOf(),
    val storageBreakdown: StorageBreakdown = StorageBreakdown(0L, 0L, 0L, 0L),
    val selectedTab: DownloadsTab = DownloadsTab.All,
    val isWifiOnlyEnabled: Boolean = true,
    val isStorageLowWarning: Boolean = false,
    val isDeleting: Boolean = false
) {
    val storageInfo: StorageBreakdown get() = storageBreakdown
}

sealed interface DownloadsEffect {
    data class LaunchPlayer(val mediaId: String, val positionMs: Long = 0L) : DownloadsEffect
    data class PlayOffline(val mediaId: String, val localFilePath: String) : DownloadsEffect
    data class ShowToast(val message: String) : DownloadsEffect
    data class ShowSnackbar(val message: String) : DownloadsEffect
}
