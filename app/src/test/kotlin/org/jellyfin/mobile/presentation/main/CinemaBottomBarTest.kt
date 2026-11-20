package org.jellyfin.mobile.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CinemaBottomBarTest {

    @Test
    fun navigationTab_entriesMatchExpectedRoutesAndTitles() {
        val tabs = NavigationTab.entries

        assertEquals(3, tabs.size)

        assertEquals("home", NavigationTab.HOME.route)
        assertEquals("الرئيسية", NavigationTab.HOME.title)
        assertNotNull(NavigationTab.HOME.selectedIcon)
        assertNotNull(NavigationTab.HOME.unselectedIcon)

        assertEquals("library", NavigationTab.LIBRARY.route)
        assertEquals("المكتبة", NavigationTab.LIBRARY.title)
        assertNotNull(NavigationTab.LIBRARY.selectedIcon)
        assertNotNull(NavigationTab.LIBRARY.unselectedIcon)

        assertEquals("profile", NavigationTab.PROFILE.route)
        assertEquals("الشخصي", NavigationTab.PROFILE.title)
        assertNotNull(NavigationTab.PROFILE.selectedIcon)
        assertNotNull(NavigationTab.PROFILE.unselectedIcon)
    }

    @Test
    fun cinemaNavigationTab_aliasResolvesCorrectly() {
        val aliasTab: CinemaNavigationTab = NavigationTab.HOME
        assertEquals(NavigationTab.HOME, aliasTab)
    }
}
