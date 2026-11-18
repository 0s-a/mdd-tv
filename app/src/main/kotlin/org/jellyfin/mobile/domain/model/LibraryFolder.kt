package org.jellyfin.mobile.domain.model

/**
 * Immutable domain representation of a Jellyfin media library folder.
 * Pure Kotlin with zero Android dependencies.
 */
data class LibraryFolder(
    val id: String,
    val name: String,
    val type: String
)
