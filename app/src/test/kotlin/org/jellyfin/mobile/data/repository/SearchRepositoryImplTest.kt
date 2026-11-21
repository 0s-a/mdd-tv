package org.jellyfin.mobile.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jellyfin.mobile.core.network.SessionManager
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
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.ServerInfo
import org.jellyfin.mobile.domain.model.UserSession
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeJellyfinApiService : JellyfinApiService {
        var capturedIncludeItemTypes: String? = null
        var capturedSearchTerm: String? = null
        var searchResponseItems: List<BaseItemDto> = emptyList()
        var genreResponseItems: List<BaseItemDto> = emptyList()
        var spotlightResponseItems: List<BaseItemDto> = emptyList()

        override suspend fun searchMedia(
            userId: String,
            searchTerm: String?,
            genres: String?,
            includeItemTypes: String,
            recursive: Boolean,
            limit: Int,
            fields: String
        ): ItemsResponseDto {
            capturedIncludeItemTypes = includeItemTypes
            capturedSearchTerm = searchTerm
            return ItemsResponseDto(
                items = searchResponseItems,
                totalRecordCount = searchResponseItems.size
            )
        }

        override suspend fun getGenres(
            userId: String?,
            parentId: String?,
            recursive: Boolean,
            sortBy: String
        ): ItemsResponseDto {
            return ItemsResponseDto(items = genreResponseItems, totalRecordCount = genreResponseItems.size)
        }

        override suspend fun getSpotlightItems(
            userId: String,
            sortBy: String,
            includeItemTypes: String,
            imageTypes: String,
            recursive: Boolean,
            limit: Int,
            fields: String
        ): ItemsResponseDto {
            return ItemsResponseDto(items = spotlightResponseItems, totalRecordCount = spotlightResponseItems.size)
        }

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
            return ItemsResponseDto(items = emptyList(), totalRecordCount = 0)
        }

        override suspend fun getItemsByGenre(
            userId: String,
            genres: String,
            includeItemTypes: String,
            recursive: Boolean,
            limit: Int,
            fields: String
        ): ItemsResponseDto {
            return ItemsResponseDto(items = emptyList(), totalRecordCount = 0)
        }

        override suspend fun getPublicSystemInfo(): PublicSystemInfoDto = throw UnsupportedOperationException()
        override suspend fun authenticateByName(body: AuthenticateUserByNameDto): AuthenticationResultDto = throw UnsupportedOperationException()
        override suspend fun getUserViews(userId: String): UserViewsResponseDto = throw UnsupportedOperationException()
        override suspend fun getResumeItems(userId: String, mediaTypes: String, limit: Int): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun getLatestItems(userId: String, parentId: String?, limit: Int, fields: String): List<BaseItemDto> = throw UnsupportedOperationException()
        override suspend fun getNextUpEpisodes(userId: String, limit: Int, fields: String): ItemsResponseDto = throw UnsupportedOperationException()
        override suspend fun getLibraryItems(userId: String, parentId: String, sortBy: String, sortOrder: String, includeItemTypes: String, filters: String?, startIndex: Int, limit: Int, fields: String, recursive: Boolean, nameStartsWith: String?): ItemsResponseDto = throw UnsupportedOperationException()
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

    private class FakeHandshakeRepository : HandshakeRepository {
        val session = UserSession(
            userId = "test-user-id",
            userName = "TestUser",
            token = "test-token",
            subtitleLanguagePreference = "ar",
            subtitleMode = "Default"
        )
        private val _currentSession = MutableStateFlow<UserSession?>(session)
        override val currentSession = _currentSession.asStateFlow()
        override val serverInfo = MutableStateFlow<ServerInfo?>(null).asStateFlow()

        override suspend fun getServerInfo(): Result<ServerInfo> = Result.success(
            ServerInfo(id = "srv-1", name = "TestServer", version = "10.8.0", operatingSystem = "Linux")
        )
        override suspend fun authenticate(username: String, password: String): Result<UserSession> = Result.success(session)
        override suspend fun performHandshake(username: String, password: String): Result<UserSession> = Result.success(session)
        override suspend fun getUserViews(userId: String?): Result<List<LibraryFolder>> = Result.success(emptyList())
        override suspend fun clearSession() { _currentSession.value = null }
    }

    private lateinit var apiService: FakeJellyfinApiService
    private lateinit var sessionManager: SessionManager
    private lateinit var handshakeRepository: FakeHandshakeRepository
    private lateinit var repository: SearchRepositoryImpl

    @Before
    fun setup() {
        apiService = FakeJellyfinApiService()
        sessionManager = SessionManager()
        sessionManager.saveSession(token = "test-token", userId = "test-user-id")
        handshakeRepository = FakeHandshakeRepository()
        repository = SearchRepositoryImpl(
            apiService = apiService,
            sessionManager = sessionManager,
            handshakeRepository = handshakeRepository,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun search_requestsOnlyMovieSeriesPerson_andDiscardsEpisodesAndSeasons() = runTest(testDispatcher) {
        apiService.searchResponseItems = listOf(
            BaseItemDto(id = "s-1", name = "Breaking Bad", type = "Series"),
            BaseItemDto(id = "ep-1", name = "Pilot", seriesName = "Breaking Bad", type = "Episode"),
            BaseItemDto(id = "sea-1", name = "Season 1", seriesName = "Breaking Bad", type = "Season"),
            BaseItemDto(id = "m-1", name = "El Camino: A Breaking Bad Movie", type = "Movie"),
            BaseItemDto(id = "p-1", name = "Bryan Cranston", type = "Person")
        )

        val result = repository.search("Breaking Bad")
        assertTrue(result.isSuccess)

        // Verify request parameter
        assertEquals("Movie,Series,Person", apiService.capturedIncludeItemTypes)

        val summary = result.getOrThrow()
        // Top match should be Breaking Bad series
        assertNotNull(summary.topMatch)
        assertEquals("Breaking Bad", summary.topMatch?.name)
        assertEquals("Series", summary.topMatch?.type)

        // mediaResults should contain El Camino (Movie), and NO Episode or Season
        assertEquals(1, summary.mediaResults.size)
        val mediaItem = summary.mediaResults.first()
        assertEquals("El Camino: A Breaking Bad Movie", mediaItem.name)
        assertEquals("Movie", mediaItem.type)

        // Verify peopleResults contains Bryan Cranston
        assertEquals(1, summary.peopleResults.size)
        val person = summary.peopleResults.first()
        assertEquals("Bryan Cranston", person.name)
        assertEquals("Person", person.type)
    }

    @Test
    fun search_whenBlankQueryAndNoGenres_returnsEmptySummaryWithoutNetworkCall() = runTest(testDispatcher) {
        val result = repository.search("   ")
        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals("", summary.query)
        assertNull(summary.topMatch)
        assertTrue(summary.mediaResults.isEmpty())
        assertTrue(summary.peopleResults.isEmpty())
        assertNull(apiService.capturedIncludeItemTypes)
    }

    @Test
    fun recentQueries_managesHistoryProperly() = runTest(testDispatcher) {
        assertTrue(repository.getRecentQueries().first().isEmpty())

        repository.addRecentQuery("Inception")
        repository.addRecentQuery("Interstellar")
        repository.addRecentQuery("Dune")

        var queries = repository.getRecentQueries().first()
        assertEquals(listOf("Dune", "Interstellar", "Inception"), queries)

        // Removing a query
        repository.removeRecentQuery("Interstellar")
        queries = repository.getRecentQueries().first()
        assertEquals(listOf("Dune", "Inception"), queries)

        // Clearing history
        repository.clearRecentQueries()
        queries = repository.getRecentQueries().first()
        assertTrue(queries.isEmpty())
    }
}
