package org.jellyfin.mobile.domain

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.jellyfin.mobile.domain.model.HomeFeed
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.domain.repository.HomeRepository
import org.jellyfin.mobile.domain.usecase.GetHomeFeedUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetHomeFeedUseCaseTest {

    private class FakeHomeRepository(
        private val result: Result<HomeFeed>
    ) : HomeRepository {
        override suspend fun getHomeFeed(forceRefresh: Boolean): Result<HomeFeed> = result
        override suspend fun getLibraries(): Result<List<LibraryFolder>> = Result.success(emptyList())
        override suspend fun getResumeItems(limit: Int): Result<List<MediaItemSummary>> = Result.success(emptyList())
        override suspend fun getLatestItems(limit: Int, parentId: String?): Result<List<MediaItemSummary>> = Result.success(emptyList())
        override suspend fun markItemWatched(itemId: String): Result<Unit> = Result.success(Unit)
        override suspend fun dismissContinueWatching(itemId: String): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun getHomeFeedUseCase_returnsSuccess() = runTest {
        val expectedFeed = HomeFeed(
            libraries = persistentListOf(LibraryFolder(id = "lib-1", name = "الأفلام", type = "movies")),
            heroItem = null,
            resumeItems = persistentListOf(),
            latestItems = persistentListOf()
        )

        val fakeRepo = FakeHomeRepository(Result.success(expectedFeed))
        val useCase = GetHomeFeedUseCase(fakeRepo)

        val result = useCase(forceRefresh = false)
        assertTrue(result.isSuccess)
        assertEquals("الأفلام", result.getOrNull()?.libraries?.first()?.name)
    }

    @Test
    fun getHomeFeedUseCase_returnsFailure() = runTest {
        val fakeRepo = FakeHomeRepository(Result.failure(Exception("Network error")))
        val useCase = GetHomeFeedUseCase(fakeRepo)

        val result = useCase(forceRefresh = true)
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
