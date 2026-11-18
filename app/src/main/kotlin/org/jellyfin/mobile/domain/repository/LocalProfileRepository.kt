package org.jellyfin.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.data.local.entity.LocalFavoriteEntity
import org.jellyfin.mobile.data.local.entity.LocalWatchProgressEntity
import org.jellyfin.mobile.data.local.entity.UserProfileEntity
import org.jellyfin.mobile.data.local.entity.WatchedArchiveEntity

/**
 * Domain repository contract for Local-First Profile, Favorites, Archive, and Watch Progress.
 */
interface LocalProfileRepository {

    // Profile
    suspend fun hasProfile(): Boolean
    fun observeProfile(): Flow<UserProfileEntity?>
    suspend fun getProfile(): UserProfileEntity?
    suspend fun createProfile(displayName: String, avatarKey: String)
    suspend fun updateProfile(displayName: String, avatarKey: String)

    // Favorites (Local-Only)
    fun observeFavorites(): Flow<List<LocalFavoriteEntity>>
    fun observeFavoriteIds(): Flow<List<String>>
    fun observeIsFavorite(itemId: String): Flow<Boolean>
    suspend fun isFavorite(itemId: String): Boolean
    suspend fun addFavorite(itemId: String, title: String, mediaType: String, releaseYear: String = "", communityRating: String = "")
    suspend fun removeFavorite(itemId: String)
    suspend fun toggleFavorite(itemId: String, title: String, mediaType: String, releaseYear: String = "", communityRating: String = ""): Boolean

    // Watched Archive (Local-Only)
    fun observeArchive(): Flow<List<WatchedArchiveEntity>>
    suspend fun isWatched(itemId: String): Boolean
    fun observeIsWatched(itemId: String): Flow<Boolean>
    suspend fun addToArchive(itemId: String, title: String, mediaType: String)
    suspend fun removeFromArchive(itemId: String)

    // Watch Progress & Continue Watching (Local-Only)
    fun observeContinueWatching(): Flow<List<LocalWatchProgressEntity>>
    suspend fun getContinueWatching(): List<LocalWatchProgressEntity>
    suspend fun getProgress(itemId: String): LocalWatchProgressEntity?
    fun observeProgress(itemId: String): Flow<LocalWatchProgressEntity?>
    suspend fun saveWatchProgress(itemId: String, title: String, positionTicks: Long, durationTicks: Long, isPlayed: Boolean)
    suspend fun dismissContinueWatching(itemId: String)
    suspend fun markWatched(itemId: String)
}
