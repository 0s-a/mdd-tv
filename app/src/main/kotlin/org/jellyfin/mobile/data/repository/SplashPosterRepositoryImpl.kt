package org.jellyfin.mobile.data.repository

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.local.dao.LocalFavoriteDao
import org.jellyfin.mobile.data.local.dao.LocalWatchProgressDao
import org.jellyfin.mobile.data.local.dao.SplashPosterDao
import org.jellyfin.mobile.data.local.entity.SplashPosterEntity
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.repository.SplashPosterRepository
import org.jellyfin.mobile.presentation.splash.model.SplashPoster
import org.jellyfin.mobile.presentation.splash.model.SplashPosterCatalog

/**
 * Concrete implementation of SplashPosterRepository using Room for local persistence,
 * dynamic server synchronization with 60/40 personal fusion, dual-asset mapping,
 * and background disk pre-caching for instant zero-latency offline readiness.
 */
class SplashPosterRepositoryImpl(
    private val splashPosterDao: SplashPosterDao,
    private val apiService: JellyfinApiService,
    private val sessionManager: SessionManager,
    private val localFavoriteDao: LocalFavoriteDao? = null,
    private val localWatchProgressDao: LocalWatchProgressDao? = null,
    private val context: Context? = null,
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
                limit = 30
            )

            val rawItems = response.items.orEmpty().filter { !it.id.isNullOrBlank() }
            if (rawItems.isEmpty()) {
                return@runCatching getCachedPosters()
            }

            // 1. Fetch local favorite and watch progress IDs for personal fusion (40% allocation)
            val favoriteIds = localFavoriteDao?.getFavoriteIds()?.toSet() ?: emptySet()
            val inProgressIds = localWatchProgressDao?.getContinueWatching()?.map { it.itemId }?.toSet() ?: emptySet()
            val viewerPreferredIds = favoriteIds + inProgressIds

            // 2. Partition items: viewer favorites/in-progress vs server discoveries
            val preferredItems = rawItems.filter { it.id in viewerPreferredIds }
            val serverDiscoveries = rawItems.filter { it.id !in viewerPreferredIds }

            // 3. Deduplicate by series/franchise so at most one title from a franchise appears
            val seenSeries = mutableSetOf<String>()
            val curatedList = mutableListOf<org.jellyfin.mobile.data.remote.dto.BaseItemDto>()

            fun shouldIncludeItem(item: org.jellyfin.mobile.data.remote.dto.BaseItemDto): Boolean {
                val seriesKey = item.seriesId?.takeIf { it.isNotBlank() }
                    ?: item.seriesName?.takeIf { it.isNotBlank() }
                    ?: item.name?.trim()?.lowercase()
                    ?: return false

                return if (seenSeries.contains(seriesKey)) {
                    false
                } else {
                    seenSeries.add(seriesKey)
                    true
                }
            }

            // Take up to 8 preferred items (40% of 20 target)
            for (item in preferredItems) {
                if (curatedList.size >= 8) break
                if (shouldIncludeItem(item)) {
                    curatedList.add(item)
                }
            }

            // Balance genres: ensure wide variety with at most 2 items per primary genre
            val genreCounts = mutableMapOf<String, Int>()
            curatedList.forEach { item ->
                val primaryGenre = item.genres?.firstOrNull()?.trim()?.uppercase() ?: "GENERAL"
                genreCounts[primaryGenre] = (genreCounts[primaryGenre] ?: 0) + 1
            }

            // Fill remaining slots (up to 20 total) with top server discoveries
            for (item in serverDiscoveries) {
                if (curatedList.size >= 20) break
                val primaryGenre = item.genres?.firstOrNull()?.trim()?.uppercase() ?: "GENERAL"
                val count = genreCounts[primaryGenre] ?: 0
                if (count < 2 && shouldIncludeItem(item)) {
                    curatedList.add(item)
                    genreCounts[primaryGenre] = count + 1
                }
            }

            // Fallback fill if genre limits left slots open
            if (curatedList.size < 12) {
                for (item in serverDiscoveries) {
                    if (curatedList.size >= 20) break
                    if (shouldIncludeItem(item)) {
                        curatedList.add(item)
                    }
                }
            }

            val finalItems = if (curatedList.isNotEmpty()) curatedList else rawItems.take(20)

            // 4. Map to dual-asset entities (Backdrop 16:9 + Primary 2:3)
            val entities = finalItems.mapIndexed { index, item ->
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

                val hasBackdrop = !item.backdropImageTags.isNullOrEmpty() ||
                    !item.parentBackdropImageTags.isNullOrEmpty()
                val backdropItemId = item.parentBackdropItemId ?: item.id!!
                val backdropUrl = if (hasBackdrop) {
                    JellyfinApiService.buildBackdropImageUrl(
                        itemId = backdropItemId,
                        maxWidth = 1920,
                        quality = 85
                    )
                } else {
                    imageUrl
                }

                SplashPosterEntity(
                    id = item.id,
                    title = title,
                    badge = badge,
                    imageUrl = imageUrl,
                    backdropUrl = backdropUrl,
                    rating = item.communityRating ?: 0f,
                    orderIndex = index
                )
            }

            splashPosterDao.replacePosters(entities)

            // 5. Opportunistic background disk pre-caching via Coil
            context?.let { ctx ->
                try {
                    val imageLoader = ctx.imageLoader
                    entities.take(8).forEach { entity ->
                        val posterReq = ImageRequest.Builder(ctx)
                            .data(entity.imageUrl)
                            .memoryCacheKey(entity.imageUrl)
                            .diskCacheKey(entity.imageUrl)
                            .build()
                        imageLoader.enqueue(posterReq)

                        if (entity.backdropUrl.isNotBlank() && entity.backdropUrl != entity.imageUrl) {
                            val backdropReq = ImageRequest.Builder(ctx)
                                .data(entity.backdropUrl)
                                .memoryCacheKey(entity.backdropUrl)
                                .diskCacheKey(entity.backdropUrl)
                                .build()
                            imageLoader.enqueue(backdropReq)
                        }
                    }
                } catch (_: Exception) {
                    // Pre-caching is non-blocking
                }
            }

            entities.map { it.toDomain() }
        }
    }

    private fun SplashPosterEntity.toDomain(): SplashPoster {
        return SplashPoster(
            title = title,
            badge = badge,
            imageUrl = imageUrl,
            backdropUrl = backdropUrl.ifBlank { imageUrl },
            rating = rating
        )
    }
}
