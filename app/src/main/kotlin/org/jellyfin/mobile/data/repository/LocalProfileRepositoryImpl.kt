package org.jellyfin.mobile.data.repository

import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.data.local.dao.LocalFavoriteDao
import org.jellyfin.mobile.data.local.dao.LocalWatchProgressDao
import org.jellyfin.mobile.data.local.dao.UserProfileDao
import org.jellyfin.mobile.data.local.dao.WatchedArchiveDao
import org.jellyfin.mobile.data.local.entity.LocalFavoriteEntity
import org.jellyfin.mobile.data.local.entity.LocalWatchProgressEntity
import org.jellyfin.mobile.data.local.entity.UserProfileEntity
import org.jellyfin.mobile.data.local.entity.WatchedArchiveEntity
import org.jellyfin.mobile.domain.repository.LocalProfileRepository

class LocalProfileRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val localFavoriteDao: LocalFavoriteDao,
    private val watchedArchiveDao: WatchedArchiveDao,
    private val localWatchProgressDao: LocalWatchProgressDao
) : LocalProfileRepository {

    // Profile
    override suspend fun hasProfile(): Boolean = userProfileDao.hasProfile()

    override fun observeProfile(): Flow<UserProfileEntity?> = userProfileDao.observeProfile()

    override suspend fun getProfile(): UserProfileEntity? = userProfileDao.getProfile()

    override suspend fun createProfile(displayName: String, avatarKey: String) {
        val profile = UserProfileEntity(
            displayName = displayName,
            avatarKey = avatarKey
        )
        userProfileDao.insertOrUpdateProfile(profile)
    }

    override suspend fun updateProfile(displayName: String, avatarKey: String) {
        userProfileDao.updateProfile(
            displayName = displayName,
            avatarKey = avatarKey
        )
    }

    // Favorites
    override fun observeFavorites(): Flow<List<LocalFavoriteEntity>> = localFavoriteDao.observeAllFavorites()

    override fun observeFavoriteIds(): Flow<List<String>> = localFavoriteDao.observeFavoriteIds()

    override fun observeIsFavorite(itemId: String): Flow<Boolean> = localFavoriteDao.observeIsFavorite(itemId)

    override suspend fun isFavorite(itemId: String): Boolean = localFavoriteDao.isFavorite(itemId)

    override suspend fun addFavorite(
        itemId: String,
        title: String,
        mediaType: String,
        releaseYear: String,
        communityRating: String
    ) {
        localFavoriteDao.insertFavorite(
            LocalFavoriteEntity(
                itemId = itemId,
                title = title,
                mediaType = mediaType,
                releaseYear = releaseYear,
                communityRating = communityRating
            )
        )
    }

    override suspend fun removeFavorite(itemId: String) {
        localFavoriteDao.deleteFavorite(itemId)
    }

    override suspend fun toggleFavorite(
        itemId: String,
        title: String,
        mediaType: String,
        releaseYear: String,
        communityRating: String
    ): Boolean {
        return if (localFavoriteDao.isFavorite(itemId)) {
            localFavoriteDao.deleteFavorite(itemId)
            false
        } else {
            localFavoriteDao.insertFavorite(
                LocalFavoriteEntity(
                    itemId = itemId,
                    title = title,
                    mediaType = mediaType,
                    releaseYear = releaseYear,
                    communityRating = communityRating
                )
            )
            true
        }
    }

    // Watched Archive
    override fun observeArchive(): Flow<List<WatchedArchiveEntity>> = watchedArchiveDao.observeAllArchive()

    override suspend fun isWatched(itemId: String): Boolean = watchedArchiveDao.isWatched(itemId)

    override fun observeIsWatched(itemId: String): Flow<Boolean> = watchedArchiveDao.observeIsWatched(itemId)

    override suspend fun addToArchive(itemId: String, title: String, mediaType: String) {
        watchedArchiveDao.insertOrUpdateArchive(
            WatchedArchiveEntity(
                itemId = itemId,
                title = title,
                mediaType = mediaType
            )
        )
    }

    override suspend fun removeFromArchive(itemId: String) {
        watchedArchiveDao.deleteFromArchive(itemId)
    }

    // Watch Progress
    override fun observeContinueWatching(): Flow<List<LocalWatchProgressEntity>> =
        localWatchProgressDao.observeContinueWatching()

    override suspend fun getContinueWatching(): List<LocalWatchProgressEntity> =
        localWatchProgressDao.getContinueWatching()

    override suspend fun getProgress(itemId: String): LocalWatchProgressEntity? =
        localWatchProgressDao.getProgress(itemId)

    override fun observeProgress(itemId: String): Flow<LocalWatchProgressEntity?> =
        localWatchProgressDao.observeProgress(itemId)

    override suspend fun saveWatchProgress(
        itemId: String,
        title: String,
        positionTicks: Long,
        durationTicks: Long,
        isPlayed: Boolean
    ) {
        localWatchProgressDao.insertOrUpdateProgress(
            LocalWatchProgressEntity(
                itemId = itemId,
                title = title,
                positionTicks = positionTicks,
                durationTicks = durationTicks,
                isPlayed = isPlayed
            )
        )
    }

    override suspend fun dismissContinueWatching(itemId: String) {
        localWatchProgressDao.deleteProgress(itemId)
    }

    override suspend fun markWatched(itemId: String) {
        localWatchProgressDao.markPlayed(itemId)
    }
}
