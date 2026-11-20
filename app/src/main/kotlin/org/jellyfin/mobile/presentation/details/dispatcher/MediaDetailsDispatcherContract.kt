package org.jellyfin.mobile.presentation.details.dispatcher

/**
 * Resolved media types supported by specialized detail screens.
 * Pure Kotlin enum with zero emojis.
 */
enum class ResolvedMediaType {
    MOVIE,
    SERIES,
    BOXSET,
    MUSIC_ALBUM
}

/**
 * UI State for polymorphic media details dispatcher.
 */
sealed interface MediaDetailsDispatcherUiState {
    data object Loading : MediaDetailsDispatcherUiState
    data class Resolved(val mediaType: ResolvedMediaType) : MediaDetailsDispatcherUiState
    data class Error(val message: String) : MediaDetailsDispatcherUiState
}

/**
 * Intents for media details dispatching.
 */
sealed interface MediaDetailsDispatcherIntent {
    data class ResolveType(
        val mediaId: String,
        val explicitType: String? = null
    ) : MediaDetailsDispatcherIntent

    data class Retry(
        val mediaId: String,
        val explicitType: String? = null
    ) : MediaDetailsDispatcherIntent
}
