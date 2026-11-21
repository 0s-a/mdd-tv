package org.jellyfin.mobile.presentation.search

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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

    @Test
    fun quickPlay_whenTargetIsSeries_emitsNavigateToDetails() = runTest {
        searchRepo.searchResult = SearchResultSummary(
            query = "Breaking Bad",
            topMatch = MediaItemSummary(id = "s-101", name = "Breaking Bad", type = "Series"),
            mediaResults = persistentListOf(),
            peopleResults = persistentListOf(),
            totalCount = 1
        )

        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        viewModel.processIntent(SearchIntent.UpdateQuery("Breaking Bad"))
        advanceTimeBy(350)
        advanceUntilIdle()

        val effects = mutableListOf<SearchEffect>()
        val job = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.processIntent(SearchIntent.QuickPlay("s-101"))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects.first() is SearchEffect.NavigateToDetails)
        assertEquals("s-101", (effects.first() as SearchEffect.NavigateToDetails).mediaId)

        job.cancel()
    }

    @Test
    fun quickPlay_whenTargetIsMovie_emitsLaunchPlayer() = runTest {
        searchRepo.searchResult = SearchResultSummary(
            query = "Inception",
            topMatch = MediaItemSummary(id = "m-202", name = "Inception", type = "Movie"),
            mediaResults = persistentListOf(),
            peopleResults = persistentListOf(),
            totalCount = 1
        )

        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        viewModel.processIntent(SearchIntent.UpdateQuery("Inception"))
        advanceTimeBy(350)
        advanceUntilIdle()

        val effects = mutableListOf<SearchEffect>()
        val job = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.processIntent(SearchIntent.QuickPlay("m-202"))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects.first() is SearchEffect.LaunchPlayer)
        assertEquals("m-202", (effects.first() as SearchEffect.LaunchPlayer).mediaId)

        job.cancel()
    }

    @Test
    fun quickPlay_whenTargetIsSeriesInMediaResults_emitsNavigateToDetails() = runTest {
        searchRepo.searchResult = SearchResultSummary(
            query = "Dark",
            topMatch = MediaItemSummary(id = "m-top", name = "Dark Knight", type = "Movie"),
            mediaResults = persistentListOf(
                MediaItemSummary(id = "s-dark", name = "Dark", type = "Series")
            ),
            peopleResults = persistentListOf(),
            totalCount = 2
        )

        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        viewModel.processIntent(SearchIntent.UpdateQuery("Dark"))
        advanceTimeBy(350)
        advanceUntilIdle()

        val effects = mutableListOf<SearchEffect>()
        val job = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.processIntent(SearchIntent.QuickPlay("s-dark"))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects.first() is SearchEffect.NavigateToDetails)
        assertEquals("s-dark", (effects.first() as SearchEffect.NavigateToDetails).mediaId)

        job.cancel()
    }

    @Test
    fun quickPlay_whenTargetIsMovieInMediaResults_emitsLaunchPlayer() = runTest {
        searchRepo.searchResult = SearchResultSummary(
            query = "Matrix",
            topMatch = MediaItemSummary(id = "m-top", name = "Matrix Reloaded", type = "Movie"),
            mediaResults = persistentListOf(
                MediaItemSummary(id = "m-sub", name = "The Matrix", type = "Movie")
            ),
            peopleResults = persistentListOf(),
            totalCount = 2
        )

        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        viewModel.processIntent(SearchIntent.UpdateQuery("Matrix"))
        advanceTimeBy(350)
        advanceUntilIdle()

        val effects = mutableListOf<SearchEffect>()
        val job = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.processIntent(SearchIntent.QuickPlay("m-sub"))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects.first() is SearchEffect.LaunchPlayer)
        assertEquals("m-sub", (effects.first() as SearchEffect.LaunchPlayer).mediaId)

        job.cancel()
    }

    @Test
    fun quickPlay_whenInIdleStateAndSpotlightIsSeries_emitsNavigateToDetails() = runTest {
        searchRepo.spotlightItem = MediaItemSummary(id = "s-spotlight", name = "Succession", type = "Series")
        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        val effects = mutableListOf<SearchEffect>()
        val job = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.processIntent(SearchIntent.QuickPlay("s-spotlight"))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects.first() is SearchEffect.NavigateToDetails)
        assertEquals("s-spotlight", (effects.first() as SearchEffect.NavigateToDetails).mediaId)

        job.cancel()
    }

    @Test
    fun quickPlay_whenInIdleStateAndSpotlightIsMovie_emitsLaunchPlayer() = runTest {
        searchRepo.spotlightItem = MediaItemSummary(id = "m-spotlight", name = "Oppenheimer", type = "Movie")
        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        val effects = mutableListOf<SearchEffect>()
        val job = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.processIntent(SearchIntent.QuickPlay("m-spotlight"))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects.first() is SearchEffect.LaunchPlayer)
        assertEquals("m-spotlight", (effects.first() as SearchEffect.LaunchPlayer).mediaId)

        job.cancel()
    }

    @Test
    fun openPersonProfile_emitsNavigateToPersonFilmography_andUpdatesSearchQuery() = runTest {
        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        val effects = mutableListOf<SearchEffect>()
        val job = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.processIntent(SearchIntent.OpenPersonProfile("p-42", "Cillian Murphy"))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        val effect = effects.first()
        assertTrue(effect is SearchEffect.NavigateToPersonFilmography)
        val navEffect = effect as SearchEffect.NavigateToPersonFilmography
        assertEquals("p-42", navEffect.personId)
        assertEquals("Cillian Murphy", navEffect.name)

        // When the screen handles this effect, it dispatches UpdateQuery(effect.name)
        viewModel.processIntent(SearchIntent.UpdateQuery(navEffect.name))
        advanceTimeBy(350)
        advanceUntilIdle()

        assertEquals("Cillian Murphy", viewModel.query.value)

        job.cancel()
    }

    @Test
    fun openDetails_and_dismissSearch_emitExpectedEffects() = runTest {
        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        val effects = mutableListOf<SearchEffect>()
        val job = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.processIntent(SearchIntent.OpenDetails("m-300"))
        advanceUntilIdle()

        viewModel.processIntent(SearchIntent.DismissSearch)
        advanceUntilIdle()

        assertEquals(2, effects.size)
        assertTrue(effects[0] is SearchEffect.NavigateToDetails)
        assertEquals("m-300", (effects[0] as SearchEffect.NavigateToDetails).mediaId)
        assertTrue(effects[1] is SearchEffect.Dismiss)

        job.cancel()
    }

    @Test
    fun recentQueries_addSelectDeleteAndClear_updatesHistoryCorrectly() = runTest {
        searchRepo.recentQueries.value = listOf("Batman", "Interstellar", "Dune")
        val viewModel = SearchViewModel(useCase, searchRepo, handshakeRepo)
        advanceUntilIdle()

        // Select a recent query
        viewModel.processIntent(SearchIntent.SelectRecentQuery("Batman"))
        assertEquals("Batman", viewModel.query.value)

        // Delete a query
        viewModel.processIntent(SearchIntent.DeleteRecentQuery("Interstellar"))
        advanceUntilIdle()
        assertFalse(searchRepo.recentQueries.value.contains("Interstellar"))

        // Clear query resets query to blank and transitions to Idle
        viewModel.processIntent(SearchIntent.ClearQuery)
        assertEquals("", viewModel.query.value)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is SearchUiState.Idle)

        // Clear all history
        viewModel.processIntent(SearchIntent.ClearHistory)
        advanceUntilIdle()
        assertTrue(searchRepo.recentQueries.value.isEmpty())
    }
}
