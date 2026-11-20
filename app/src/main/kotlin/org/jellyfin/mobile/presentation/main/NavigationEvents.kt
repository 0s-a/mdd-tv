package org.jellyfin.mobile.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * CompositionLocal providing unified bottom clearance padding across screens.
 * Ensures scrollable content (LazyColumn, LazyVerticalGrid, etc.) has sufficient
 * clearance to prevent being obscured by the bottom navigation bar, mini player,
 * and system navigation bars.
 */
val LocalBottomClearance = compositionLocalOf<Dp> { 80.dp }

/**
 * Event dispatcher for navigation tab reselection.
 * Dispatches events when the user taps on an already-selected navigation tab,
 * allowing active screens (such as Home, Library, Profile) to perform ergonomic
 * actions like scrolling to the top or resetting nested navigation stacks.
 */
object NavigationEvents {
    private val _reselectEvents = MutableSharedFlow<NavigationTab>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val reselectEvents: SharedFlow<NavigationTab> = _reselectEvents.asSharedFlow()

    /**
     * Emits a tab reselection event to all active collectors.
     * Uses tryEmit to dispatch synchronously without blocking or suspending.
     *
     * @param tab The navigation tab that was reselected.
     */
    fun emitReselect(tab: NavigationTab) {
        _reselectEvents.tryEmit(tab)
    }
}

/**
 * Composable helper that observes tab reselection events from [NavigationEvents.reselectEvents]
 * and invokes [onReselect] when [targetTab] matches the reselected tab.
 *
 * @param targetTab The tab to listen for reselection events on.
 * @param onReselect Lambda invoked when [targetTab] is reselected (e.g. scroll to top).
 */
@Composable
fun OnTabReselected(
    targetTab: NavigationTab,
    onReselect: () -> Unit
) {
    val currentOnReselect by rememberUpdatedState(onReselect)

    LaunchedEffect(targetTab) {
        NavigationEvents.reselectEvents.collect { tab ->
            if (tab == targetTab) {
                currentOnReselect()
            }
        }
    }
}
