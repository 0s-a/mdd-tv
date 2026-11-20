package org.jellyfin.mobile.presentation.details.dispatcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jellyfin.mobile.presentation.core.components.CinemaTopBar
import org.jellyfin.mobile.presentation.details.boxset.BoxSetDetailsScreen
import org.jellyfin.mobile.presentation.details.boxset.BoxSetViewModel
import org.jellyfin.mobile.presentation.details.movie.MovieDetailsScreen
import org.jellyfin.mobile.presentation.details.movie.MovieViewModel
import org.jellyfin.mobile.presentation.details.music.MusicAlbumDetailsScreen
import org.jellyfin.mobile.presentation.details.music.MusicAlbumViewModel
import org.jellyfin.mobile.presentation.details.series.SeriesDetailsScreen
import org.jellyfin.mobile.presentation.details.series.SeriesViewModel
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives
import org.koin.androidx.compose.koinViewModel

/**
 * MediaDetailsDispatcher is the root entrypoint for all `/details/{mediaId}` navigation.
 * It dynamically probes item metadata and delegates presentation to the corresponding
 * dedicated screen (Movie, Series, BoxSet, or MusicAlbum) to keep memory footprint minimal
 * and eliminate monolithic screen bloat.
 * Zero-emoji compliance.
 */
@Composable
fun MediaDetailsDispatcher(
    mediaId: String,
    onNavigateBack: () -> Unit,
    onLaunchPlayer: (String, Long) -> Unit,
    explicitType: String? = null,
    onNavigateToGenre: (String) -> Unit = {},
    onNavigateToDetails: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MediaDetailsDispatcherViewModel = koinViewModel(key = "dispatcher_$mediaId")
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(mediaId, explicitType) {
        viewModel.processIntent(MediaDetailsDispatcherIntent.ResolveType(mediaId, explicitType))
    }

    when (val state = uiState) {
        is MediaDetailsDispatcherUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(JellyfinPrimitives.BlackOled)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = JellyfinColors.TextPrimary
                )
                CinemaTopBar.Child(
                    title = "",
                    onBackClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        is MediaDetailsDispatcherUiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(JellyfinPrimitives.BlackOled)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(JellyfinDimens.SpaceLg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = JellyfinColors.Error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMd))
                    Button(
                        onClick = {
                            viewModel.processIntent(
                                MediaDetailsDispatcherIntent.Retry(mediaId, explicitType)
                            )
                        }
                    ) {
                        Text(text = "إعادة المحاولة")
                    }
                }
                CinemaTopBar.Child(
                    title = "",
                    onBackClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        is MediaDetailsDispatcherUiState.Resolved -> {
            when (state.mediaType) {
                ResolvedMediaType.MOVIE -> {
                    val movieViewModel: MovieViewModel = koinViewModel(key = "movie_$mediaId")
                    MovieDetailsScreen(
                        movieId = mediaId,
                        viewModel = movieViewModel,
                        onNavigateBack = onNavigateBack,
                        onLaunchPlayer = onLaunchPlayer,
                        onNavigateToGenre = onNavigateToGenre,
                        modifier = modifier
                    )
                }

                ResolvedMediaType.SERIES -> {
                    val seriesViewModel: SeriesViewModel = koinViewModel(key = "series_$mediaId")
                    SeriesDetailsScreen(
                        seriesId = mediaId,
                        viewModel = seriesViewModel,
                        onNavigateBack = onNavigateBack,
                        onLaunchPlayer = onLaunchPlayer,
                        onNavigateToGenre = onNavigateToGenre,
                        modifier = modifier
                    )
                }

                ResolvedMediaType.BOXSET -> {
                    val boxSetViewModel: BoxSetViewModel = koinViewModel(key = "boxset_$mediaId")
                    BoxSetDetailsScreen(
                        boxSetId = mediaId,
                        viewModel = boxSetViewModel,
                        onNavigateBack = onNavigateBack,
                        onPlayMovie = onLaunchPlayer,
                        onOpenMovieDetails = onNavigateToDetails,
                        modifier = modifier
                    )
                }

                ResolvedMediaType.MUSIC_ALBUM -> {
                    val musicViewModel: MusicAlbumViewModel = koinViewModel(key = "music_$mediaId")
                    MusicAlbumDetailsScreen(
                        albumId = mediaId,
                        viewModel = musicViewModel,
                        onNavigateBack = onNavigateBack,
                        onLaunchPlayer = onLaunchPlayer,
                        modifier = modifier
                    )
                }
            }
        }
    }
}
