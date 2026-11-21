package org.jellyfin.mobile.domain

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.domain.model.GenreCategory
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.domain.model.SearchResultSummary
import org.jellyfin.mobile.domain.repository.SearchRepository
import org.jellyfin.mobile.domain.usecase.SearchMediaUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMediaUseCaseTest {

    private class FakeSearchRepository(
        private val searchResult: Result<SearchResultSummary>
    ) : SearchRepository {
        override suspend fun search(query: String, genres: Set<String>): Result<SearchResultSummary> = searchResult
        override suspend fun getAvailableGenres(): Result<List<String>> = Result.success(emptyList())
        override suspend fun getRandomSpotlight(): Result<MediaItemSummary?> = Result.success(null)
        override suspend fun getItemsByGenre(genreName: String): Result<List<MediaItemSummary>> = Result.success(emptyList())
        override fun getRecentQueries(): Flow<List<String>> = emptyFlow()
        override suspend fun addRecentQuery(query: String) {}
        override suspend fun removeRecentQuery(query: String) {}
        override suspend fun clearRecentQueries() {}
        override fun getExploreGenres(): List<GenreCategory> = emptyList()
    }

    @Test
    fun searchMediaUseCase_returnsMatchingResults() = runTest {
        val expected = SearchResultSummary(
            query = "Batman",
            topMatch = MediaItemSummary(id = "m-1", name = "The Batman", type = "Movie"),
            mediaResults = persistentListOf(
                MediaItemSummary(id = "m-1", name = "The Batman", type = "Movie"),
                MediaItemSummary(id = "m-2", name = "The Dark Knight", type = "Movie")
            ),
            peopleResults = persistentListOf(),
            totalCount = 2
        )

        val repo = FakeSearchRepository(Result.success(expected))
        val useCase = SearchMediaUseCase(repo)

        val result = useCase("Batman")
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertEquals("Batman", data?.query)
        assertEquals("The Batman", data?.topMatch?.name)
        assertEquals(2, data?.totalCount)
    }

    @Test
    fun searchMediaUseCase_withMultiGenreFilter_returnsFilteredResults() = runTest {
        val expected = SearchResultSummary(
            query = "Interstellar",
            topMatch = MediaItemSummary(id = "m-1", name = "Interstellar", type = "Movie"),
            mediaResults = persistentListOf(
                MediaItemSummary(id = "m-1", name = "Interstellar", type = "Movie")
            ),
            peopleResults = persistentListOf(),
            totalCount = 1
        )

        val repo = FakeSearchRepository(Result.success(expected))
        val useCase = SearchMediaUseCase(repo)

        val result = useCase("Interstellar", setOf("خيال علمي", "دراما"))
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertEquals("Interstellar", data?.query)
        assertEquals(1, data?.totalCount)
    }

    @Test
    fun searchMediaUseCase_handlesSearchFailure() = runTest {
        val repo = FakeSearchRepository(Result.failure(Exception("Server unreachable")))
        val useCase = SearchMediaUseCase(repo)

        val result = useCase("Avatar")
        assertTrue(result.isFailure)
        assertEquals("Server unreachable", result.exceptionOrNull()?.message)
    }

    @Test
    fun searchMediaUseCase_whenSearchingSeries_returnsSeriesWithoutEpisodes() = runTest {
        val expected = SearchResultSummary(
            query = "Breaking Bad",
            topMatch = MediaItemSummary(id = "s-1", name = "Breaking Bad", type = "Series"),
            mediaResults = persistentListOf(
                MediaItemSummary(id = "s-2", name = "Better Call Saul", type = "Series")
            ),
            peopleResults = persistentListOf(),
            totalCount = 2
        )

        val repo = FakeSearchRepository(Result.success(expected))
        val useCase = SearchMediaUseCase(repo)

        val result = useCase("Breaking Bad")
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertEquals("Breaking Bad", data?.topMatch?.name)
        assertEquals("Series", data?.topMatch?.type)

        // Verify that only Series are present and no Episodes exist in mediaResults
        val hasEpisodes = data?.mediaResults?.any { it.type.equals("Episode", ignoreCase = true) } == true
        assertFalse(hasEpisodes)
        data?.mediaResults?.forEach { item ->
            assertEquals("Series", item.type)
        }
    }

    @Test
    fun searchMediaUseCase_whenSearchingActor_returnsActorInPeopleResults() = runTest {
        val expected = SearchResultSummary(
            query = "Bryan Cranston",
            topMatch = null,
            mediaResults = persistentListOf(
                MediaItemSummary(id = "m-1", name = "Breaking Bad", type = "Series")
            ),
            peopleResults = persistentListOf(
                CastMember(
                    id = "p-1",
                    name = "Bryan Cranston",
                    role = "طاقم العمل",
                    type = "Person",
                    imageUrl = null
                )
            ),
            totalCount = 2
        )

        val repo = FakeSearchRepository(Result.success(expected))
        val useCase = SearchMediaUseCase(repo)

        val result = useCase("Bryan Cranston")
        assertTrue(result.isSuccess)
        val data = result.getOrNull()
        assertEquals(1, data?.peopleResults?.size)
        assertEquals("Bryan Cranston", data?.peopleResults?.first()?.name)
        assertEquals("Person", data?.peopleResults?.first()?.type)
    }
}
