package org.jellyfin.mobile.presentation.splash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jellyfin.mobile.data.local.entity.LocalFavoriteEntity
import org.jellyfin.mobile.data.local.entity.LocalWatchProgressEntity
import org.jellyfin.mobile.data.local.entity.UserProfileEntity
import org.jellyfin.mobile.data.local.entity.WatchedArchiveEntity
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.ServerInfo
import org.jellyfin.mobile.domain.model.UserSession
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.jellyfin.mobile.domain.repository.SplashPosterRepository
import org.jellyfin.mobile.presentation.splash.model.SplashPoster
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeHandshakeRepository : HandshakeRepository {
        var shouldSucceed: Boolean = true
        private val _currentSession = MutableStateFlow<UserSession?>(null)
        override val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

        private val _serverInfo = MutableStateFlow<ServerInfo?>(
            ServerInfo(id = "srv-1", name = "استراحة الرصاص Server", version = "10.8.13", operatingSystem = "Linux")
        )
        override val serverInfo: StateFlow<ServerInfo?> = _serverInfo.asStateFlow()

        override suspend fun getServerInfo(): Result<ServerInfo> {
            return if (shouldSucceed) Result.success(_serverInfo.value!!)
            else Result.failure(Exception("Network unreachable"))
        }

        override suspend fun authenticate(username: String, password: String): Result<UserSession> {
            return if (shouldSucceed) {
                val session = UserSession(userId = "u-1", userName = username, token = "tok-1", subtitleLanguagePreference = "ar", subtitleMode = "Default")
                _currentSession.value = session
                Result.success(session)
            } else {
                Result.failure(Exception("Authentication failed"))
            }
        }

        override suspend fun performHandshake(username: String, password: String): Result<UserSession> {
            return if (shouldSucceed) {
                val session = UserSession(userId = "u-1", userName = username, token = "tok-1", subtitleLanguagePreference = "ar", subtitleMode = "Default")
                _currentSession.value = session
                Result.success(session)
            } else {
                Result.failure(Exception("Connection timed out"))
            }
        }

        override suspend fun getUserViews(userId: String?): Result<List<LibraryFolder>> = Result.success(emptyList())
        override suspend fun clearSession() { _currentSession.value = null }
    }

    private class FakeLocalProfileRepository : LocalProfileRepository {
        var hasProfileValue: Boolean = true

        override suspend fun hasProfile(): Boolean = hasProfileValue
        override fun observeProfile(): Flow<UserProfileEntity?> = flowOf(null)
        override suspend fun getProfile(): UserProfileEntity? = null
        override suspend fun createProfile(displayName: String, avatarKey: String) {}
        override suspend fun updateProfile(displayName: String, avatarKey: String) {}
        override fun observeFavorites(): Flow<List<LocalFavoriteEntity>> = flowOf(emptyList())
        override fun observeFavoriteIds(): Flow<List<String>> = flowOf(emptyList())
        override fun observeIsFavorite(itemId: String): Flow<Boolean> = flowOf(false)
        override suspend fun isFavorite(itemId: String): Boolean = false
        override suspend fun addFavorite(itemId: String, title: String, mediaType: String, releaseYear: String, communityRating: String) {}
        override suspend fun removeFavorite(itemId: String) {}
        override suspend fun toggleFavorite(itemId: String, title: String, mediaType: String, releaseYear: String, communityRating: String): Boolean = false
        override fun observeArchive(): Flow<List<WatchedArchiveEntity>> = flowOf(emptyList())
        override suspend fun isWatched(itemId: String): Boolean = false
        override fun observeIsWatched(itemId: String): Flow<Boolean> = flowOf(false)
        override suspend fun addToArchive(itemId: String, title: String, mediaType: String) {}
        override suspend fun removeFromArchive(itemId: String) {}
        override fun observeContinueWatching(): Flow<List<LocalWatchProgressEntity>> = flowOf(emptyList())
        override suspend fun getContinueWatching(): List<LocalWatchProgressEntity> = emptyList()
        override suspend fun getProgress(itemId: String): LocalWatchProgressEntity? = null
        override fun observeProgress(itemId: String): Flow<LocalWatchProgressEntity?> = flowOf(null)
        override suspend fun saveWatchProgress(itemId: String, title: String, positionTicks: Long, durationTicks: Long, isPlayed: Boolean) {}
        override suspend fun dismissContinueWatching(itemId: String) {}
        override suspend fun markWatched(itemId: String) {}
    }

    private class FakeSplashPosterRepository : SplashPosterRepository {
        var postersList = listOf(
            SplashPoster("Server Movie 1", "ACTION", "http://192.168.200.200:80/Items/1/Images/Primary"),
            SplashPoster("Server Series 1", "DRAMA", "http://192.168.200.200:80/Items/2/Images/Primary")
        )
        var shouldSyncSucceed = true

        override fun observePosters(): Flow<List<SplashPoster>> = flowOf(postersList)
        override suspend fun getCachedPosters(): List<SplashPoster> = postersList
        override suspend fun syncPostersFromServer(userId: String?): Result<List<SplashPoster>> {
            return if (shouldSyncSucceed) Result.success(postersList)
            else Result.failure(Exception("Sync failed"))
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
    fun `when handshake succeeds and profile exists, navigates to Home after min time`() = runTest(testDispatcher) {
        val fakeHandshake = FakeHandshakeRepository()
        val fakeProfile = FakeLocalProfileRepository().apply { hasProfileValue = true }
        val fakePoster = FakeSplashPosterRepository()

        val viewModel = SplashViewModel(
            fakeHandshake,
            fakeProfile,
            fakePoster,
            testDispatcher,
            500L
        )

        val effects = mutableListOf<SplashEffect>()
        val job = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.handleIntent(SplashIntent.MontageFinished)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.serverStatus is ServerStatus.Connected)
        assertEquals(listOf(SplashEffect.NavigateToHome), effects)
        job.cancel()
    }

    @Test
    fun `when handshake succeeds and no profile exists, navigates to Onboarding`() = runTest(testDispatcher) {
        val fakeHandshake = FakeHandshakeRepository()
        val fakeProfile = FakeLocalProfileRepository().apply { hasProfileValue = false }
        val fakePoster = FakeSplashPosterRepository()

        val viewModel = SplashViewModel(
            fakeHandshake,
            fakeProfile,
            fakePoster,
            testDispatcher,
            500L
        )

        val effects = mutableListOf<SplashEffect>()
        val job = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.handleIntent(SplashIntent.MontageFinished)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.serverStatus is ServerStatus.Connected)
        assertEquals(listOf(SplashEffect.NavigateToOnboarding), effects)
        job.cancel()
    }

    @Test
    fun `when handshake fails, updates status to Offline and does not navigate automatically`() = runTest(testDispatcher) {
        val fakeHandshake = FakeHandshakeRepository().apply { shouldSucceed = false }
        val fakeProfile = FakeLocalProfileRepository()
        val fakePoster = FakeSplashPosterRepository()

        val viewModel = SplashViewModel(
            fakeHandshake,
            fakeProfile,
            fakePoster,
            testDispatcher,
            500L
        )

        val effects = mutableListOf<SplashEffect>()
        val job = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.handleIntent(SplashIntent.MontageFinished)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.serverStatus is ServerStatus.Offline)
        assertTrue(effects.isEmpty())

        // Test manual offline continue navigation
        viewModel.handleIntent(SplashIntent.ContinueOffline)
        advanceUntilIdle()

        assertEquals(listOf(SplashEffect.NavigateToHome), effects)
        job.cancel()
    }

    @Test
    fun `when initialized, loads cached posters and updates uiState`() = runTest(testDispatcher) {
        val fakeHandshake = FakeHandshakeRepository()
        val fakeProfile = FakeLocalProfileRepository()
        val fakePoster = FakeSplashPosterRepository()

        val viewModel = SplashViewModel(
            fakeHandshake,
            fakeProfile,
            fakePoster,
            testDispatcher,
            500L
        )

        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.posters.size)
        assertEquals("Server Movie 1", viewModel.uiState.value.posters[0].title)
    }
}
