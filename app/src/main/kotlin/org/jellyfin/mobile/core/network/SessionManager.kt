package org.jellyfin.mobile.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe in-memory session manager for managing active Jellyfin authentication
 * tokens and user state across OkHttp interceptors and repositories.
 */
class SessionManager {

    private val tokenRef = AtomicReference<String?>(null)
    private val userIdRef = AtomicReference<String?>(null)
    private val serverIdRef = AtomicReference<String?>(null)

    private val _tokenFlow = MutableStateFlow<String?>(null)
    val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    fun getToken(): String? = tokenRef.get()

    fun getUserId(): String? = userIdRef.get()

    fun getServerId(): String? = serverIdRef.get()

    fun saveSession(token: String?, userId: String?, serverId: String? = null) {
        tokenRef.set(token)
        userIdRef.set(userId)
        serverIdRef.set(serverId)
        _tokenFlow.value = token
    }

    fun clear() {
        tokenRef.set(null)
        userIdRef.set(null)
        serverIdRef.set(null)
        _tokenFlow.value = null
    }
}
