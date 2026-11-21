package org.jellyfin.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.presentation.splash.model.SplashPoster

/**
 * Domain repository contract for splash screen posters.
 * Adheres to Clean Architecture and local-first caching principles.
 */
interface SplashPosterRepository {
    fun observePosters(): Flow<List<SplashPoster>>
    suspend fun getCachedPosters(): List<SplashPoster>
    suspend fun syncPostersFromServer(userId: String? = null): Result<List<SplashPoster>>
}
