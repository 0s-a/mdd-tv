package org.jellyfin.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.domain.model.DownloadedMediaItem
import org.jellyfin.mobile.domain.model.StorageBreakdown

/**
 * Domain repository contract for managing offline media downloads and physical storage metrics.
 * Pure Kotlin with zero android.* dependencies.
 */
interface DownloadsRepository {

    /**
     * Observes the list of downloaded items stored locally.
     */
    fun getDownloadedItems(): Flow<List<DownloadedMediaItem>>

    /**
     * Calculates device storage breakdown (total, free, Jellyfin used, other apps used).
     */
    suspend fun getStorageBreakdown(): StorageBreakdown

    /**
     * Deletes a downloaded media item and its physical file on disk.
     */
    suspend fun deleteDownloadedItem(mediaId: String): Result<Unit>

    /**
     * Clears all downloaded items and removes all physical media files from local storage.
     */
    suspend fun clearAllDownloads(): Result<Unit>

    /**
     * Saves a completed download record into local database.
     */
    suspend fun saveDownloadedMedia(
        id: String,
        title: String,
        mediaType: String,
        localFilePath: String,
        subtitleFilePath: String? = null,
        totalSizeBytes: Long,
        durationTicks: Long? = null,
        qualityTag: String = "1080p"
    ): Result<Unit>
}
