package org.jellyfin.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an offline downloaded media item in the local cache vault.
 */
@Entity(tableName = "downloaded_media")
data class DownloadedMediaEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val mediaType: String,
    val localFilePath: String,
    val subtitleFilePath: String? = null,
    val totalSizeBytes: Long = 0L,
    val durationTicks: Long? = null,
    val qualityTag: String = "1080p",
    val downloadedAt: Long = System.currentTimeMillis()
)
