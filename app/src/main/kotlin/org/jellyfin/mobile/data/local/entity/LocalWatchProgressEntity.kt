package org.jellyfin.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing local watch progress per item.
 * Updated every 10 seconds during playback; used to build Continue Watching locally.
 */
@Entity(tableName = "local_watch_progress")
data class LocalWatchProgressEntity(
    @PrimaryKey val itemId: String,
    val title: String,
    val positionTicks: Long,
    val durationTicks: Long,
    val isPlayed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
