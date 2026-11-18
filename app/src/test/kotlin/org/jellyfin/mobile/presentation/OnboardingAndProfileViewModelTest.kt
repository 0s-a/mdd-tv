package org.jellyfin.mobile.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jellyfin.mobile.data.local.entity.LocalFavoriteEntity
import org.jellyfin.mobile.data.local.entity.LocalWatchProgressEntity
import org.jellyfin.mobile.data.local.entity.UserProfileEntity
import org.jellyfin.mobile.data.local.entity.WatchedArchiveEntity
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.jellyfin.mobile.presentation.onboarding.OnboardingViewModel
import org.jellyfin.mobile.presentation.profile.ProfileTab
import org.jellyfin.mobile.presentation.profile.ProfileViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingAndProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class TestLocalProfileRepository : LocalProfileRepository {
        var profile: UserProfileEntity? = null
        val favorites = mutableListOf<LocalFavoriteEntity>()
        val archive = mutableListOf<WatchedArchiveEntity>()

        override suspend fun hasProfile(): Boolean = profile != null
        override fun observeProfile(): Flow<UserProfileEntity?> = flowOf(profile)
        override suspend fun getProfile(): UserProfileEntity? = profile
        override suspend fun createProfile(displayName: String, avatarKey: String) {
            profile = UserProfileEntity(id = "default_user", displayName = displayName, avatarKey = avatarKey)
        }
        override suspend fun updateProfile(displayName: String, avatarKey: String) {
            profile = profile?.copy(displayName = displayName, avatarKey = avatarKey)
        }
        override fun observeFavorites(): Flow<List<LocalFavoriteEntity>> = flowOf(favorites)
        override fun observeFavoriteIds(): Flow<List<String>> = flowOf(favorites.map { it.itemId })
        override fun observeIsFavorite(itemId: String): Flow<Boolean> = flowOf(favorites.any { it.itemId == itemId })
        override suspend fun isFavorite(itemId: String): Boolean = favorites.any { it.itemId == itemId }
        override suspend fun addFavorite(itemId: String, title: String, mediaType: String, releaseYear: String, communityRating: String) {
            favorites.add(LocalFavoriteEntity(itemId, title, mediaType, releaseYear, communityRating))
        }
        override suspend fun removeFavorite(itemId: String) {
            favorites.removeAll { it.itemId == itemId }
        }
        override suspend fun toggleFavorite(itemId: String, title: String, mediaType: String, releaseYear: String, communityRating: String): Boolean {
            val exists = isFavorite(itemId)
            if (exists) removeFavorite(itemId) else addFavorite(itemId, title, mediaType, releaseYear, communityRating)
            return !exists
        }
        override fun observeArchive(): Flow<List<WatchedArchiveEntity>> = flowOf(archive)
        override suspend fun isWatched(itemId: String): Boolean = archive.any { it.itemId == itemId }
        override fun observeIsWatched(itemId: String): Flow<Boolean> = flowOf(archive.any { it.itemId == itemId })
        override suspend fun addToArchive(itemId: String, title: String, mediaType: String) {
            archive.add(WatchedArchiveEntity(itemId, title, mediaType))
        }
        override suspend fun removeFromArchive(itemId: String) {
            archive.removeAll { it.itemId == itemId }
        }
        override fun observeContinueWatching(): Flow<List<LocalWatchProgressEntity>> = flowOf(emptyList())
        override suspend fun getContinueWatching(): List<LocalWatchProgressEntity> = emptyList()
        override suspend fun getProgress(itemId: String): LocalWatchProgressEntity? = null
        override fun observeProgress(itemId: String): Flow<LocalWatchProgressEntity?> = flowOf(null)
        override suspend fun saveWatchProgress(itemId: String, title: String, positionTicks: Long, durationTicks: Long, isPlayed: Boolean) {}
        override suspend fun dismissContinueWatching(itemId: String) {}
        override suspend fun markWatched(itemId: String) {}
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
    fun onboardingViewModel_submitSavesProfile() = runTest {
        val repo = TestLocalProfileRepository()
        val viewModel = OnboardingViewModel(repo)

        viewModel.updateName("سلطان")
        viewModel.selectAvatar("avatar_4")
        viewModel.submit()
        advanceUntilIdle()

        assertTrue(repo.hasProfile())
        assertEquals("سلطان", repo.profile?.displayName)
        assertEquals("avatar_4", repo.profile?.avatarKey)
    }

    @Test
    fun onboardingViewModel_skipCreatesDefaultProfile() = runTest {
        val repo = TestLocalProfileRepository()
        val viewModel = OnboardingViewModel(repo)

        viewModel.skip()
        advanceUntilIdle()

        assertTrue(repo.hasProfile())
        assertEquals("مشاهد مدد", repo.profile?.displayName)
        assertEquals("avatar_1", repo.profile?.avatarKey)
    }

    @Test
    fun profileViewModel_tabsAndSheetToggleCorrectly() = runTest {
        val repo = TestLocalProfileRepository()
        repo.createProfile("فيصل", "avatar_2")
        val viewModel = ProfileViewModel(repo)

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(ProfileTab.FAVORITES, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(ProfileTab.WATCHED_ARCHIVE)
        advanceUntilIdle()
        assertEquals(ProfileTab.WATCHED_ARCHIVE, viewModel.uiState.value.selectedTab)

        viewModel.setEditSheetVisible(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isEditSheetVisible)

        viewModel.updateProfile("فيصل الدوسري", "avatar_6")
        advanceUntilIdle()
        assertEquals("فيصل الدوسري", repo.profile?.displayName)
        assertEquals("avatar_6", repo.profile?.avatarKey)
        assertFalse(viewModel.uiState.value.isEditSheetVisible)

        collectJob.cancel()
    }
}
