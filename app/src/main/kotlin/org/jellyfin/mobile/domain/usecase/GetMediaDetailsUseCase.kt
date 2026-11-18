package org.jellyfin.mobile.domain.usecase

import org.jellyfin.mobile.domain.model.MediaDetails
import org.jellyfin.mobile.domain.repository.MediaDetailsRepository

/**
 * UseCase for retrieving media details for a movie or TV show series.
 * Pure Kotlin with zero android.* dependencies.
 */
class GetMediaDetailsUseCase(
    private val mediaDetailsRepository: MediaDetailsRepository
) {
    suspend operator fun invoke(itemId: String): Result<MediaDetails> {
        return mediaDetailsRepository.getMediaDetails(itemId)
    }
}
