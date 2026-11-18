package org.jellyfin.mobile.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.mapper.toDomain
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.data.remote.dto.AuthenticateUserByNameDto
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.domain.model.ServerInfo
import org.jellyfin.mobile.domain.model.UserSession
import org.jellyfin.mobile.domain.repository.HandshakeRepository

/**
 * Concrete implementation of HandshakeRepository managing Jellyfin server discovery,
 * dynamic user authentication, session state, and library view queries.
 */
class HandshakeRepositoryImpl(
    private val apiService: JellyfinApiService,
    private val sessionManager: SessionManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HandshakeRepository {

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    override val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    private val _serverInfo = MutableStateFlow<ServerInfo?>(null)
    override val serverInfo: StateFlow<ServerInfo?> = _serverInfo.asStateFlow()

    override suspend fun getServerInfo(): Result<ServerInfo> = withContext(ioDispatcher) {
        runCatching {
            val response = apiService.getPublicSystemInfo()
            val domainInfo = response.toDomain()
            _serverInfo.value = domainInfo
            domainInfo
        }
    }

    override suspend fun authenticate(
        username: String,
        password: String
    ): Result<UserSession> = withContext(ioDispatcher) {
        runCatching {
            val request = AuthenticateUserByNameDto(
                username = username,
                password = password
            )
            val response = apiService.authenticateByName(request)
            val session = response.toDomain()
            sessionManager.saveSession(
                token = session.token,
                userId = session.userId,
                serverId = response.serverId
            )
            _currentSession.value = session
            session
        }
    }

    override suspend fun performHandshake(
        username: String,
        password: String
    ): Result<UserSession> = withContext(ioDispatcher) {
        runCatching {
            // 1. Fetch server info dynamically
            getServerInfo().getOrThrow()

            // 2. Authenticate user dynamically with fallback to default config
            val userToAuth = username.ifBlank { AppConfig.DEFAULT_USERNAME }
            val passToAuth = if (username.isBlank()) AppConfig.DEFAULT_PASSWORD else password
            authenticate(userToAuth, passToAuth).getOrThrow()
        }
    }

    override suspend fun getUserViews(userId: String?): Result<List<LibraryFolder>> = withContext(ioDispatcher) {
        runCatching {
            val targetUserId = userId
                ?: _currentSession.value?.userId
                ?: sessionManager.getUserId()
                ?: throw IllegalStateException("Cannot fetch user views: No active user session")

            val response = apiService.getUserViews(targetUserId)
            response.toDomain()
        }
    }

    override suspend fun clearSession() = withContext(ioDispatcher) {
        sessionManager.clear()
        _currentSession.value = null
    }
}
