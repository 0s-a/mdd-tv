package org.jellyfin.mobile.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jellyfin.mobile.domain.repository.HandshakeRepository
import org.jellyfin.mobile.domain.repository.LocalProfileRepository

class SplashViewModel(
    private val handshakeRepository: HandshakeRepository,
    private val localProfileRepository: LocalProfileRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val minDisplayDurationMs: Long = 2200L
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SplashEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        handleIntent(SplashIntent.Initialize)
    }

    fun handleIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.Initialize -> performStartupHandshake()
            SplashIntent.Retry -> performStartupHandshake()
            SplashIntent.OpenDownloads -> {
                viewModelScope.launch {
                    _effects.send(SplashEffect.NavigateToDownloads)
                }
            }
        }
    }

    private fun performStartupHandshake() {
        viewModelScope.launch {
            _uiState.update { it.copy(serverStatus = ServerStatus.Checking) }

            // 1. Concurrently run minimum visual display delay and server handshake
            val minTimeDeferred = async {
                delay(minDisplayDurationMs)
                true
            }

            val handshakeDeferred = async(ioDispatcher) {
                val start = System.currentTimeMillis()
                val handshakeResult = withTimeoutOrNull(3500L) {
                    handshakeRepository.performHandshake()
                }
                val latency = System.currentTimeMillis() - start
                Pair(handshakeResult, latency)
            }

            val (handshakeResult, latencyMs) = handshakeDeferred.await()
            val hasProfile = withContext(ioDispatcher) {
                localProfileRepository.hasProfile()
            }

            if (handshakeResult != null && handshakeResult.isSuccess) {
                val session = handshakeResult.getOrNull()
                val serverName = handshakeRepository.serverInfo.value?.name ?: "Jellyfin"
                _uiState.update {
                    it.copy(
                        serverStatus = ServerStatus.Connected(serverName, latencyMs.coerceAtLeast(1L)),
                        hasProfile = hasProfile
                    )
                }

                // Wait for minimum display time so branding is appreciated smoothly
                minTimeDeferred.await()

                if (hasProfile) {
                    _effects.send(SplashEffect.NavigateToHome)
                } else {
                    _effects.send(SplashEffect.NavigateToOnboarding)
                }
            } else {
                // Server unreachable or timed out
                minTimeDeferred.await()
                val errorMsg = handshakeResult?.exceptionOrNull()?.localizedMessage
                    ?: "انتهت مهلة الاتصال بالخادم"
                _uiState.update {
                    it.copy(
                        serverStatus = ServerStatus.Offline(errorMsg),
                        hasProfile = hasProfile
                    )
                }
            }
        }
    }
}
