package org.jellyfin.mobile.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.jellyfin.mobile.domain.repository.LocalProfileRepository
import org.jellyfin.mobile.presentation.navigation.MainNavGraph
import org.jellyfin.mobile.presentation.navigation.Screen
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack
import org.koin.compose.koinInject

/**
 * MainScaffold hosts the primary application navigation shell with a floating frosted
 * glass bottom navigation bar.
 * Strictly adheres to docs/v2/01_HEADER_AND_NAV_V2.md:
 * - 3 tabs only: Home, Library, Downloads.
 * - Hidden in Player, Details, Search, Onboarding, Profile.
 * - Automatically hidden when keyboard is visible (isImeVisible).
 * - Checks local profile on start to route to Onboarding or Home.
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

        val isTopLevelDestination = currentRoute == Screen.Home.route ||
                currentRoute.startsWith("library") ||
                currentRoute == Screen.Downloads.route
        val isImeVisible = WindowInsets.isImeVisible
        val showBottomBar = isTopLevelDestination && !isImeVisible

        if (showBottomBar) {
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
