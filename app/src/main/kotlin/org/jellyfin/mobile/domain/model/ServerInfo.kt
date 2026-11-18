package org.jellyfin.mobile.domain.model

/**
 * Immutable domain representation of Jellyfin server details.
 * Pure Kotlin with zero Android dependencies.
 */
data class ServerInfo(
    val id: String,
    val name: String,
    val version: String,
    val operatingSystem: String
)
