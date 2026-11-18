package org.jellyfin.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing completed watches in the user's personal archive.
 */
@Entity(tableName = "watched_archive")
data class WatchedArchiveEntity(
    @PrimaryKey val itemId: String,
    val title: String,
    val mediaType: String, // "Movie", "Series", or "Episode"
    val completedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)
