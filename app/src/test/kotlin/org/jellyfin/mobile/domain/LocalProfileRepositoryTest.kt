package org.jellyfin.mobile.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.jellyfin.mobile.data.local.dao.LocalFavoriteDao
import org.jellyfin.mobile.data.local.dao.LocalWatchProgressDao
import org.jellyfin.mobile.data.local.dao.UserProfileDao
import org.jellyfin.mobile.data.local.dao.WatchedArchiveDao
import org.jellyfin.mobile.data.local.entity.LocalFavoriteEntity
import org.jellyfin.mobile.data.local.entity.LocalWatchProgressEntity
import org.jellyfin.mobile.data.local.entity.UserProfileEntity
import org.jellyfin.mobile.data.local.entity.WatchedArchiveEntity
import org.jellyfin.mobile.data.repository.LocalProfileRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalProfileRepositoryTest {

    private class FakeUserProfileDao : UserProfileDao {
        var profile: UserProfileEntity? = null
        override fun observeProfile(id: String): Flow<UserProfileEntity?> = flowOf(profile)
        override suspend fun getProfile(id: String): UserProfileEntity? = profile
        override suspend fun hasProfile(id: String): Boolean = profile != null
        override suspend fun insertOrUpdateProfile(profile: UserProfileEntity) {
            this.profile = profile
        }
        override suspend fun updateProfile(id: String, displayName: String, avatarKey: String, updatedAt: Long) {
            profile = profile?.copy(displayName = displayName, avatarKey = avatarKey, lastUpdatedAt = updatedAt)
        }
    }

    private class FakeLocalFavoriteDao : LocalFavoriteDao {
        val favorites = mutableMapOf<String, LocalFavoriteEntity>()
        override fun observeAllFavorites(): Flow<List<LocalFavoriteEntity>> = flowOf(favorites.values.toList())
        override suspend fun getAllFavorites(): List<LocalFavoriteEntity> = favorites.values.toList()
        override fun observeFavoriteIds(): Flow<List<String>> = flowOf(favorites.keys.toList())
        override suspend fun getFavoriteIds(): List<String> = favorites.keys.toList()
        override fun observeIsFavorite(itemId: String): Flow<Boolean> = flowOf(favorites.containsKey(itemId))
        override suspend fun isFavorite(itemId: String): Boolean = favorites.containsKey(itemId)
        override suspend fun insertFavorite(favorite: LocalFavoriteEntity) {
            favorites[favorite.itemId] = favorite
        }
        override suspend fun deleteFavorite(itemId: String) {
            favorites.remove(itemId)
        }
    }

    private class FakeWatchedArchiveDao : WatchedArchiveDao {
        val archive = mutableMapOf<String, WatchedArchiveEntity>()
        override fun observeAllArchive(): Flow<List<WatchedArchiveEntity>> = flowOf(archive.values.toList())
        override suspend fun getAllArchive(): List<WatchedArchiveEntity> = archive.values.toList()
        override suspend fun isWatched(itemId: String): Boolean = archive.containsKey(itemId)
        override fun observeIsWatched(itemId: String): Flow<Boolean> = flowOf(archive.containsKey(itemId))
        override suspend fun insertOrUpdateArchive(item: WatchedArchiveEntity) {
            archive[item.itemId] = item
        }
        override suspend fun deleteFromArchive(itemId: String) {
            archive.remove(itemId)
        }
    }

    private class FakeLocalWatchProgressDao : LocalWatchProgressDao {
        val progressMap = mutableMapOf<String, LocalWatchProgressEntity>()
        override fun observeContinueWatching(): Flow<List<LocalWatchProgressEntity>> =
            flowOf(progressMap.values.filter { !it.isPlayed && it.positionTicks > 0 }.toList())
        override suspend fun getContinueWatching(): List<LocalWatchProgressEntity> =
            progressMap.values.filter { !it.isPlayed && it.positionTicks > 0 }.toList()
        override suspend fun getProgress(itemId: String): LocalWatchProgressEntity? = progressMap[itemId]
        override fun observeProgress(itemId: String): Flow<LocalWatchProgressEntity?> = flowOf(progressMap[itemId])
        override suspend fun insertOrUpdateProgress(progress: LocalWatchProgressEntity) {
            progressMap[progress.itemId] = progress
        }
        override suspend fun deleteProgress(itemId: String) {
            progressMap.remove(itemId)
        }
        override suspend fun markPlayed(itemId: String) {
            progressMap[itemId]?.let {
                progressMap[itemId] = it.copy(isPlayed = true)
            }
        }
    }

    @Test
    fun profile_lifecycle_createsAndUpdatesCorrectly() = runTest {
        val profileDao = FakeUserProfileDao()
        val repository = LocalProfileRepositoryImpl(
            userProfileDao = profileDao,
            localFavoriteDao = FakeLocalFavoriteDao(),
            watchedArchiveDao = FakeWatchedArchiveDao(),
            localWatchProgressDao = FakeLocalWatchProgressDao()
        )

        assertFalse(repository.hasProfile())

        repository.createProfile("أحمد", "avatar_3")
        assertTrue(repository.hasProfile())

        val profile = repository.getProfile()
        assertNotNull(profile)
        assertEquals("أحمد", profile?.displayName)
        assertEquals("avatar_3", profile?.avatarKey)

        repository.updateProfile("أحمد العتيبي", "avatar_7")
        val updated = repository.getProfile()
        assertEquals("أحمد العتيبي", updated?.displayName)
        assertEquals("avatar_7", updated?.avatarKey)
    }

    @Test
    fun favorites_toggle_worksCorrectlyLocally() = runTest {
        val favDao = FakeLocalFavoriteDao()
        val repository = LocalProfileRepositoryImpl(
            userProfileDao = FakeUserProfileDao(),
            localFavoriteDao = favDao,
            watchedArchiveDao = FakeWatchedArchiveDao(),
            localWatchProgressDao = FakeLocalWatchProgressDao()
        )

        assertFalse(repository.isFavorite("item-100"))

        val added = repository.toggleFavorite("item-100", "Interstellar", "Movie", "2014", "8.7")
        assertTrue(added)
        assertTrue(repository.isFavorite("item-100"))

        val removed = repository.toggleFavorite("item-100", "Interstellar", "Movie")
        assertFalse(removed)
        assertFalse(repository.isFavorite("item-100"))
    }

    @Test
    fun watchProgress_tracksAndDismissesProperly() = runTest {
        val progressDao = FakeLocalWatchProgressDao()
        val repository = LocalProfileRepositoryImpl(
            userProfileDao = FakeUserProfileDao(),
            localFavoriteDao = FakeLocalFavoriteDao(),
            watchedArchiveDao = FakeWatchedArchiveDao(),
            localWatchProgressDao = progressDao
        )

        repository.saveWatchProgress(
            itemId = "ep-1",
            title = "Breaking Bad S01E01",
            positionTicks = 120_000_000L,
            durationTicks = 300_000_000L,
            isPlayed = false
        )

        val cwList = repository.getContinueWatching()
        assertEquals(1, cwList.size)
        assertEquals("ep-1", cwList.first().itemId)

        repository.dismissContinueWatching("ep-1")
        val emptyCwList = repository.getContinueWatching()
        assertEquals(0, emptyCwList.size)
    }
}
