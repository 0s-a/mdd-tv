package org.jellyfin.mobile.domain.usecase

import org.jellyfin.mobile.domain.model.SearchResultSummary
import org.jellyfin.mobile.domain.repository.SearchRepository

/**
 * UseCase for searching media items and people across the Jellyfin catalog.
 * Pure Kotlin with zero android.* dependencies.
 */
class SearchMediaUseCase(
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke(query: String, genres: Set<String> = emptySet()): Result<SearchResultSummary> {
        return searchRepository.search(query, genres)
    }
}
