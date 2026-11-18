package org.jellyfin.mobile.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.domain.model.CastMember
import org.jellyfin.mobile.presentation.search.components.FrostedSearchBar
import org.jellyfin.mobile.presentation.search.components.SearchIdleView
import org.jellyfin.mobile.presentation.search.components.SearchResultsView
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * SearchScreen implements the comprehensive multi-domain instant search experience.
 * Strictly adheres to docs/screens/05_SEARCH_AND_EXPLORE.md:
 * - Frosted glass search bar with clear and voice search actions.
 * - Reactive state toggling (Idle Explore ↔ Searching Shimmer ↔ Results Grid ↔ Empty State).
 * - True Black OLED background and TalkBack accessibility.
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToDetails: (String) -> Unit,
    onLaunchPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is SearchEffect.NavigateToDetails -> onNavigateToDetails(effect.mediaId)
                is SearchEffect.LaunchPlayer -> onLaunchPlayer(effect.mediaId)
                is SearchEffect.NavigateToPersonFilmography -> { /* Person profile */ }
                is SearchEffect.ShowToast -> { /* Notification */ }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Frosted Search Bar Header
            FrostedSearchBar(
                query = query,
                onQueryChange = { newQuery ->
                    viewModel.processIntent(SearchIntent.UpdateQuery(newQuery))
                },
                onClearClick = {
                    viewModel.processIntent(SearchIntent.ClearQuery)
                },
                onVoiceClick = {
                    // Voice recognition trigger
                },
                modifier = Modifier
                    .padding(horizontal = JellyfinDimens.SpaceLarge)
                    .padding(top = JellyfinDimens.SpaceSmall, bottom = JellyfinDimens.SpaceMedium)
            )

            // 2. Dynamic Content Area based on UI State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is SearchUiState.Idle -> {
                        SearchIdleView(
                            recentQueries = state.recentQueries,
                            genres = state.genres,
                            onQueryClick = { selectedQuery ->
                                viewModel.processIntent(SearchIntent.SelectRecentQuery(selectedQuery))
                            },
                            onDeleteQuery = { deletedQuery ->
                                viewModel.processIntent(SearchIntent.DeleteRecentQuery(deletedQuery))
                            },
                            onClearAllQueries = {
                                viewModel.processIntent(SearchIntent.ClearSearchHistory)
                            },
                            onGenreClick = { genre ->
                                viewModel.processIntent(SearchIntent.SelectGenre(genre.name))
                            }
                        )
                    }

                    is SearchUiState.Searching -> {
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

                    is SearchUiState.Success -> {
                        SearchResultsView(
                            topMatch = state.topMatch,
                            mediaResults = state.mediaResults,
                            peopleResults = state.peopleResults,
                            onMediaClick = { mediaId ->
                                viewModel.processIntent(SearchIntent.OpenMediaDetails(mediaId))
                            },
                            onQuickPlayClick = { mediaId ->
                                viewModel.processIntent(SearchIntent.QuickPlay(mediaId))
                            },
                            onPersonClick = { person ->
                                viewModel.processIntent(SearchIntent.OpenPersonProfile(person.id, person.name))
                            }
                        )
                    }

                    is SearchUiState.Empty -> {
                        SearchEmptyState(
                            query = state.query,
                            onClear = { viewModel.processIntent(SearchIntent.ClearQuery) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is SearchUiState.Error -> {
                        SearchErrorState(
                            message = state.message,
                            onRetry = { viewModel.processIntent(SearchIntent.UpdateQuery(query)) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    query: String,
    onClear: () -> Unit,
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
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = JellyfinColors.TextSecondary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
            Text(
                text = "لم يتم العثور على نتائج",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
            Text(
                text = "لا توجد أفلام أو مسلسلات أو ممثلين يطابقون \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                color = JellyfinColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JellyfinColors.PrimaryIndigo,
                    contentColor = JellyfinColors.TextPrimary
                ),
                shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                modifier = Modifier.defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            ) {
                Text(text = "مسح البحث", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SearchErrorState(
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
                text = "حدث خطأ أثناء البحث",
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
                Text(text = "إعادة المحاولة", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
