package org.jellyfin.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing local isolated favorites.
 * Strictly local-only to prevent multi-device cross-pollution on the shared Jellyfin account.
 */
@Entity(tableName = "local_favorites")
data class LocalFavoriteEntity(
    @PrimaryKey val itemId: String,
    val title: String,
    val mediaType: String, // "Movie" or "Series"
    val releaseYear: String = "",
    val communityRating: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
