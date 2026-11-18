package org.jellyfin.mobile.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.ServerInfo
import org.jellyfin.mobile.domain.model.UserSession

/**
 * Pure Kotlin contract for Jellyfin server handshake, authentication,
 * session lifecycle, and library view discovery.
 */
interface HandshakeRepository {
    /**
     * Observable state flow of the currently authenticated user session.
     * Null when unauthenticated.
     */
    val currentSession: StateFlow<UserSession?>

    /**
     * Observable state flow of discovered server information.
     * Null when server discovery has not completed.
     */
    val serverInfo: StateFlow<ServerInfo?>

    /**
     * Fetches public server metadata from GET /System/Info/Public.
     */
    suspend fun getServerInfo(): Result<ServerInfo>

    /**
     * Authenticates a user by username and password.
     */
    suspend fun authenticate(username: String, password: String): Result<UserSession>

    /**
     * Executes the complete handshake flow:
     * 1. Fetches public server info.
     * 2. Authenticates the default user.
     * 3. Stores session and exposes updated state flows.
     */
    suspend fun performHandshake(
        username: String = "الاستراحه",
        password: String = ""
    ): Result<UserSession>

    /**
     * Discovers top-level media library folders for the active or specified user.
     */
    suspend fun getUserViews(userId: String? = null): Result<List<LibraryFolder>>

    /**
     * Clears active session and stored tokens.
     */
    suspend fun clearSession()
}
