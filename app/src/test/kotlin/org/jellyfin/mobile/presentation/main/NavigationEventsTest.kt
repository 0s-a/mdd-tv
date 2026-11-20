package org.jellyfin.mobile.presentation.main

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationEventsTest {

    @Test
    fun emitReselect_emitsExpectedTabEvents() = runTest {
        val emittedEvents = mutableListOf<NavigationTab>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            NavigationEvents.reselectEvents.collect { tab ->
                emittedEvents.add(tab)
            }
        }

        NavigationEvents.emitReselect(NavigationTab.HOME)
        NavigationEvents.emitReselect(NavigationTab.LIBRARY)
        NavigationEvents.emitReselect(NavigationTab.PROFILE)

        assertEquals(
            listOf(NavigationTab.HOME, NavigationTab.LIBRARY, NavigationTab.PROFILE),
            emittedEvents
        )

        collectJob.cancel()
    }
}
