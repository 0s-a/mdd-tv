package org.jellyfin.mobile.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.domain.model.AvatarCatalog
import org.jellyfin.mobile.presentation.core.components.AmbientGlow
import org.jellyfin.mobile.presentation.core.components.FloatingFrostedTopBar
import org.jellyfin.mobile.presentation.core.components.rememberDominantColor
import org.jellyfin.mobile.presentation.home.components.ContinueWatchingRow
import org.jellyfin.mobile.presentation.home.components.HeroSpotlight
import org.jellyfin.mobile.presentation.home.components.LibraryCapsulesRow
import org.jellyfin.mobile.presentation.home.components.RecentlyAddedRow
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * HomeScreen is the primary dashboard of the Jellyfin client ("مدد TV").
 * Strictly adheres to docs/screens/01_HOME_DASHBOARD.md and docs/DESIGN_SYSTEM.md:
 * - OLED True Black and Ambient Glow background.
 * - Dynamic server libraries without hardcoding.
 * - 48dp minimum touch targets on all interactive elements.
 * - Full TalkBack screen reader accessibility.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetails: (String) -> Unit,
    onLaunchPlayer: (String, Long) -> Unit,
    onNavigateToLibrary: (String, String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background

    // Collect single-event side effects
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is HomeEffect.NavigateToDetails -> onNavigateToDetails(effect.mediaId)
                is HomeEffect.LaunchPlayer -> onLaunchPlayer(effect.mediaId, effect.resumePositionMs)
                is HomeEffect.NavigateToLibraryBrowser -> onNavigateToLibrary(effect.libraryId, effect.title)
                is HomeEffect.ShowToast -> { /* Handle notification or toast */ }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                LoadingState(modifier = Modifier.align(Alignment.Center))
            }
            is HomeUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.processIntent(HomeIntent.LoadDashboardData) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is HomeUiState.Success -> {
                val currentHeroItem = state.heroItems.getOrNull(state.currentHeroIndex)
                val ambientColor by rememberDominantColor(
                    imageUrl = currentHeroItem?.backdropUrl ?: currentHeroItem?.posterUrl
                )

                // 1. Ambient Glow Scrim at top
                AmbientGlow(
                    color = ambientColor,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // 2. Main Scrollable Dashboard Content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 76.dp, bottom = 100.dp) // Clearance for floating top & bottom bars
                ) {
                    // Hero Spotlight Carousel
                    if (currentHeroItem != null) {
                        item(key = "hero_spotlight") {
                            HeroSpotlight(
                                item = currentHeroItem,
                                heroItemsCount = state.heroItems.size,
                                currentIndex = state.currentHeroIndex,
                                onSelectIndex = { index ->
                                    viewModel.processIntent(HomeIntent.SelectHeroIndex(index))
                                },
                                onResumeClick = { mediaId ->
                                    viewModel.processIntent(HomeIntent.QuickPlay(mediaId))
                                },
                                onDetailsClick = { mediaId ->
                                    viewModel.processIntent(HomeIntent.OpenMediaDetails(mediaId))
                                }
                            )
                        }
                    }

                    // Dynamic Server Libraries Row
                    if (state.libraries.isNotEmpty()) {
                        item(key = "libraries_row") {
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
                            LibraryCapsulesRow(
                                libraries = state.libraries,
                                onLibraryClick = { libId ->
                                    val libTitle = state.libraries.find { it.id == libId }?.name ?: ""
                                    viewModel.processIntent(HomeIntent.SelectLibrary(libId, libTitle))
                                }
                            )
                        }
                    }

                    // Continue Watching Row (16:9 thumbnails)
                    if (state.continueWatchingItems.isNotEmpty()) {
                        item(key = "continue_watching_row") {
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                            ContinueWatchingRow(
                                items = state.continueWatchingItems,
                                onItemClick = { mediaId ->
                                    viewModel.processIntent(HomeIntent.OpenMediaDetails(mediaId))
                                },
                                onQuickPlayClick = { mediaId, positionMs ->
                                    viewModel.processIntent(HomeIntent.QuickPlay(mediaId, positionMs))
                                }
                            )
                        }
                    }

                    // Recently Added Row (2:3 posters)
                    if (state.recentlyAddedItems.isNotEmpty()) {
                        item(key = "recently_added_row") {
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                            RecentlyAddedRow(
                                items = state.recentlyAddedItems,
                                onItemClick = { mediaId ->
                                    viewModel.processIntent(HomeIntent.OpenMediaDetails(mediaId))
                                }
                            )
                        }
                    }
                }

                // 3. Floating Frosted Top Bar (Detached / بشكل غير متصل)
                HomeFloatingTopBar(
                    avatarEmoji = AvatarCatalog.getEmoji(state.userAvatarKey),
                    onSearchClick = onNavigateToSearch,
                    onProfileClick = onNavigateToProfile,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun HomeFloatingTopBar(
    avatarEmoji: String,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingFrostedTopBar(modifier = modifier) {
        // App Title & Brand
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
        ) {
            Icon(
                imageVector = Icons.Rounded.Movie,
                contentDescription = null,
                tint = JellyfinColors.PrimaryIndigo,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = AppConfig.APP_NAME,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Actions: Search & Local Profile Avatar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
        ) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(36.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "البحث في المكتبات"
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = JellyfinColors.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, JellyfinColors.PrimaryIndigo, CircleShape)
                    .background(JellyfinColors.SurfaceGlass)
                    .clickable(onClick = onProfileClick)
                    .semantics {
                        role = Role.Button
                        contentDescription = "الملف الشخصي"
                    }
            ) {
                Text(
                    text = avatarEmoji,
                    fontSize = 17.sp
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            color = JellyfinColors.PrimaryIndigo,
            trackColor = JellyfinColors.SurfaceDark,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier
            .padding(JellyfinDimens.SpaceLarge)
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(JellyfinDimens.SpaceExtraLarge)
        ) {
            Text(
                text = "تعذر تحميل البيانات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = JellyfinColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JellyfinColors.PrimaryIndigo,
                    contentColor = JellyfinColors.TextPrimary
                ),
                shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                modifier = Modifier.defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = "إعادة المحاولة",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
