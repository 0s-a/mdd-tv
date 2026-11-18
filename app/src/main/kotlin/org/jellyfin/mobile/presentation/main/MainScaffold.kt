package org.jellyfin.mobile.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.jellyfin.mobile.presentation.navigation.MainNavGraph
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * MainScaffold hosts the primary application navigation shell with a floating frosted
 * glass bottom navigation bar.
 * Strictly adheres to docs/DESIGN_SYSTEM.md:
 * - Floating glassmorphism pill with 70% opacity and 1dp cyan glass outline.
 * - Dynamic bottom insets handling (navigationBarsPadding).
 * - True Black OLED background support.
 */
@Composable
fun MainScaffold(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavigationTab.HOME.route

    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Main Screen NavHost
        MainNavGraph(
            navController = navController,
            modifier = Modifier.fillMaxSize()
        )

        // Floating Frosted Glass Bottom Navigation Bar (Hidden when in Fullscreen Player)
        val isPlayerScreen = currentRoute.startsWith("player")
        if (!isPlayerScreen) {
            FloatingBottomNavigationBar(
                currentRoute = currentRoute,
                onTabSelected = { tab ->
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}
