package org.jellyfin.mobile.core.player.external

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.jellyfin.mobile.data.local.entity.LocalFavoriteEntity
import org.jellyfin.mobile.data.local.entity.LocalWatchProgressEntity
import org.jellyfin.mobile.data.local.entity.UserProfileEntity
import org.jellyfin.mobile.data.local.entity.WatchedArchiveEntity
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test verifying ExternalPlayer contract, result parsing,
 * adaptive estimation, and local-first Room DB progress persistence.
 * Strictly adheres to project rules with zero emojis and ADR 0001 compliance.
 */
class ExternalPlayerTest {

    private class FakeLocalProfileRepository : LocalProfileRepository {
        val savedProgress = mutableMapOf<String, LocalWatchProgressEntity>()
        val watchedItems = mutableSetOf<String>()
        val archivedItems = mutableMapOf<String, String>()

        override suspend fun hasProfile(): Boolean = true
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
        override suspend fun isWatched(itemId: String): Boolean = watchedItems.contains(itemId)
        override fun observeIsWatched(itemId: String): Flow<Boolean> = flowOf(watchedItems.contains(itemId))
        override suspend fun addToArchive(itemId: String, title: String, mediaType: String) {
            archivedItems[itemId] = title
        }
        override suspend fun removeFromArchive(itemId: String) {
            archivedItems.remove(itemId)
        }

        override fun observeContinueWatching(): Flow<List<LocalWatchProgressEntity>> = flowOf(savedProgress.values.toList())
        override suspend fun getContinueWatching(): List<LocalWatchProgressEntity> = savedProgress.values.toList()
        override suspend fun getProgress(itemId: String): LocalWatchProgressEntity? = savedProgress[itemId]
        override fun observeProgress(itemId: String): Flow<LocalWatchProgressEntity?> = flowOf(savedProgress[itemId])

        override suspend fun saveWatchProgress(
            itemId: String,
            title: String,
            positionTicks: Long,
            durationTicks: Long,
            isPlayed: Boolean
        ) {
            savedProgress[itemId] = LocalWatchProgressEntity(
                itemId = itemId,
                title = title,
                positionTicks = positionTicks,
                durationTicks = durationTicks,
                isPlayed = isPlayed
            )
        }

        override suspend fun dismissContinueWatching(itemId: String) {
            savedProgress.remove(itemId)
        }

        override suspend fun markWatched(itemId: String) {
            watchedItems.add(itemId)
        }
    }

    @Test
    fun externalPlayerPayload_createsCorrectMetadata() {
        val subtitles = listOf(
            ExternalSubtitle(
                url = "http://localhost/sub.srt",
                name = "Arabic",
                language = "ar",
                isDefault = true
            )
        )
        val payload = ExternalPlayerPayload(
            mediaId = "media-123",
            title = "Test Movie",
            streamUrl = "http://localhost/stream.mkv",
            resumePositionMs = 50_000L,
            durationMs = 6_000_000L,
            subtitles = subtitles
        )

        assertEquals("media-123", payload.mediaId)
        assertEquals("Test Movie", payload.title)
        assertEquals(50_000L, payload.resumePositionMs)
        assertEquals(1, payload.subtitles.size)
        assertTrue(payload.subtitles.first().isDefault)
    }

    @Test
    fun parseResult_withFallbackEstimation_whenElapsedSufficient() {
        val payload = ExternalPlayerPayload(
            mediaId = "m1",
            title = "Sample",
            streamUrl = "http://localhost/stream",
            resumePositionMs = 10_000L,
            durationMs = 100_000L
        )

        // Elapsed 20 seconds, no intent returned (e.g. VLC)
        val result = ExternalPlayerResultParser.parse(
            payload = payload,
            resultCode = 0,
            data = null,
            elapsedMs = 20_000L
        )

        assertEquals("m1", result.mediaId)
        assertEquals(30_000L, result.positionMs)
        assertEquals(100_000L, result.durationMs)
        assertFalse(result.isCompleted)
    }

    @Test
    fun parseResult_withFallbackEstimation_marksCompletedWhenOverNinetyPercent() {
        val payload = ExternalPlayerPayload(
            mediaId = "m2",
            title = "Sample 2",
            streamUrl = "http://localhost/stream",
            resumePositionMs = 85_000L,
            durationMs = 100_000L
        )

        // Elapsed 10 seconds -> estimated 95_000L (95% > 90%)
        val result = ExternalPlayerResultParser.parse(
            payload = payload,
            resultCode = 0,
            data = null,
            elapsedMs = 10_000L
        )

        assertEquals(95_000L, result.positionMs)
        assertTrue(result.isCompleted)
    }

    @Test
    fun parseResult_abortsImmediately_whenElapsedUnderFiveSeconds() {
        val payload = ExternalPlayerPayload(
            mediaId = "m3",
            title = "Sample 3",
            streamUrl = "http://localhost/stream",
            resumePositionMs = 10_000L,
            durationMs = 100_000L
        )

        // Elapsed 2 seconds (quick close)
        val result = ExternalPlayerResultParser.parse(
            payload = payload,
            resultCode = 0,
            data = null,
            elapsedMs = 2_000L
        )

        assertEquals(10_000L, result.positionMs)
        assertFalse(result.isCompleted)
    }

    @Test
    fun persistResult_savesWatchProgress_andMarksCompletedInRoomDb() = runTest {
        val repository = FakeLocalProfileRepository()
        val manager = ExternalPlayerManagerImpl(repository)

        val completedResult = ExternalPlaybackResult(
            mediaId = "movie-completed",
            positionMs = 95_000L,
            durationMs = 100_000L,
            isCompleted = true
        )

        manager.persistResult(completedResult, "Completed Movie", "Movie")

        // Progress converted to ticks (ms * 10_000)
        val saved = repository.getProgress("movie-completed")
        assertEquals(95_000L * 10_000L, saved?.positionTicks)
        assertEquals(100_000L * 10_000L, saved?.durationTicks)
        assertTrue(saved?.isPlayed == true)

        // Watched and Archive updated
        assertTrue(repository.isWatched("movie-completed"))
        assertEquals("Completed Movie", repository.archivedItems["movie-completed"])
    }

    @Test
    fun persistResult_partiallyWatched_doesNotMarkWatched() = runTest {
        val repository = FakeLocalProfileRepository()
        val manager = ExternalPlayerManagerImpl(repository)

        val partialResult = ExternalPlaybackResult(
            mediaId = "movie-partial",
            positionMs = 45_000L,
            durationMs = 100_000L,
            isCompleted = false
        )

        manager.persistResult(partialResult, "Partial Movie", "Movie")

        val saved = repository.getProgress("movie-partial")
        assertEquals(45_000L * 10_000L, saved?.positionTicks)
        assertFalse(saved?.isPlayed == true)
        assertFalse(repository.isWatched("movie-partial"))
    }
}
