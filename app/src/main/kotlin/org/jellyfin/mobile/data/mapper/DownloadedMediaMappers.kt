package org.jellyfin.mobile.data.mapper

import org.jellyfin.mobile.data.local.entity.DownloadedMediaEntity
import org.jellyfin.mobile.domain.model.DownloadedMediaItem

/**
 * Maps local downloaded media entities to pure domain models.
 */
fun DownloadedMediaEntity.toDomain(): DownloadedMediaItem {
    val sizeMb = totalSizeBytes.toDouble() / (1024.0 * 1024.0)
    val sizeGb = sizeMb / 1024.0
    val sizeFormatted = when {
        sizeGb >= 1.0 -> String.format("%.2f GB", sizeGb)
        sizeMb >= 1.0 -> String.format("%.1f MB", sizeMb)
        else -> "${totalSizeBytes / 1024} KB"
    }

    val durationFormatted = durationTicks?.let { ticks ->
        val totalMinutes = (ticks / 10_000_000L / 60L).toInt()
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        when {
            hours > 0 -> "${hours}س ${mins}د"
            mins > 0 -> "${mins}د"
            else -> null
        }
    }

    return DownloadedMediaItem(
        id = id,
        title = title,
        mediaType = mediaType,
        localFilePath = localFilePath,
        subtitleFilePath = subtitleFilePath,
        sizeFormatted = sizeFormatted,
        durationFormatted = durationFormatted,
        qualityTag = qualityTag,
        totalSizeBytes = totalSizeBytes
    )
}
