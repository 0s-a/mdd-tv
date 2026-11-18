package org.jellyfin.mobile.data.repository

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.jellyfin.mobile.data.local.dao.DownloadedMediaDao
import org.jellyfin.mobile.data.local.entity.DownloadedMediaEntity
import org.jellyfin.mobile.data.mapper.toDomain
import org.jellyfin.mobile.domain.model.DownloadedMediaItem
import org.jellyfin.mobile.domain.model.StorageBreakdown
import org.jellyfin.mobile.domain.repository.DownloadsRepository
import java.io.File

/**
 * Concrete implementation of DownloadsRepository.
 * Manages local Room persistence, physical file deletion, and real-time StatFs storage metrics.
 */
class DownloadsRepositoryImpl(
    private val context: Context,
    private val dao: DownloadedMediaDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DownloadsRepository {

    override fun getDownloadedItems(): Flow<List<DownloadedMediaItem>> {
        return dao.getDownloadedItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getStorageBreakdown(): StorageBreakdown = withContext(ioDispatcher) {
        val rootPath = context.getExternalFilesDir(null)?.path ?: context.filesDir.path
        val statFs = StatFs(rootPath)

        val totalBytes = statFs.totalBytes
        val freeBytes = statFs.availableBytes
        val jellyfinBytes = dao.getTotalStorageBytes() ?: 0L
        val otherAppsBytes = (totalBytes - freeBytes - jellyfinBytes).coerceAtLeast(0L)

        StorageBreakdown(
            totalDeviceBytes = totalBytes,
            freeDeviceBytes = freeBytes,
            jellyfinMediaBytes = jellyfinBytes,
            otherAppsBytes = otherAppsBytes
        )
    }

    override suspend fun deleteDownloadedItem(mediaId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val item = dao.getDownloadedItemById(mediaId)
            if (item != null) {
                // Remove physical video file
                runCatching { File(item.localFilePath).delete() }

                // Remove physical subtitle file if present
                item.subtitleFilePath?.let { subPath ->
                    runCatching { File(subPath).delete() }
                }

                // Delete entry from Room
                dao.deleteItem(mediaId)
            }
        }
    }

    override suspend fun clearAllDownloads(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            // Remove download directory contents
            val downloadDir = context.getExternalFilesDir("media_vault")
            if (downloadDir != null && downloadDir.exists()) {
                downloadDir.deleteRecursively()
            }

            dao.clearAll()
        }
    }

    override suspend fun saveDownloadedMedia(
        id: String,
        title: String,
        mediaType: String,
        localFilePath: String,
        subtitleFilePath: String?,
        totalSizeBytes: Long,
        durationTicks: Long?,
        qualityTag: String
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            dao.insertItem(
                DownloadedMediaEntity(
                    id = id,
                    title = title,
                    mediaType = mediaType,
                    localFilePath = localFilePath,
                    subtitleFilePath = subtitleFilePath,
                    totalSizeBytes = totalSizeBytes,
                    durationTicks = durationTicks,
                    qualityTag = qualityTag
                )
            )
        }
    }
}
