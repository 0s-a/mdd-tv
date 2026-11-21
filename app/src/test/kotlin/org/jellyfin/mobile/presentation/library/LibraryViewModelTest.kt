package org.jellyfin.mobile.presentation.library

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jellyfin.mobile.domain.model.LibraryFilter
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.LibrarySortBy
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.domain.model.ServerInfo
import org.jellyfin.mobile.domain.model.UserSession
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.LibraryPage
import org.jellyfin.mobile.domain.repository.LibraryRepository
import org.jellyfin.mobile.domain.usecase.GetLibraryItemsUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LibraryViewModel with Pure Hierarchical Navigation.
 * Zero emojis across all code and comments.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeHandshakeRepository : HandshakeRepository {
        private val _currentSession = MutableStateFlow<UserSession?>(null)
        override val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

        private val _serverInfo = MutableStateFlow<ServerInfo?>(null)
        override val serverInfo: StateFlow<ServerInfo?> = _serverInfo.asStateFlow()

        var userViews = listOf(
            LibraryFolder(id = "lib-movies", name = "الأفلام", type = "movies"),
            LibraryFolder(id = "lib-shows", name = "المسلسلات", type = "tvshows")
        )

        override suspend fun getServerInfo(): Result<ServerInfo> =
            Result.success(ServerInfo(id = "s1", name = "Server", version = "10.8", operatingSystem = "Linux"))

        override suspend fun authenticate(username: String, password: String): Result<UserSession> =
            Result.success(UserSession("u1", username, "tok", "ar", "Default"))

        override suspend fun performHandshake(username: String, password: String): Result<UserSession> =
            Result.success(UserSession("u1", username, "tok", "ar", "Default"))

        override suspend fun getUserViews(userId: String?): Result<List<LibraryFolder>> =
            Result.success(userViews)

        override suspend fun clearSession() { _currentSession.value = null }
    }

    private class FakeLibraryRepository : LibraryRepository {
        val requests = mutableListOf<String>()
        var itemsToReturn: List<MediaItemSummary> = listOf(
            MediaItemSummary(
                id = "item-1",
                name = "Inception",
                type = "Movie"
            ),
            MediaItemSummary(
                id = "folder-1",
                name = "Action Movies",
                type = "Folder"
            )
        )

        override suspend fun getLibraryItems(
            parentId: String,
            filter: LibraryFilter,
            letter: Char?,
            startIndex: Int,
            limit: Int,
            isHierarchical: Boolean
        ): Result<LibraryPage> {
            requests.add(parentId)
            return Result.success(
                LibraryPage(
                    items = itemsToReturn.toImmutableList(),
                    totalCount = itemsToReturn.size,
                    startIndex = startIndex
                )
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_discoversLibrariesAndLoadsFirstLibraryItems() = runTest(testDispatcher) {
        val fakeHandshake = FakeHandshakeRepository()
        val fakeRepo = FakeLibraryRepository()
        val useCase = GetLibraryItemsUseCase(fakeRepo)

        val viewModel = LibraryViewModel(
            getLibraryItemsUseCase = useCase,
            handshakeRepository = fakeHandshake
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LibraryUiState.Success)
        val success = state as LibraryUiState.Success
        assertEquals("lib-movies", success.currentLibraryId)
        assertEquals("الأفلام", success.currentLibraryName)
        assertEquals(2, success.items.size)
        assertEquals(1, success.breadcrumbs.size)
        assertEquals("الأفلام", success.breadcrumbs.first().name)
    }

    @Test
    fun switchLibrary_updatesLibraryIdAndEmitsScrollToTop() = runTest(testDispatcher) {
        val fakeHandshake = FakeHandshakeRepository()
        val fakeRepo = FakeLibraryRepository()
        val useCase = GetLibraryItemsUseCase(fakeRepo)

        val viewModel = LibraryViewModel(
            getLibraryItemsUseCase = useCase,
            handshakeRepository = fakeHandshake
        )
        advanceUntilIdle()

        val effects = mutableListOf<LibraryEffect>()
        val job = launch { viewModel.effects.collect { effects.add(it) } }

        viewModel.processIntent(LibraryIntent.SwitchLibrary("lib-shows", "المسلسلات"))
        advanceUntilIdle()

        val success = viewModel.uiState.value as LibraryUiState.Success
        assertEquals("lib-shows", success.currentLibraryId)
        assertEquals("المسلسلات", success.currentLibraryName)
        assertEquals(1, success.breadcrumbs.size)
        assertTrue(effects.contains(LibraryEffect.ScrollToIndex(0)))
        job.cancel()
    }

    @Test
    fun navigateIntoFolder_appendsBreadcrumbAndNavigatesDeeper() = runTest(testDispatcher) {
        val fakeHandshake = FakeHandshakeRepository()
        val fakeRepo = FakeLibraryRepository()
        val useCase = GetLibraryItemsUseCase(fakeRepo)

        val viewModel = LibraryViewModel(
            getLibraryItemsUseCase = useCase,
            handshakeRepository = fakeHandshake
        )
        advanceUntilIdle()

        val effects = mutableListOf<LibraryEffect>()
        val job = launch { viewModel.effects.collect { effects.add(it) } }

        viewModel.processIntent(LibraryIntent.NavigateIntoFolder("folder-1", "Action Movies"))
        advanceUntilIdle()

        val success = viewModel.uiState.value as LibraryUiState.Success
        assertEquals(2, success.breadcrumbs.size)
        assertEquals("Action Movies", success.breadcrumbs.last().name)
        assertEquals("folder-1", success.breadcrumbs.last().id)
        assertTrue(fakeRepo.requests.contains("folder-1"))
        assertTrue(effects.contains(LibraryEffect.ScrollToIndex(0)))
        job.cancel()
    }

    @Test
    fun navigateBackInHierarchy_popsBreadcrumbLevel() = runTest(testDispatcher) {
        val fakeHandshake = FakeHandshakeRepository()
        val fakeRepo = FakeLibraryRepository()
        val useCase = GetLibraryItemsUseCase(fakeRepo)

        val viewModel = LibraryViewModel(
            getLibraryItemsUseCase = useCase,
            handshakeRepository = fakeHandshake
        )
        advanceUntilIdle()

        viewModel.processIntent(LibraryIntent.NavigateIntoFolder("folder-1", "Action Movies"))
        advanceUntilIdle()

        val effects = mutableListOf<LibraryEffect>()
        val job = launch { viewModel.effects.collect { effects.add(it) } }

        viewModel.processIntent(LibraryIntent.NavigateBackInHierarchy)
        advanceUntilIdle()

        val success = viewModel.uiState.value as LibraryUiState.Success
        assertEquals(1, success.breadcrumbs.size)
        assertEquals("الأفلام", success.breadcrumbs.first().name)
        assertTrue(effects.contains(LibraryEffect.ScrollToIndex(0)))
        job.cancel()
    }

    @Test
    fun changeSort_updatesSortCriterionAndEmitsScrollToTop() = runTest(testDispatcher) {
        val fakeHandshake = FakeHandshakeRepository()
        val fakeRepo = FakeLibraryRepository()
        val useCase = GetLibraryItemsUseCase(fakeRepo)

        val viewModel = LibraryViewModel(
            getLibraryItemsUseCase = useCase,
            handshakeRepository = fakeHandshake
        )
        advanceUntilIdle()

        val effects = mutableListOf<LibraryEffect>()
        val job = launch { viewModel.effects.collect { effects.add(it) } }

        viewModel.processIntent(LibraryIntent.ChangeSort(LibrarySortBy.RATING))
        advanceUntilIdle()

        val success = viewModel.uiState.value as LibraryUiState.Success
        assertEquals(LibrarySortBy.RATING, success.selectedSort)
        assertTrue(effects.contains(LibraryEffect.ScrollToIndex(0)))
        job.cancel()
    }
}
