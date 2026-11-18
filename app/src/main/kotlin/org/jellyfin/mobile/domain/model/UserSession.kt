package org.jellyfin.mobile.domain.model

/**
 * Immutable domain representation of authenticated user session and preferences.
 * Pure Kotlin with zero Android dependencies.
 */
data class UserSession(
    val userId: String,
    val userName: String,
    val token: String,
    val subtitleLanguagePreference: String,
    val subtitleMode: String
)
