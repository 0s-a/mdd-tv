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
import org.jellyfin.mobile.domain.repository.SplashPosterRepository

class SplashViewModel(
    private val handshakeRepository: HandshakeRepository,
    private val localProfileRepository: LocalProfileRepository,
    private val splashPosterRepository: SplashPosterRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val minDisplayDurationMs: Long = 1800L
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SplashEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    @Volatile
    private var hasNavigated = false

    init {
        loadCachedPosters()
        handleIntent(SplashIntent.Initialize)
    }

    private fun loadCachedPosters() {
        viewModelScope.launch(ioDispatcher) {
            val cached = splashPosterRepository.getCachedPosters()
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(posters = cached) }
            }
        }
    }

    fun handleIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.Initialize -> performStartupHandshake()
            SplashIntent.Retry -> {
                hasNavigated = false
                performStartupHandshake()
            }
            SplashIntent.MontageFinished -> onMontageFinished()
            SplashIntent.ContinueOffline -> {
                if (hasNavigated) return
                hasNavigated = true
                viewModelScope.launch {
                    if (_uiState.value.hasProfile) {
                        _effects.send(SplashEffect.NavigateToHome)
                    } else {
                        _effects.send(SplashEffect.NavigateToOnboarding)
                    }
                }
            }
        }
    }

    private fun triggerNavigation() {
        if (hasNavigated) return
        hasNavigated = true
        viewModelScope.launch {
            delay(minDisplayDurationMs)
            if (_uiState.value.hasProfile) {
                _effects.send(SplashEffect.NavigateToHome)
            } else {
                _effects.send(SplashEffect.NavigateToOnboarding)
            }
        }
    }

    fun onMontageFinished() {
        if (_uiState.value.montagePhase != MontagePhase.RUNNING) return
        _uiState.update { it.copy(montagePhase = MontagePhase.IMPLODING) }
        viewModelScope.launch {
            delay(400L)
            _uiState.update { it.copy(montagePhase = MontagePhase.SETTLED) }

            // If server is connected and profile exists, give the user minimum display duration before navigating
            if (_uiState.value.serverStatus is ServerStatus.Connected) {
                triggerNavigation()
            }
        }
    }

    private fun performStartupHandshake() {
        viewModelScope.launch {
            _uiState.update { it.copy(serverStatus = ServerStatus.Checking) }

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
                val serverName = handshakeRepository.serverInfo.value?.name ?: "Jellyfin"
                _uiState.update {
                    it.copy(
                        serverStatus = ServerStatus.Connected(serverName, latencyMs.coerceAtLeast(1L)),
                        hasProfile = hasProfile
                    )
                }

                // Sync fresh posters in background from the local Jellyfin server
                viewModelScope.launch(ioDispatcher) {
                    splashPosterRepository.syncPostersFromServer().onSuccess { freshPosters ->
                        if (freshPosters.isNotEmpty()) {
                            _uiState.update { it.copy(posters = freshPosters) }
                        }
                    }
                }

                // If the montage has already settled, transition to home after comfortable display
                if (_uiState.value.montagePhase == MontagePhase.SETTLED) {
                    triggerNavigation()
                }
            } else {
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
