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
import androidx.compose.material.icons.rounded.FileDownload
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
import androidx.navigation.navArgument
import org.jellyfin.mobile.presentation.details.MediaDetailsScreen
import org.jellyfin.mobile.presentation.details.MediaDetailsViewModel
import org.jellyfin.mobile.presentation.downloads.DownloadsScreen
import org.jellyfin.mobile.presentation.downloads.DownloadsViewModel
import org.jellyfin.mobile.presentation.home.HomeScreen
import org.jellyfin.mobile.presentation.home.HomeViewModel
import org.jellyfin.mobile.presentation.library.LibraryScreen
import org.jellyfin.mobile.presentation.library.LibraryViewModel
import org.jellyfin.mobile.presentation.main.NavigationTab
import org.jellyfin.mobile.presentation.player.AdvancedPlayerScreen
import org.jellyfin.mobile.presentation.player.PlayerViewModel
import org.jellyfin.mobile.presentation.search.SearchScreen
import org.jellyfin.mobile.presentation.search.SearchViewModel
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String) {
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
    data object Search : Screen(NavigationTab.SEARCH.route)
    data object Downloads : Screen(NavigationTab.DOWNLOADS.route)
    data object Details : Screen("details/{mediaId}") {
        fun createRoute(mediaId: String): String = "details/$mediaId"
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
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
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
                }
            )
        }

        composable(
            route = Screen.Details.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
            val detailsViewModel: MediaDetailsViewModel = koinViewModel()
            MediaDetailsScreen(
                mediaId = mediaId,
                viewModel = detailsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLaunchPlayer = { id, resumePositionMs ->
                    navController.navigate(Screen.Player.createRoute(id, resumePositionMs))
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

        composable(Screen.Search.route) {
            val searchViewModel: SearchViewModel = koinViewModel()
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateToDetails = { mediaId ->
                    navController.navigate(Screen.Details.createRoute(mediaId))
                },
                onLaunchPlayer = { mediaId ->
                    navController.navigate(Screen.Player.createRoute(mediaId, 0L))
                }
            )
        }

        composable(Screen.Downloads.route) {
            val downloadsViewModel: DownloadsViewModel = koinViewModel()
            DownloadsScreen(
                viewModel = downloadsViewModel,
                onLaunchPlayer = { mediaId, positionMs ->
                    navController.navigate(Screen.Player.createRoute(mediaId, positionMs))
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
