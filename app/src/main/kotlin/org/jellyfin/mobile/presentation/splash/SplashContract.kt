package org.jellyfin.mobile.presentation.splash

import androidx.compose.runtime.Immutable

/**
 * Server connection status on splash launch.
 */
@Immutable
sealed interface ServerStatus {
    data object Checking : ServerStatus
    data class Connected(val serverName: String, val latencyMs: Long) : ServerStatus
    data class Offline(val message: String) : ServerStatus
}

/**
 * State for SplashScreen.
 */
@Immutable
data class SplashUiState(
    val serverStatus: ServerStatus = ServerStatus.Checking,
    val isMinimumTimeElapsed: Boolean = false,
    val hasProfile: Boolean = false
)

/**
 * User or lifecycle intents for Splash.
 */
sealed interface SplashIntent {
    data object Initialize : SplashIntent
    data object Retry : SplashIntent
    data object ContinueOffline : SplashIntent
}

/**
 * One-off navigation effects from Splash.
 */
sealed interface SplashEffect {
    data object NavigateToHome : SplashEffect
    data object NavigateToOnboarding : SplashEffect
}
