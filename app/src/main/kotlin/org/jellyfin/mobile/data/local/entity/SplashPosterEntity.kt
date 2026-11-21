package org.jellyfin.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache entity for splash screen posters fetched from the Jellyfin server.
 */
@Entity(tableName = "splash_posters")
data class SplashPosterEntity(
    @PrimaryKey val id: String,
    val title: String,
    val badge: String,
    val imageUrl: String,
    val orderIndex: Int = 0
)
