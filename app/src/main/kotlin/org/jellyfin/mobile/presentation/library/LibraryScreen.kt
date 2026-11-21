package org.jellyfin.mobile.presentation.library

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.presentation.core.components.CinemaChildTopBar
import org.jellyfin.mobile.presentation.library.components.BreadcrumbsBar
import org.jellyfin.mobile.presentation.library.components.FolderCard
import org.jellyfin.mobile.presentation.library.components.LibraryPosterCard
import org.jellyfin.mobile.presentation.main.LocalBottomClearance
import org.jellyfin.mobile.presentation.main.NavigationTab
import org.jellyfin.mobile.presentation.main.OnTabReselected
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * LibraryScreen implements the pure hierarchical 3-column media grid.
 * Strictly adheres to docs/current/screens/02_library_browser.md:
 * - Floating frosted glass header (detached / بشكل غير متصل).
 * - Primary screen title dynamically reflects the current active folder.
 * - Dynamic Library Categories Row to switch between all server libraries seamlessly.
 * - Hierarchical BreadcrumbsBar with one-tap navigation and instant back button.
 * - Symmetrical 3-column poster grid with 16dp margins on both sides.
 * - OLED True Black background and WCAG 2.1 AA accessibility.
 * - Zero emojis across all code and comments.
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSearch: (() -> Unit)? = null,
    onLaunchPlayer: ((String, Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background
    val context = LocalContext.current

    val isAtRoot = (uiState as? LibraryUiState.Success)?.isAtRoot ?: true

    // Intercept Android hardware & gesture back button for hierarchical navigation
    BackHandler(enabled = !isAtRoot) {
        viewModel.processIntent(LibraryIntent.NavigateBackInHierarchy)
    }

    OnTabReselected(NavigationTab.LIBRARY) {
        coroutineScope.launch {
            gridState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is LibraryEffect.NavigateToDetails -> onNavigateToDetails(effect.mediaId)
                is LibraryEffect.LaunchPlayer -> {
                    onLaunchPlayer?.invoke(effect.mediaId, effect.resumePositionMs)
                }
                is LibraryEffect.ScrollToIndex -> {
                    gridState.animateScrollToItem(effect.index)
                }
                is LibraryEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (uiState is LibraryUiState.Success) {
        val successState = uiState as LibraryUiState.Success

        val shouldLoadMore by remember {
            derivedStateOf {
                val totalItems = gridState.layoutInfo.totalItemsCount
                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisible >= totalItems - 9
            }
        }
        LaunchedEffect(shouldLoadMore, successState.hasMoreItems, successState.isLoadingMore) {
            if (shouldLoadMore && successState.hasMoreItems && !successState.isLoadingMore) {
                viewModel.processIntent(LibraryIntent.LoadNextPage)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        when (val state = uiState) {
            is LibraryUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        color = JellyfinColors.PrimaryIndigo,
                        trackColor = JellyfinColors.SurfaceDark,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            is LibraryUiState.Error -> {
                LibraryErrorView(
                    message = state.message,
                    onRetry = { viewModel.processIntent(LibraryIntent.Refresh) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is LibraryUiState.Empty -> {
                LibraryEmptyView(
                    message = state.message,
                    onRefresh = { viewModel.processIntent(LibraryIntent.Refresh) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is LibraryUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Clearance for floating top bar
                    Spacer(modifier = Modifier.height(72.dp))

                    // 1. Library Categories Row (الأقسام والمكتبات الحية)
                    if (state.availableLibraries.isNotEmpty()) {
                        LibraryCategoriesRow(
                            libraries = state.availableLibraries,
                            selectedLibraryId = state.selectedLibraryId,
                            onSelectLibrary = { libId, libName ->
                                viewModel.processIntent(LibraryIntent.SwitchLibrary(libId, libName))
                            },
                            modifier = Modifier.padding(vertical = JellyfinDimens.SpaceExtraSmall)
                        )
                    }

                    // 2. Breadcrumbs Bar (مسار المجلدات النقي مع زر الترتيب المدمج)
                    BreadcrumbsBar(
                        breadcrumbs = state.breadcrumbs,
                        onBreadcrumbClick = { index ->
                            viewModel.processIntent(LibraryIntent.JumpToBreadcrumb(index))
                        },
                        onBackClick = {
                            viewModel.processIntent(LibraryIntent.NavigateBackInHierarchy)
                        },
                        selectedSort = state.selectedSort,
                        onSortClick = {
                            val nextSort = when (state.selectedSort) {
                                org.jellyfin.mobile.domain.model.LibrarySortBy.DATE_ADDED -> org.jellyfin.mobile.domain.model.LibrarySortBy.PREMIERE_DATE
                                org.jellyfin.mobile.domain.model.LibrarySortBy.PREMIERE_DATE -> org.jellyfin.mobile.domain.model.LibrarySortBy.NAME
                                org.jellyfin.mobile.domain.model.LibrarySortBy.NAME -> org.jellyfin.mobile.domain.model.LibrarySortBy.RATING
                                org.jellyfin.mobile.domain.model.LibrarySortBy.RATING -> org.jellyfin.mobile.domain.model.LibrarySortBy.DATE_ADDED
                            }
                            viewModel.processIntent(LibraryIntent.ChangeSort(nextSort))
                        }
                    )

                    // 3. Grid Area
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (state.items.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "هذا المجلد فارغ حالياً على الخادم",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = JellyfinColors.TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.processIntent(LibraryIntent.Refresh) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JellyfinColors.PrimaryIndigo,
                                        contentColor = JellyfinColors.TextPrimary
                                    )
                                ) {
                                    Text("تحديث")
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                state = gridState,
                                contentPadding = PaddingValues(
                                    start = JellyfinDimens.SpaceLarge,
                                    end = JellyfinDimens.SpaceLarge,
                                    top = JellyfinDimens.SpaceSmall,
                                    bottom = LocalBottomClearance.current + JellyfinDimens.SpaceMedium
                                ),
                                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
                                verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = state.items,
                                    key = { it.id }
                                ) { item ->
                                    if (item.isFolder) {
                                        FolderCard(
                                            item = item,
                                            onClick = {
                                                viewModel.processIntent(LibraryIntent.NavigateIntoFolder(item.id, item.name))
                                            }
                                        )
                                    } else {
                                        LibraryPosterCard(
                                            item = item,
                                            onClick = {
                                                viewModel.processIntent(LibraryIntent.OpenMediaDetails(item.id))
                                            }
                                        )
                                    }
                                }

                                if (state.isLoadingMore) {
                                    item(span = { GridItemSpan(3) }) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(JellyfinDimens.SpaceMedium),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = JellyfinColors.PrimaryIndigo,
                                                modifier = Modifier.size(28.dp),
                                                strokeWidth = 2.5.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Frosted Top Bar (Detached / بشكل غير متصل) - Always on top
        LibraryFloatingTopBar(
            title = (uiState as? LibraryUiState.Success)?.currentFolderTitle ?: "المكتبة",
            totalCount = (uiState as? LibraryUiState.Success)?.totalItemCount ?: 0,
            onBackClick = if (!isAtRoot) {
                { viewModel.processIntent(LibraryIntent.NavigateBackInHierarchy) }
            } else onNavigateBack,
            onSearchClick = onNavigateToSearch,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun LibraryFloatingTopBar(
    title: String,
    totalCount: Int,
    onBackClick: (() -> Unit)?,
    onSearchClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    CinemaChildTopBar(
        title = title,
        subtitle = if (totalCount > 0) "($totalCount عملاً)" else null,
        onBackClick = onBackClick,
        modifier = modifier,
        actions = {
            if (onSearchClick != null) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(JellyfinDimens.MinTouchTarget)
                        .semantics {
                            role = Role.Button
                            contentDescription = "البحث في المكتبة"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = JellyfinColors.TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun LibraryCategoriesRow(
    libraries: List<LibraryFolder>,
    selectedLibraryId: String?,
    onSelectLibrary: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
    ) {
        items(libraries, key = { it.id }) { lib ->
            val isSelected = lib.id == selectedLibraryId
            val icon = when {
                lib.name.contains("فيلم", ignoreCase = true) || lib.name.contains("أفلام", ignoreCase = true) || lib.type.equals("movies", ignoreCase = true) -> Icons.Rounded.Movie
                lib.name.contains("مسلسل", ignoreCase = true) || lib.type.equals("tvshows", ignoreCase = true) -> Icons.Rounded.Tv
                else -> Icons.Rounded.FolderSpecial
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(JellyfinDimens.RadiusPill))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) JellyfinColors.PrimaryIndigo else JellyfinColors.BorderGlass,
                        shape = RoundedCornerShape(JellyfinDimens.RadiusPill)
                    )
                    .background(
                        if (isSelected) JellyfinColors.PrimaryIndigo.copy(alpha = 0.25f)
                        else JellyfinColors.SurfaceGlass
                    )
                    .clickable { onSelectLibrary(lib.id, lib.name) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) JellyfinColors.PrimaryIndigo else JellyfinColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = lib.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) JellyfinColors.TextPrimary else JellyfinColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryErrorView(
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
            modifier = Modifier.padding(JellyfinDimens.SpaceExtraLarge)
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderSpecial,
                contentDescription = null,
                tint = JellyfinColors.TextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
            Text(
                text = "تعذر تحميل محتوى المكتبة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = JellyfinColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))
                Text(text = "إعادة المحاولة", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun LibraryEmptyView(
    message: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier
            .padding(JellyfinDimens.SpaceLarge)
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(JellyfinDimens.SpaceExtraLarge)
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderSpecial,
                contentDescription = null,
                tint = JellyfinColors.TextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
            Text(
                text = "المكتبة فارغة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = JellyfinColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
            Button(
                onClick = onRefresh,
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
                Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))
                Text(text = "تحديث", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
