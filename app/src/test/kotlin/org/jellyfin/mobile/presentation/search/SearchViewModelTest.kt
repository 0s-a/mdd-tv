package org.jellyfin.mobile.presentation.search

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jellyfin.mobile.domain.model.GenreCategory
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.domain.model.SearchResultSummary
import org.jellyfin.mobile.domain.model.ServerInfo
import org.jellyfin.mobile.domain.model.UserSession
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.SearchRepository
import org.jellyfin.mobile.domain.usecase.SearchMediaUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSearchRepository : SearchRepository {
        var availableGenres = listOf("أكشن", "خيال علمي", "دراما", "كوميديا")
        var spotlightItem: MediaItemSummary? = MediaItemSummary(id = "s-1", name = "Dune", type = "Movie")
        var searchResult: SearchResultSummary = SearchResultSummary(query = "")
        var shouldFail: Boolean = false
        val recentQueries = MutableStateFlow<List<String>>(emptyList())

        override suspend fun search(query: String, genres: Set<String>): Result<SearchResultSummary> {
            if (shouldFail) {
                return Result.failure(RuntimeException("Network error"))
            }
            return Result.success(searchResult)
        }

        override suspend fun getAvailableGenres(): Result<List<String>> {
            return Result.success(availableGenres)
        }

        override suspend fun getRandomSpotlight(): Result<MediaItemSummary?> {
            return Result.success(spotlightItem)
        }

        override suspend fun getItemsByGenre(genreName: String): Result<List<MediaItemSummary>> {
            return Result.success(emptyList())
        }

        override fun getRecentQueries(): Flow<List<String>> = recentQueries.asStateFlow()

        override suspend fun addRecentQuery(query: String) {
            recentQueries.value = listOf(query) + recentQueries.value
        }

        override suspend fun removeRecentQuery(query: String) {
            recentQueries.value = recentQueries.value - query
        }

        override suspend fun clearRecentQueries() {
            recentQueries.value = emptyList()
        }

        override fun getExploreGenres(): List<GenreCategory> = emptyList()
    }

    private class FakeHandshakeRepository : HandshakeRepository {
        private val _currentSession = MutableStateFlow<UserSession?>(
            UserSession(
                userId = "user-1",
                userName = "TestUser",
                token = "tok-1",
                subtitleLanguagePreference = "ar",
                subtitleMode = "Default"
            )
        )
        override val currentSession = _currentSession.asStateFlow()
        override val serverInfo = MutableStateFlow<ServerInfo?>(null).asStateFlow()

        override suspend fun getServerInfo(): Result<ServerInfo> = Result.success(
            ServerInfo(id = "srv-1", name = "Test", version = "10.8.0", operatingSystem = "Linux")
        )

        override suspend fun authenticate(username: String, password: String): Result<UserSession> =
            Result.success(_currentSession.value!!)

        override suspend fun performHandshake(username: String, password: String): Result<UserSession> =
            Result.success(_currentSession.value!!)

        override suspend fun getUserViews(userId: String?): Result<List<org.jellyfin.mobile.domain.model.LibraryFolder>> =
            Result.success(emptyList())

        override suspend fun clearSession() {
            _currentSession.value = null
        }
    }

    private lateinit var searchRepo: FakeSearchRepository
    private lateinit var handshakeRepo: FakeHandshakeRepository
    private lateinit var useCase: SearchMediaUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        searchRepo = FakeSearchRepository()
        handshakeRepo = FakeHandshakeRepository()
        useCase = SearchMediaUseCase(searchRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoading_populatesDynamicGenresAndSpotlight() = runTest {
        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Idle)
        val idle = state as SearchUiState.Idle
        assertEquals(4, idle.availableGenres.size)
        assertEquals("Dune", idle.spotlightItem?.name)
    }

    @Test
    fun toggleGenre_addsAndRemovesGenresFromSelection() = runTest {
        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        viewModel.processIntent(SearchIntent.ToggleGenre("خيال علمي"))
        advanceTimeBy(100)
        assertTrue(viewModel.selectedGenres.value.contains("خيال علمي"))

        viewModel.processIntent(SearchIntent.ToggleGenre("أكشن"))
        advanceTimeBy(100)
        assertEquals(2, viewModel.selectedGenres.value.size)

        viewModel.processIntent(SearchIntent.ToggleGenre("خيال علمي"))
        advanceTimeBy(100)
        assertFalse(viewModel.selectedGenres.value.contains("خيال علمي"))
        assertTrue(viewModel.selectedGenres.value.contains("أكشن"))
    }

    @Test
    fun openAndCloseGenrePicker_updatesStateCorrectly() = runTest {
        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        assertFalse(viewModel.isGenrePickerOpen.value)

        viewModel.processIntent(SearchIntent.OpenGenrePicker)
        assertTrue(viewModel.isGenrePickerOpen.value)
        assertTrue(viewModel.uiState.value.isGenrePickerOpen)

        viewModel.processIntent(SearchIntent.CloseGenrePicker)
        assertFalse(viewModel.isGenrePickerOpen.value)
        assertFalse(viewModel.uiState.value.isGenrePickerOpen)
    }

    @Test
    fun searchPipeline_combinesQueryAndGenres_emitsSuccess() = runTest {
        searchRepo.searchResult = SearchResultSummary(
            query = "Interstellar",
            topMatch = MediaItemSummary(id = "m-1", name = "Interstellar", type = "Movie"),
            mediaResults = persistentListOf(
                MediaItemSummary(id = "m-1", name = "Interstellar", type = "Movie")
            ),
            peopleResults = persistentListOf(),
            totalCount = 1
        )

        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        viewModel.processIntent(SearchIntent.ToggleGenre("خيال علمي"))
        viewModel.processIntent(SearchIntent.UpdateQuery("Interstellar"))
        advanceTimeBy(350)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Success)
        val success = state as SearchUiState.Success
        assertEquals("Interstellar", success.query)
        assertTrue(success.selectedGenres.contains("خيال علمي"))
        assertEquals("Interstellar", success.topMatch?.name)
    }

    @Test
    fun searchPipeline_onFailure_thenRetry_reTriggersSearchAndSucceeds() = runTest {
        searchRepo.shouldFail = true
        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        viewModel.processIntent(SearchIntent.UpdateQuery("Batman"))
        advanceTimeBy(350)
        advanceUntilIdle()

        val errorState = viewModel.uiState.value
        assertTrue(errorState is SearchUiState.Error)
        assertEquals("Network error", (errorState as SearchUiState.Error).message)

        searchRepo.shouldFail = false
        searchRepo.searchResult = SearchResultSummary(
            query = "Batman",
            topMatch = MediaItemSummary(id = "b-1", name = "Batman", type = "Movie"),
            mediaResults = persistentListOf(),
            peopleResults = persistentListOf(),
            totalCount = 1
        )

        viewModel.processIntent(SearchIntent.Retry)
        advanceUntilIdle()

        val successState = viewModel.uiState.value
        assertTrue(successState is SearchUiState.Success)
        val success = successState as SearchUiState.Success
        assertEquals("Batman", success.query)
        assertEquals("Batman", success.topMatch?.name)
    }
}
