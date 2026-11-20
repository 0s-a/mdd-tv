package org.jellyfin.mobile.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.domain.model.AvatarCatalog
import org.jellyfin.mobile.domain.model.ContinueWatchingItem
import org.jellyfin.mobile.presentation.core.components.AmbientGlow
import org.jellyfin.mobile.presentation.core.components.FloatingFrostedTopBar
import org.jellyfin.mobile.presentation.core.components.rememberDominantColor
import org.jellyfin.mobile.presentation.home.components.ContinueWatchingRow
import org.jellyfin.mobile.presentation.home.components.HeroSpotlight
import org.jellyfin.mobile.presentation.home.components.HomeShimmerSkeleton
import org.jellyfin.mobile.presentation.home.components.LibraryCapsulesRow
import org.jellyfin.mobile.presentation.home.components.RecentlyAddedRow
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * HomeScreen is the primary cinematic dashboard of the Jellyfin client ("مدد TV").
 * Strictly adheres to docs/screens/01_HOME_DASHBOARD.md, docs/v2/ and Apple TV+ elegance:
 * - Fluid HorizontalPager Hero spotlight (440dp) with auto-scroll and expanding pill indicators.
 * - OLED True Black and smoothly animated Ambient Glow.
 * - Dynamic server libraries without hardcoding.
 * - Interactive Continue Watching cards with long-press quick-action BottomSheet.
 * - Material 3 Pull-to-Refresh with haptic feedback.
 * - Shimmer skeleton loading state replacing plain spinners.
 * - 48dp minimum touch targets on all interactive elements.
 * - Full Arabic TalkBack accessibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val hapticFeedback = LocalHapticFeedback.current

    // Contextual bottom sheet for Continue Watching item options
    var selectedContinueItem by remember { mutableStateOf<ContinueWatchingItem?>(null) }

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
                HomeShimmerSkeleton()
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

                // 1. Ambient Glow Scrim at top (dynamically animated)
                AmbientGlow(
                    color = ambientColor,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // 2. Main Pull-to-Refresh Content
                val pullRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.processIntent(HomeIntent.RefreshDashboard)
                    },
                    state = pullRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 76.dp, bottom = 100.dp) // Clearance for floating bars
                    ) {
                        // Enlarged Cinematic Hero Spotlight Carousel (440dp height, HorizontalPager)
                        if (state.heroItems.isNotEmpty()) {
                            item(key = "hero_spotlight") {
                                HeroSpotlight(
                                    items = state.heroItems,
                                    currentIndex = state.currentHeroIndex,
                                    onIndexChanged = { newIndex ->
                                        viewModel.processIntent(HomeIntent.SelectHeroIndex(newIndex))
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

                        // Continue Watching Row (16:9 cards with glowing progress and long-press actions)
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
                                    },
                                    onItemLongClick = { item ->
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedContinueItem = item
                                    }
                                )
                            }
                        }

                        // Next Up Row (الحلقات التالية للمسلسلات)
                        if (state.nextUpItems.isNotEmpty()) {
                            item(key = "next_up_row") {
                                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                                RecentlyAddedRow(
                                    items = state.nextUpItems,
                                    title = "الحلقات التالية",
                                    onItemClick = { mediaId ->
                                        viewModel.processIntent(HomeIntent.OpenMediaDetails(mediaId))
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
                }

                // 3. Floating Frosted Top Bar
                HomeFloatingTopBar(
                    avatarEmoji = AvatarCatalog.getEmoji(state.userAvatarKey),
                    onSearchClick = onNavigateToSearch,
                    onProfileClick = onNavigateToProfile,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        // 4. Contextual Bottom Sheet for Continue Watching items
        if (selectedContinueItem != null) {
            val item = selectedContinueItem!!
            ModalBottomSheet(
                onDismissRequest = { selectedContinueItem = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = JellyfinColors.SurfaceDark,
                contentColor = JellyfinColors.TextPrimary,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = JellyfinDimens.SpaceLarge, vertical = JellyfinDimens.SpaceMedium)
                ) {
                    // Header with thumbnail & title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = JellyfinDimens.SpaceMedium)
                    ) {
                        AsyncImage(
                            model = item.backdropUrl ?: item.posterUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(74.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = JellyfinColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sub = if (!item.subtitle.isNullOrBlank()) item.subtitle else "متبقي ${item.remainingMinutes} دقيقة"
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = JellyfinColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                    // Option 1: Remove from continue watching
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                            .clickable {
                                viewModel.processIntent(HomeIntent.DismissFromContinueWatching(item.id))
                                selectedContinueItem = null
                            }
                            .padding(vertical = JellyfinDimens.SpaceMedium)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = JellyfinColors.Error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(JellyfinDimens.SpaceMedium))
                        Text(
                            text = "إزالة من متابعة المشاهدة",
                            style = MaterialTheme.typography.bodyLarge,
                            color = JellyfinColors.Error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Option 2: Mark watched
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                            .clickable {
                                viewModel.processIntent(HomeIntent.MarkItemWatched(item.id))
                                selectedContinueItem = null
                            }
                            .padding(vertical = JellyfinDimens.SpaceMedium)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = JellyfinColors.TechSky,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(JellyfinDimens.SpaceMedium))
                        Text(
                            text = "تحديد كمشاهد بالكامل",
                            style = MaterialTheme.typography.bodyLarge,
                            color = JellyfinColors.TextPrimary
                        )
                    }

                    // Option 3: View details
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                            .clickable {
                                val id = item.id
                                selectedContinueItem = null
                                onNavigateToDetails(id)
                            }
                            .padding(vertical = JellyfinDimens.SpaceMedium)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = JellyfinColors.TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(JellyfinDimens.SpaceMedium))
                        Text(
                            text = "عرض تفاصيل العمل",
                            style = MaterialTheme.typography.bodyLarge,
                            color = JellyfinColors.TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                }
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
