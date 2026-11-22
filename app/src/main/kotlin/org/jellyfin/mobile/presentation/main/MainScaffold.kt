package org.jellyfin.mobile.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.jellyfin.mobile.presentation.navigation.MainNavGraph
import org.jellyfin.mobile.presentation.navigation.Screen
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * MainScaffold hosts the primary application navigation shell with a docked Cinema
 * bottom navigation bar adhering to Obsidian 2.2.0 design tokens.
 *
 * Architecture and design features:
 * - 3 top-level tabs: Home, Library, Profile.
 * - Docked full-width CinemaBottomBar anchored to the bottom.
 * - Automatically hidden in Player, Details, Search, Onboarding, and when IME is visible.
 * - Dynamic bottom clearance provided via LocalBottomClearance CompositionLocal.
 * - Dedicated slot for docking future MiniPlayer directly above CinemaBottomBar.
 * - Dual reselection handling for scroll-to-top and ergonomic interactions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScaffold(
    modifier: Modifier = Modifier
) {
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Splash.route

    val isTopLevelDestination = currentRoute == Screen.Home.route ||
            currentRoute.startsWith("library") ||
            currentRoute == Screen.Profile.route
    val isImeVisible = WindowInsets.isImeVisible
    val showBottomBar = isTopLevelDestination && !isImeVisible

    val bottomClearance = if (showBottomBar) 88.dp else 0.dp

    CompositionLocalProvider(
        LocalBottomClearance provides bottomClearance
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            MainNavGraph(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.fillMaxSize()
            )

            if (showBottomBar) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    // Placeholder slot for future docked MiniPlayer
                    // MiniPlayerContainer(navController = navController)

                    CinemaBottomBar(
                        currentRoute = currentRoute,
                        onTabSelected = { tab ->
                            val targetRoute = tab.route
                            val isAlreadySelected = currentRoute == targetRoute ||
                                (tab == NavigationTab.LIBRARY && currentRoute.startsWith("library"))
                            if (!isAlreadySelected) {
                                navController.navigate(targetRoute) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                NavigationEvents.emitReselect(tab)
                            }
                        },
                        onTabReselected = { tab ->
                            NavigationEvents.emitReselect(tab)
                        }
                    )
                }
            }
        }
    }
}
