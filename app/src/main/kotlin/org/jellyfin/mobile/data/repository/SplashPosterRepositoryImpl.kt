package org.jellyfin.mobile.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.local.dao.SplashPosterDao
import org.jellyfin.mobile.data.local.entity.SplashPosterEntity
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.repository.SplashPosterRepository
import org.jellyfin.mobile.presentation.splash.model.SplashPoster
import org.jellyfin.mobile.presentation.splash.model.SplashPosterCatalog

/**
 * Concrete implementation of SplashPosterRepository using Room for local persistence
 * and JellyfinApiService for syncing dynamic media posters directly from the Jellyfin server.
 */
class SplashPosterRepositoryImpl(
    private val splashPosterDao: SplashPosterDao,
    private val apiService: JellyfinApiService,
    private val sessionManager: SessionManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SplashPosterRepository {

    override fun observePosters(): Flow<List<SplashPoster>> {
        return splashPosterDao.observePosters().map { entities ->
            if (entities.isNotEmpty()) {
                entities.map { it.toDomain() }
            } else {
                SplashPosterCatalog
            }
        }
    }

    override suspend fun getCachedPosters(): List<SplashPoster> = withContext(ioDispatcher) {
        val entities = splashPosterDao.getPosters()
        if (entities.isNotEmpty()) {
            entities.map { it.toDomain() }
        } else {
            SplashPosterCatalog
        }
    }

    override suspend fun syncPostersFromServer(userId: String?): Result<List<SplashPoster>> = withContext(ioDispatcher) {
        runCatching {
            val targetUserId = userId
                ?: sessionManager.getUserId()
                ?: throw IllegalStateException("Cannot sync splash posters: No active user session")

            val response = apiService.getSplashItems(
                userId = targetUserId,
                limit = 20
            )

            val items = response.items.orEmpty().filter { !it.id.isNullOrBlank() }
            if (items.isEmpty()) {
                return@runCatching getCachedPosters()
            }

            val entities = items.mapIndexed { index, item ->
                val title = item.name ?: "Untitled"
                val genre = item.genres?.firstOrNull()?.trim()?.uppercase()
                val badge = when {
                    !genre.isNullOrBlank() -> genre
                    item.type.equals("Movie", ignoreCase = true) -> "CINEMA MOVIE"
                    item.type.equals("Series", ignoreCase = true) -> "TV SERIES"
                    else -> "JELLYFIN STREAM"
                }
                val imageUrl = JellyfinApiService.buildPrimaryImageUrl(
                    itemId = item.id!!,
                    maxWidth = 780,
                    quality = 85
                )

                SplashPosterEntity(
                    id = item.id,
                    title = title,
                    badge = badge,
                    imageUrl = imageUrl,
                    orderIndex = index
                )
            }

            splashPosterDao.replacePosters(entities)
            entities.map { it.toDomain() }
        }
    }

    private fun SplashPosterEntity.toDomain(): SplashPoster {
        return SplashPoster(
            title = title,
            badge = badge,
            imageUrl = imageUrl
        )
    }
}
