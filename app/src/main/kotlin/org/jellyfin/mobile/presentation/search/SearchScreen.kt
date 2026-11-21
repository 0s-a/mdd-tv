package org.jellyfin.mobile.presentation.search

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.presentation.search.components.FrostedSearchBar
import org.jellyfin.mobile.presentation.search.components.GenrePickerBottomSheet
import org.jellyfin.mobile.presentation.search.components.SearchIdleView
import org.jellyfin.mobile.presentation.search.components.SearchResultsView
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * SearchScreen implements the comprehensive multi-domain instant search experience as a transparent modal window.
 * - Transparent smoked glass overlay with click-to-dismiss on empty background.
 * - Dynamic server genres with multi-selection trailing button.
 * - 100% dynamic server data for idle state and search results.
 * - Mobile-first gestures, auto-keyboard dismiss, and zero static filter ribbons.
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToDetails: (String) -> Unit,
    onLaunchPlayer: (String) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val availableGenres by viewModel.availableGenres.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Smoked Obsidian Glass Scrim
    val overlayScrim = JellyfinColors.PureBlack.copy(alpha = 0.90f)

    // Intercept back press when dynamic genre picker bottom sheet is open
    BackHandler(enabled = uiState.isGenrePickerOpen) {
        viewModel.processIntent(SearchIntent.CloseGenrePicker)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is SearchEffect.NavigateToDetails -> onNavigateToDetails(effect.mediaId)
                is SearchEffect.LaunchPlayer -> onLaunchPlayer(effect.mediaId)
                is SearchEffect.NavigateToPersonFilmography -> {
                    viewModel.processIntent(SearchIntent.UpdateQuery(effect.name))
                }
                is SearchEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SearchEffect.Dismiss -> onNavigateBack()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(overlayScrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onNavigateBack
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        keyboardController?.hide()
                    }
                )
        ) {
            // 1. Frosted Search Bar Header with Trailing Filter Button
            FrostedSearchBar(
                query = query,
                onQueryChange = { newQuery ->
                    viewModel.processIntent(SearchIntent.UpdateQuery(newQuery))
                },
                onClearClick = {
                    viewModel.processIntent(SearchIntent.ClearQuery)
                },
                selectedGenres = uiState.selectedGenres,
                onRemoveGenre = { genre ->
                    viewModel.processIntent(SearchIntent.RemoveGenre(genre))
                },
                onClearGenres = {
                    viewModel.processIntent(SearchIntent.ClearGenres)
                },
                onFilterClick = {
                    viewModel.processIntent(SearchIntent.OpenGenrePicker)
                },
                onBackClick = onNavigateBack,
                autoFocus = true,
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
                            availableGenres = state.availableGenres,
                            spotlightItem = state.spotlightItem,
                            onQueryClick = { selectedQuery ->
                                viewModel.processIntent(SearchIntent.SelectRecentQuery(selectedQuery))
                            },
                            onDeleteQuery = { deletedQuery ->
                                viewModel.processIntent(SearchIntent.DeleteRecentQuery(deletedQuery))
                            },
                            onClearAllQueries = {
                                viewModel.processIntent(SearchIntent.ClearHistory)
                            },
                            onGenreClick = { genre ->
                                viewModel.processIntent(SearchIntent.ToggleGenre(genre))
                            },
                            onQuickPlayClick = { mediaId ->
                                viewModel.processIntent(SearchIntent.QuickPlay(mediaId))
                            },
                            onDetailsClick = { mediaId ->
                                viewModel.processIntent(SearchIntent.OpenDetails(mediaId))
                            }
                        )
                    }

                    is SearchUiState.Searching -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator(
                                color = JellyfinColors.AccentAmber,
                                trackColor = JellyfinColors.SurfaceDark,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(44.dp)
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
                            hasGenres = state.selectedGenres.isNotEmpty(),
                            onClear = {
                                viewModel.processIntent(SearchIntent.ClearQuery)
                                viewModel.processIntent(SearchIntent.ClearGenres)
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is SearchUiState.Error -> {
                        SearchErrorState(
                            message = state.message,
                            onRetry = { viewModel.processIntent(SearchIntent.Retry) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // 3. Dynamic Server Genres Multi-Select Embedded Bottom Sheet
        GenrePickerBottomSheet(
            isVisible = uiState.isGenrePickerOpen,
            availableGenres = availableGenres.toImmutableList(),
            selectedGenres = uiState.selectedGenres,
            onToggleGenre = { genre ->
                viewModel.processIntent(SearchIntent.ToggleGenre(genre))
            },
            onClearGenres = {
                viewModel.processIntent(SearchIntent.ClearGenres)
            },
            onDismiss = {
                viewModel.processIntent(SearchIntent.CloseGenrePicker)
            }
        )
    }
}

@Composable
private fun SearchEmptyState(
    query: String,
    hasGenres: Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier
            .padding(JellyfinDimens.SpaceLarge)
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Prevents clicking the card from dismissing the dialog
            )
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
            val description = if (query.isNotBlank() && hasGenres) {
                "لا توجد نتائج تطابق \"$query\" مع التصنيفات المختارة"
            } else if (query.isNotBlank()) {
                "لا توجد أفلام أو مسلسلات تطابق \"$query\""
            } else {
                "لا توجد أعمال في المكتبة تطابق التصنيفات المختارة"
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = JellyfinColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JellyfinColors.AccentAmber,
                    contentColor = JellyfinColors.PureBlack
                ),
                shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                modifier = Modifier.defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            ) {
                Text(
                    text = "مسح الفلاتر والبحث",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Prevents clicking the card from dismissing the dialog
            )
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
                    containerColor = JellyfinColors.AccentAmber,
                    contentColor = JellyfinColors.PureBlack
                ),
                shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                modifier = Modifier.defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            ) {
                Text(
                    text = "إعادة المحاولة",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
