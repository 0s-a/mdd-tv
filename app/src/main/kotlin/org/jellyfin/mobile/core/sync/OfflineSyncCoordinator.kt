package org.jellyfin.mobile.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jellyfin.mobile.domain.repository.OfflineActionRepository

/**
 * Monitors device network connectivity and automatically triggers offline action queue flushing
 * as soon as a reliable Internet connection is re-established.
 */
class OfflineSyncCoordinator(
    context: Context,
    private val offlineActionRepository: OfflineActionRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    offlineActionRepository.syncPendingActions()
                }
            }
        }

        networkCallback = callback
        try {
            connectivityManager?.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            // Graceful fallback if network callback registration fails
        }

        // Trigger immediate sync on start if network is currently connected
        triggerSync()
    }

    fun triggerSync() {
        scope.launch {
            offlineActionRepository.syncPendingActions()
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (_: Exception) {
                // Ignore callback unregister errors on shutdown
            }
        }
        networkCallback = null
    }
}
