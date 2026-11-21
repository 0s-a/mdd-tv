package org.jellyfin.mobile.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.local.dao.SplashPosterDao
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

    private class FakeJellyfinApiService : JellyfinApiService {
        var splashItems: List<BaseItemDto> = emptyList()

        override suspend fun getSplashItems(
            userId: String,
            sortBy: String,
            sortOrder: String,
            includeItemTypes: String,
            imageTypes: String,
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
                    imageUrl = "http://localhost/item-1"
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
                    genres = listOf("Sci-Fi", "Adventure")
                ),
                BaseItemDto(
                    id = "series-200",
                    name = "Breaking Bad MDD",
                    type = "Series",
                    genres = listOf("Crime")
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
        assertEquals("Breaking Bad MDD", posters[1].title)
        assertEquals("CRIME", posters[1].badge)

        // Verify stored in DAO
        assertEquals(2, fakeDao.storedPosters.size)
        assertEquals("movie-100", fakeDao.storedPosters[0].id)
    }
}
