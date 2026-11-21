package org.jellyfin.mobile.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.navArgument
import org.jellyfin.mobile.presentation.details.dispatcher.MediaDetailsDispatcher
import org.jellyfin.mobile.presentation.home.HomeScreen
import org.jellyfin.mobile.presentation.home.HomeViewModel
import org.jellyfin.mobile.presentation.library.LibraryScreen
import org.jellyfin.mobile.presentation.library.LibraryViewModel
import org.jellyfin.mobile.presentation.main.NavigationTab
import org.jellyfin.mobile.presentation.onboarding.OnboardingScreen
import org.jellyfin.mobile.presentation.onboarding.OnboardingViewModel
import org.jellyfin.mobile.presentation.player.AdvancedPlayerScreen
import org.jellyfin.mobile.presentation.player.PlayerViewModel
import org.jellyfin.mobile.presentation.profile.ProfileScreen
import org.jellyfin.mobile.presentation.profile.ProfileViewModel
import org.jellyfin.mobile.presentation.search.SearchScreen
import org.jellyfin.mobile.presentation.search.SearchViewModel
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack
import org.jellyfin.mobile.presentation.splash.SplashScreen
import org.jellyfin.mobile.presentation.splash.SplashViewModel
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Profile : Screen("profile")
    data object Home : Screen(NavigationTab.HOME.route)
    data object Library : Screen("library?libraryId={libraryId}&title={title}") {
        fun createRoute(libraryId: String? = null, title: String? = null): String {
            return if (!libraryId.isNullOrBlank()) {
                val encodedTitle = title?.let {
                    try {
                        java.net.URLEncoder.encode(it, "UTF-8")
                    } catch (e: Exception) {
                        it
                    }
                } ?: ""
                "library?libraryId=$libraryId&title=$encodedTitle"
            } else {
                "library"
            }
        }
    }
    data object Search : Screen("search?genre={genre}") {
        fun createRoute(genre: String? = null): String {
            return if (!genre.isNullOrBlank()) {
                val encodedGenre = try {
                    java.net.URLEncoder.encode(genre, "UTF-8")
                } catch (e: Exception) {
                    genre
                }
                "search?genre=$encodedGenre"
            } else {
                "search"
            }
        }
    }
    data object Details : Screen("details/{mediaId}?type={type}") {
        fun createRoute(mediaId: String, type: String? = null): String =
            if (type != null) "details/$mediaId?type=$type" else "details/$mediaId"
    }
    data object Player : Screen("player/{mediaId}?positionMs={positionMs}") {
        fun createRoute(mediaId: String, positionMs: Long = 0L): String =
            "player/$mediaId?positionMs=$positionMs"
    }
}

/**
 * Navigation graph managing the 4 primary tabs of the app.
 */
@Composable
fun MainNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            val splashViewModel: SplashViewModel = koinViewModel()
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            val onboardingViewModel: OnboardingViewModel = koinViewModel()
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = koinViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateToDetails = { mediaId ->
                    navController.navigate(Screen.Details.createRoute(mediaId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = koinViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToDetails = { mediaId ->
                    navController.navigate(Screen.Details.createRoute(mediaId))
                },
                onLaunchPlayer = { mediaId, positionMs ->
                    navController.navigate(Screen.Player.createRoute(mediaId, positionMs))
                },
                onNavigateToLibrary = { libraryId, title ->
                    navController.navigate(Screen.Library.createRoute(libraryId, title))
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(
            route = Screen.Library.route,
            arguments = listOf(
                navArgument("libraryId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val libraryId = backStackEntry.arguments?.getString("libraryId")
            val rawTitle = backStackEntry.arguments?.getString("title")
            val decodedTitle = rawTitle?.let {
                try {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } catch (e: Exception) {
                    it
                }
            }
            val libraryViewModel: LibraryViewModel = koinViewModel()
            androidx.compose.runtime.LaunchedEffect(libraryId, decodedTitle) {
                if (!libraryId.isNullOrBlank()) {
                    libraryViewModel.processIntent(
                        org.jellyfin.mobile.presentation.library.LibraryIntent.SwitchLibrary(
                            libraryId = libraryId,
                            libraryName = decodedTitle.orEmpty()
                        )
                    )
                }
            }
            LibraryScreen(
                viewModel = libraryViewModel,
                onNavigateToDetails = { mediaId ->
                    navController.navigate(Screen.Details.createRoute(mediaId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onLaunchPlayer = { mediaId, positionMs ->
                    navController.navigate(Screen.Player.createRoute(mediaId, positionMs))
                }
            )
        }

        composable(
            route = Screen.Details.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("type") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
            val explicitType = backStackEntry.arguments?.getString("type")
            MediaDetailsDispatcher(
                mediaId = mediaId,
                explicitType = explicitType,
                onNavigateBack = { navController.popBackStack() },
                onLaunchPlayer = { id, resumePositionMs ->
                    navController.navigate(Screen.Player.createRoute(id, resumePositionMs))
                },
                onNavigateToGenre = { genre ->
                    navController.navigate(Screen.Search.createRoute(genre))
                },
                onNavigateToDetails = { childMediaId ->
                    navController.navigate(Screen.Details.createRoute(childMediaId))
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("positionMs") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
            val positionMs = backStackEntry.arguments?.getLong("positionMs") ?: 0L
            val playerViewModel: PlayerViewModel = koinViewModel()
            AdvancedPlayerScreen(
                mediaId = mediaId,
                startPositionMs = positionMs,
                viewModel = playerViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        dialog(
            route = Screen.Search.route,
            arguments = listOf(
                navArgument("genre") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            dialogProperties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) { backStackEntry ->
            val rawGenre = backStackEntry.arguments?.getString("genre")
            val decodedGenre = rawGenre?.let {
                try {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } catch (e: Exception) {
                    it
                }
            }
            val searchViewModel: SearchViewModel = koinViewModel()
            androidx.compose.runtime.LaunchedEffect(decodedGenre) {
                if (!decodedGenre.isNullOrBlank()) {
                    searchViewModel.processIntent(
                        org.jellyfin.mobile.presentation.search.SearchIntent.SelectGenre(decodedGenre)
                    )
                }
            }
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateToDetails = { mediaId ->
                    navController.navigate(Screen.Details.createRoute(mediaId))
                },
                onLaunchPlayer = { mediaId ->
                    navController.navigate(Screen.Player.createRoute(mediaId, 0L))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun PlaceholderTabScreen(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(JellyfinDimens.SpaceLarge)
    ) {
        GlassmorphicCard(
            modifier = Modifier.padding(JellyfinDimens.SpaceLarge)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(JellyfinDimens.SpaceExtraLarge)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = JellyfinColors.PrimaryIndigo,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JellyfinColors.TextSecondary
                )
            }
        }
    }
}
