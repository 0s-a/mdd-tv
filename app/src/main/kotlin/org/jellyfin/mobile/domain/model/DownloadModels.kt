package org.jellyfin.mobile.domain.model

/**
 * Domain model representing a downloaded media item stored in the local vault.
 * Pure Kotlin with zero android.* dependencies.
 */
data class DownloadedMediaItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val mediaType: String = "Movie",
    val localFilePath: String = "",
    val subtitleFilePath: String? = null,
    val sizeFormatted: String = "0 MB",
    val durationFormatted: String? = null,
    val qualityTag: String = "1080p",
    val totalSizeBytes: Long = 0L,
    val audioTrack: String? = null,
    val posterUrl: String? = null,
    val isWatched: Boolean = false,
    val isSeries: Boolean = mediaType.equals("Series", ignoreCase = true) || mediaType.equals("Episode", ignoreCase = true)
) {
    val fileSizeBytes: Long get() = totalSizeBytes
    val resolution: String get() = qualityTag
    val fileSizeGb: Double
        get() = totalSizeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
}

enum class DownloadsTab(val title: String) {
    All("الكل"),
    Movies("الأفلام"),
    Series("المسلسلات"),
    Storage("إدارة المساحة");

    companion object {
        val ALL = All
        val MOVIES = Movies
        val SERIES = Series
        val STORAGE = Storage
    }
}

data class DownloadTaskProgress(
    val mediaId: String,
    val title: String,
    val qualityInfo: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val speedBytesPerSec: Long,
    val remainingSeconds: Long,
    val isPaused: Boolean = false,
    val thumbnailUrl: String? = null
) {
    val progressPercentage: Float
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val totalGb: Double get() = totalBytes.toDouble() / (1024 * 1024 * 1024)
    val downloadedGb: Double get() = downloadedBytes.toDouble() / (1024 * 1024 * 1024)
    val speedMbPerSec: Double get() = speedBytesPerSec.toDouble() / (1024 * 1024)
}

/**
 * Domain model representing physical storage allocation breakdown on the user's device.
 * Pure Kotlin with zero android.* dependencies.
 */
data class StorageBreakdown(
    val totalDeviceBytes: Long,
    val freeDeviceBytes: Long,
    val jellyfinMediaBytes: Long,
    val otherAppsBytes: Long
) {
    constructor(
        appUsedBytes: Long,
        systemAndOtherUsedBytes: Long,
        freeBytes: Long
    ) : this(
        totalDeviceBytes = appUsedBytes + systemAndOtherUsedBytes + freeBytes,
        freeDeviceBytes = freeBytes,
        jellyfinMediaBytes = appUsedBytes,
        otherAppsBytes = systemAndOtherUsedBytes
    )

    val appUsedBytes: Long get() = jellyfinMediaBytes
    val systemAndOtherUsedBytes: Long get() = otherAppsBytes
    val freeBytes: Long get() = freeDeviceBytes
    val totalBytes: Long get() = totalDeviceBytes

    val appUsedGb: Double get() = jellyfinMediaBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val otherUsedGb: Double get() = otherAppsBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val freeGb: Double get() = freeDeviceBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val totalGb: Double get() = totalDeviceBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

    val jellyfinMediaFormatted: String
        get() = formatBytes(jellyfinMediaBytes)

    val otherAppsFormatted: String
        get() = formatBytes(otherAppsBytes)

    val freeDeviceFormatted: String
        get() = formatBytes(freeDeviceBytes)

    val totalDeviceFormatted: String
        get() = formatBytes(totalDeviceBytes)

    val jellyfinPercentage: Float
        get() = if (totalDeviceBytes > 0) (jellyfinMediaBytes.toFloat() / totalDeviceBytes) else 0f

    val otherAppsPercentage: Float
        get() = if (totalDeviceBytes > 0) (otherAppsBytes.toFloat() / totalDeviceBytes) else 0f

    val freePercentage: Float
        get() = if (totalDeviceBytes > 0) (freeDeviceBytes.toFloat() / totalDeviceBytes) else 0f

    companion object {
        fun formatBytes(bytes: Long): String {
            val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1.0) {
                String.format("%.1f GB", gb)
            } else {
                val mb = bytes.toDouble() / (1024.0 * 1024.0)
                String.format("%.1f MB", mb)
            }
        }
    }
}

typealias StorageGaugeInfo = StorageBreakdown

fun StorageBreakdown.toStorageGaugeInfo(): StorageBreakdown = this
