package org.jellyfin.mobile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.local.dao.LocalFavoriteDao
import org.jellyfin.mobile.data.local.dao.LocalWatchProgressDao
import org.jellyfin.mobile.data.local.dao.SplashPosterDao
import org.jellyfin.mobile.data.local.entity.LocalFavoriteEntity
import org.jellyfin.mobile.data.local.entity.LocalWatchProgressEntity
import org.jellyfin.mobile.data.local.entity.SplashPosterEntity
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.data.remote.dto.AuthenticateUserByNameDto
import org.jellyfin.mobile.data.remote.dto.AuthenticationResultDto
import org.jellyfin.mobile.data.remote.dto.BaseItemDto
import org.jellyfin.mobile.data.remote.dto.ItemsResponseDto
import org.jellyfin.mobile.data.remote.dto.PlaybackProgressDto
import org.jellyfin.mobile.data.remote.dto.PlaybackStartDto
import org.jellyfin.mobile.data.remote.dto.PlaybackStopDto
import org.jellyfin.mobile.data.remote.dto.PublicSystemInfoDto
import org.jellyfin.mobile.data.remote.dto.UserViewsResponseDto
import org.jellyfin.mobile.presentation.splash.model.SplashPosterCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SplashPosterRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSplashPosterDao : SplashPosterDao {
        val storedPosters = mutableListOf<SplashPosterEntity>()

        override fun observePosters(): Flow<List<SplashPosterEntity>> = flowOf(storedPosters)
        override suspend fun getPosters(): List<SplashPosterEntity> = storedPosters.toList()
        override suspend fun insertPosters(posters: List<SplashPosterEntity>) {
            storedPosters.addAll(posters)
        }
        override suspend fun clearPosters() {
            storedPosters.clear()
        }
    }

    private class FakeLocalFavoriteDao : LocalFavoriteDao {
        val favoriteIds = mutableListOf<String>()

        override fun observeAllFavorites(): Flow<List<LocalFavoriteEntity>> = flowOf(emptyList())
        override suspend fun getAllFavorites(): List<LocalFavoriteEntity> = emptyList()
        override fun observeFavoriteIds(): Flow<List<String>> = flowOf(favoriteIds)
        override suspend fun getFavoriteIds(): List<String> = favoriteIds
        override suspend fun isFavorite(itemId: String): Boolean = favoriteIds.contains(itemId)
        override fun observeIsFavorite(itemId: String): Flow<Boolean> = flowOf(favoriteIds.contains(itemId))
        override suspend fun insertFavorite(favorite: LocalFavoriteEntity) { favoriteIds.add(favorite.itemId) }
        override suspend fun deleteFavorite(itemId: String) { favoriteIds.remove(itemId) }
    }

    private class FakeLocalWatchProgressDao : LocalWatchProgressDao {
        val inProgressItems = mutableListOf<LocalWatchProgressEntity>()

        override fun observeContinueWatching(): Flow<List<LocalWatchProgressEntity>> = flowOf(inProgressItems)
        override suspend fun getContinueWatching(): List<LocalWatchProgressEntity> = inProgressItems
        override suspend fun getProgress(itemId: String): LocalWatchProgressEntity? = inProgressItems.find { it.itemId == itemId }
        override fun observeProgress(itemId: String): Flow<LocalWatchProgressEntity?> = flowOf(inProgressItems.find { it.itemId == itemId })
        override suspend fun insertOrUpdateProgress(progress: LocalWatchProgressEntity) { inProgressItems.add(progress) }
        override suspend fun deleteProgress(itemId: String) { inProgressItems.removeAll { it.itemId == itemId } }
        override suspend fun markPlayed(itemId: String) {}
    }

    private class FakeJellyfinApiService : JellyfinApiService {
        var splashItems: List<BaseItemDto> = emptyList()

        override suspend fun getSplashItems(
            userId: String,
            sortBy: String,
            sortOrder: String,
            includeItemTypes: String,
            imageTypes: String,
            minCommunityRating: Float?,
            recursive: Boolean,
            limit: Int,
            fields: String
        ): ItemsResponseDto {
            return ItemsResponseDto(items = splashItems, totalRecordCount = splashItems.size)
        }

        override suspend fun getPublicSystemInfo(): PublicSystemInfoDto = throw UnsupportedOperationException()
        override suspend fun authenticateByName(body: AuthenticateUserByNameDto): AuthenticationResultDto = throw UnsupportedOperationException()
        override suspend fun getUserViews(userId: String): UserViewsResponseDto = throw UnsupportedOperationException()
        override suspend fun getResumeItems(userId: String, mediaTypes: String, limit: Int): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun getLatestItems(userId: String, parentId: String?, limit: Int, fields: String): List<BaseItemDto> = throw UnsupportedOperationException()
        override suspend fun getNextUpEpisodes(userId: String, limit: Int, fields: String): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun getSpotlightItems(userId: String, sortBy: String, includeItemTypes: String, imageTypes: String, recursive: Boolean, limit: Int, fields: String): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun getLibraryItems(userId: String, parentId: String, sortBy: String, sortOrder: String, includeItemTypes: String, filters: String?, startIndex: Int, limit: Int, fields: String, recursive: Boolean, nameStartsWith: String?): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun getGenres(userId: String?, parentId: String?, recursive: Boolean, sortBy: String): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun searchMedia(userId: String, searchTerm: String?, genres: String?, includeItemTypes: String, recursive: Boolean, limit: Int, fields: String): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun getItemsByGenre(userId: String, genres: String, includeItemTypes: String, recursive: Boolean, limit: Int, fields: String): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun getItemDetails(userId: String, itemId: String): BaseItemDto = throw UnsupportedOperationException()
        override suspend fun getSeasons(seriesId: String, userId: String): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun getEpisodes(seriesId: String, userId: String, seasonId: String?, fields: String): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun getSimilarItems(itemId: String, userId: String, limit: Int): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun markFavorite(userId: String, itemId: String): BaseItemDto = throw UnsupportedOperationException()
        override suspend fun removeFavorite(userId: String, itemId: String): BaseItemDto = throw UnsupportedOperationException()
        override suspend fun markPlayed(userId: String, itemId: String): BaseItemDto = throw UnsupportedOperationException()
        override suspend fun markUnplayed(userId: String, itemId: String): BaseItemDto = throw UnsupportedOperationException()
        override suspend fun reportPlaybackStart(body: PlaybackStartDto): Response<Unit> = throw UnsupportedOperationException()
        override suspend fun reportPlaybackProgress(body: PlaybackProgressDto): Response<Unit> = throw UnsupportedOperationException()
        override suspend fun reportPlaybackStopped(body: PlaybackStopDto): Response<Unit> = throw UnsupportedOperationException()
    }

    @Test
    fun `getCachedPosters returns catalog fallback when cache is empty`() = runTest(testDispatcher) {
        val fakeDao = FakeSplashPosterDao()
        val fakeApi = FakeJellyfinApiService()
        val sessionManager = SessionManager()

        val repository = SplashPosterRepositoryImpl(
            splashPosterDao = fakeDao,
            apiService = fakeApi,
            sessionManager = sessionManager,
            ioDispatcher = testDispatcher
        )

        val result = repository.getCachedPosters()
        assertEquals(SplashPosterCatalog.size, result.size)
    }

    @Test
    fun `getCachedPosters returns cached entities when cache is not empty`() = runTest(testDispatcher) {
        val fakeDao = FakeSplashPosterDao().apply {
            storedPosters.add(
                SplashPosterEntity(
                    id = "item-1",
                    title = "Inception Local",
                    badge = "SCI-FI",
                    imageUrl = "http://localhost/item-1",
                    backdropUrl = "http://localhost/item-1-backdrop",
                    rating = 8.8f
                )
            )
        }
        val fakeApi = FakeJellyfinApiService()
        val sessionManager = SessionManager()

        val repository = SplashPosterRepositoryImpl(
            splashPosterDao = fakeDao,
            apiService = fakeApi,
            sessionManager = sessionManager,
            ioDispatcher = testDispatcher
        )

        val result = repository.getCachedPosters()
        assertEquals(1, result.size)
        assertEquals("Inception Local", result[0].title)
        assertEquals("SCI-FI", result[0].badge)
        assertEquals("http://localhost/item-1-backdrop", result[0].backdropUrl)
        assertEquals(8.8f, result[0].rating)
    }

    @Test
    fun `syncPostersFromServer fetches items from api and saves them to dao`() = runTest(testDispatcher) {
        val fakeDao = FakeSplashPosterDao()
        val fakeApi = FakeJellyfinApiService().apply {
            splashItems = listOf(
                BaseItemDto(
                    id = "movie-100",
                    name = "Interstellar MDD",
                    type = "Movie",
                    genres = listOf("Sci-Fi", "Adventure"),
                    communityRating = 8.7f,
                    backdropImageTags = listOf("tag1")
                ),
                BaseItemDto(
                    id = "series-200",
                    name = "Breaking Bad MDD",
                    type = "Series",
                    genres = listOf("Crime"),
                    communityRating = 9.5f
                )
            )
        }
        val sessionManager = SessionManager().apply {
            saveSession(token = "tok-1", userId = "user-1")
        }

        val repository = SplashPosterRepositoryImpl(
            splashPosterDao = fakeDao,
            apiService = fakeApi,
            sessionManager = sessionManager,
            ioDispatcher = testDispatcher
        )

        val syncResult = repository.syncPostersFromServer()
        assertTrue(syncResult.isSuccess)
        val posters = syncResult.getOrThrow()
        assertEquals(2, posters.size)
        assertEquals("Interstellar MDD", posters[0].title)
        assertEquals("SCI-FI", posters[0].badge)
        assertEquals(8.7f, posters[0].rating)
        assertTrue(posters[0].backdropUrl.contains("Backdrop"))
        assertEquals("Breaking Bad MDD", posters[1].title)
        assertEquals("CRIME", posters[1].badge)

        // Verify stored in DAO
        assertEquals(2, fakeDao.storedPosters.size)
        assertEquals("movie-100", fakeDao.storedPosters[0].id)
        assertEquals(8.7f, fakeDao.storedPosters[0].rating)
    }

    @Test
    fun `syncPostersFromServer prioritizes user favorites in 60-40 personal fusion`() = runTest(testDispatcher) {
        val fakeDao = FakeSplashPosterDao()
        val fakeFavDao = FakeLocalFavoriteDao().apply {
            favoriteIds.add("fav-item-99")
        }
        val fakeApi = FakeJellyfinApiService().apply {
            splashItems = listOf(
                BaseItemDto(
                    id = "regular-item-1",
                    name = "Regular Movie 1",
                    type = "Movie",
                    genres = listOf("Action")
                ),
                BaseItemDto(
                    id = "fav-item-99",
                    name = "My Favorite Dune",
                    type = "Movie",
                    genres = listOf("Sci-Fi")
                )
            )
        }
        val sessionManager = SessionManager().apply {
            saveSession(token = "tok-1", userId = "user-1")
        }

        val repository = SplashPosterRepositoryImpl(
            splashPosterDao = fakeDao,
            apiService = fakeApi,
            sessionManager = sessionManager,
            localFavoriteDao = fakeFavDao,
            ioDispatcher = testDispatcher
        )

        val syncResult = repository.syncPostersFromServer()
        assertTrue(syncResult.isSuccess)
        val posters = syncResult.getOrThrow()
        assertEquals(2, posters.size)
        // Favorite item elevated to first position
        assertEquals("My Favorite Dune", posters[0].title)
        assertEquals("fav-item-99", fakeDao.storedPosters[0].id)
    }
}
