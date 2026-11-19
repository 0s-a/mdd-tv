package org.jellyfin.mobile.presentation.library

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
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
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.domain.model.LibraryFolder
import org.jellyfin.mobile.presentation.core.components.FloatingFrostedTopBar
import org.jellyfin.mobile.presentation.library.components.AlphabetScrubber
import org.jellyfin.mobile.presentation.library.components.FilterChipsRow
import org.jellyfin.mobile.presentation.library.components.LibraryPosterCard
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * LibraryScreen implements the massive catalog infinite 3-column grid.
 * Strictly adheres to docs/v2/02_LIBRARY_BROWSER_V2.md:
 * - Floating frosted glass header (detached / بشكل غير متصل).
 * - Primary screen title is ALWAYS "المكتبة" with item count badge.
 * - Dynamic Library Categories Row to switch between all server libraries seamlessly.
 * - Horizontal quick filter chips row.
 * - Trailing AlphabetScrubber with haptic feedback.
 * - OLED True Black background and WCAG 2.1 AA accessibility.
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSearch: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is LibraryEffect.NavigateToDetails -> onNavigateToDetails(effect.mediaId)
                is LibraryEffect.LaunchPlayer -> { /* Player invocation */ }
                is LibraryEffect.ScrollToIndex -> {
                    gridState.animateScrollToItem(effect.index)
                }
                is LibraryEffect.ShowToast -> { /* Toast notification */ }
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

                    // 2. Horizontal Filter Chips Row
                    FilterChipsRow(
                        activeFilters = state.activeFilters,
                        selectedSort = state.selectedSort,
                        onToggleFilter = { filter ->
                            viewModel.processIntent(LibraryIntent.ToggleFilter(filter))
                        },
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

                    // 3. Grid + Alphabet Scrubber Area
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
                                    text = "لا توجد أعمال تطابق الفلاتر المحددة",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = JellyfinColors.TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.processIntent(LibraryIntent.ResetFilters) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JellyfinColors.PrimaryIndigo,
                                        contentColor = JellyfinColors.TextPrimary
                                    )
                                ) {
                                    Text("إعادة ضبط الفلاتر")
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                state = gridState,
                                contentPadding = PaddingValues(
                                    start = JellyfinDimens.SpaceLarge,
                                    end = 36.dp, // Clearance for trailing AlphabetScrubber
                                    top = JellyfinDimens.SpaceSmall,
                                    bottom = 120.dp // Clearance for floating bottom bar
                                ),
                                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
                                verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = state.items,
                                    key = { it.id }
                                ) { item ->
                                    LibraryPosterCard(
                                        item = item,
                                        onClick = {
                                            viewModel.processIntent(LibraryIntent.OpenMediaDetails(item.id))
                                        }
                                    )
                                }
                            }

                            // Fast Alphabetical Scrubber along the trailing edge
                            AlphabetScrubber(
                                onLetterSelected = { char ->
                                    viewModel.processIntent(LibraryIntent.JumpToLetter(char))
                                },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }

                // 4. Floating Frosted Top Bar (Detached / بشكل غير متصل)
                LibraryFloatingTopBar(
                    totalCount = state.totalItemCount,
                    onBackClick = onNavigateBack,
                    onSearchClick = onNavigateToSearch,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun LibraryFloatingTopBar(
    totalCount: Int,
    onBackClick: (() -> Unit)?,
    onSearchClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    FloatingFrostedTopBar(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(36.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "الرجوع"
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = JellyfinColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.VideoLibrary,
                    contentDescription = null,
                    tint = JellyfinColors.PrimaryIndigo,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "المكتبة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )

            if (totalCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(JellyfinDimens.RadiusPill))
                        .background(JellyfinColors.SurfaceDark)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$totalCount عملاً",
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (onSearchClick != null) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(36.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "البحث في المكتبة"
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = JellyfinColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
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
