package org.jellyfin.mobile.presentation.library

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.FolderSpecial
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.presentation.library.components.AlphabetScrubber
import org.jellyfin.mobile.presentation.library.components.FilterChipsRow
import org.jellyfin.mobile.presentation.library.components.LibraryPosterCard
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * LibraryScreen implements the massive catalog infinite 3-column grid.
 * Strictly adheres to docs/screens/02_LIBRARY_BROWSER.md:
 * - 3-column LazyVerticalGrid optimized for 120Hz smooth scrolling.
 * - Top header with dynamic library name & count.
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Top App Bar Header
                    LibraryTopBar(
                        title = state.currentLibraryName,
                        totalCount = state.totalItemCount,
                        onBackClick = onNavigateBack,
                        onSearchClick = onNavigateToSearch,
                        modifier = Modifier.statusBarsPadding()
                    )

                    // 2. Horizontal Filter Chips Row
                    FilterChipsRow(
                        activeFilters = state.activeFilters,
                        selectedSort = state.selectedSort,
                        onToggleFilter = { filter ->
                            viewModel.processIntent(LibraryIntent.ToggleFilter(filter))
                        },
                        onSortClick = {
                            // Cycle through sort options
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
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = gridState,
                            contentPadding = PaddingValues(
                                start = JellyfinDimens.SpaceLarge,
                                end = 36.dp, // Clearance for trailing AlphabetScrubber
                                top = JellyfinDimens.SpaceSmall,
                                bottom = 100.dp // Clearance for floating bottom bar
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
        }
    }
}

@Composable
private fun LibraryTopBar(
    title: String,
    totalCount: Int,
    onBackClick: (() -> Unit)?,
    onSearchClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = JellyfinDimens.SpaceLarge,
                vertical = JellyfinDimens.SpaceSmall
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(JellyfinDimens.MinTouchTarget)
                        .semantics {
                            role = Role.Button
                            contentDescription = "الرجوع"
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = JellyfinColors.TextPrimary
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
                if (totalCount > 0) {
                    Text(
                        text = "$totalCount عملاً",
                        style = MaterialTheme.typography.labelSmall,
                        color = JellyfinColors.TextSecondary
                    )
                }
            }
        }

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
                    modifier = Modifier.size(24.dp)
                )
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
            Text(
                text = "تعذر تحميل المكتبة",
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
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
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
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = "تحديث",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
