package org.jellyfin.mobile.domain.usecase

import org.jellyfin.mobile.domain.model.HomeFeed
import org.jellyfin.mobile.domain.repository.HomeRepository

/**
 * Pure Kotlin use case for loading and refreshing the Home Dashboard feed.
 */
class GetHomeFeedUseCase(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<HomeFeed> {
        return homeRepository.getHomeFeed(forceRefresh)
    }
}
