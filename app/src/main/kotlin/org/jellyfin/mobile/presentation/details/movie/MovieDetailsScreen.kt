package org.jellyfin.mobile.presentation.details.movie

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.core.util.BidiFormatter
import org.jellyfin.mobile.domain.model.MediaItemSummary
import org.jellyfin.mobile.presentation.core.components.AmbientGlow
import org.jellyfin.mobile.presentation.core.components.rememberDominantColor
import org.jellyfin.mobile.presentation.details.components.AdaptiveBackdropHeader
import org.jellyfin.mobile.presentation.details.components.CastCarousel
import org.jellyfin.mobile.presentation.details.components.GenreChipsRow
import org.jellyfin.mobile.presentation.details.components.InfuseTechBadgesRow
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack
import java.util.Locale

/**
 * Dedicated Movie Details Screen for mobile touch portrait experience.
 * Strictly adheres to docs/current/screens/media_details/02_movie_details_screen.md.
 * Features:
 * - Constrained 220dp AdaptiveBackdropHeader with dual scrims and adaptive artwork fallback
 * - Ambient glow matching dominant artwork color
 * - Grand primary action Play/Resume button with progress bar and thumb-friendly 56dp height
 * - Quick action bar: download, trailer, favorite, watched, and audio/subtitles sheet
 * - Expandable synopsis section
 * - Horizontal chapters row (visible when chapters exist, safely omitted when missing)
 * - Cast carousel
 * - Recommended similar movies
 * - Modal bottom sheet for audio and subtitle track selections
 * Absolute zero emojis throughout all code and presentation strings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailsScreen(
    movieId: String,
    viewModel: MovieViewModel,
    onNavigateBack: () -> Unit,
    onLaunchPlayer: (String, Long) -> Unit,
    onNavigateToGenre: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background
    val context = LocalContext.current

    var isSynopsisExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(movieId) {
        viewModel.processIntent(MovieIntent.LoadMovie(movieId))
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is MovieEffect.LaunchPlayer -> {
                    onLaunchPlayer(effect.mediaId, effect.resumePositionMs)
                }
                is MovieEffect.OpenTrailerPlayer -> {
                    Toast.makeText(context, "تشغيل الإعلان الترويجي", Toast.LENGTH_SHORT).show()
                }
                is MovieEffect.NavigateToActorFilmography -> {
                    Toast.makeText(context, "سيرة الفنان: ${effect.name}", Toast.LENGTH_SHORT).show()
                }
                is MovieEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MovieEffect.ShowSnackbar -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MovieEffect.ShareMovie -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "${effect.title}\n${effect.shareUrl}")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "مشاركة الفيلم")
                    context.startActivity(shareIntent)
                }
                is MovieEffect.NavigateToMovieDetails -> {
                    viewModel.processIntent(MovieIntent.LoadMovie(effect.movieId))
                }
            }
        }
    }

    // Refresh state when resuming to update watch progress and status
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.processIntent(MovieIntent.RefreshPlaybackState)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        when (val state = uiState) {
            is MovieUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        color = JellyfinPrimitives.Ivory50,
                        trackColor = JellyfinColors.SurfaceDark,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            is MovieUiState.Error -> {
                MovieErrorView(
                    message = state.message,
                    onRetry = { viewModel.processIntent(MovieIntent.RetryLoad) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is MovieUiState.Success -> {
                val movie = state.movie
                val dominantColor by rememberDominantColor(imageUrl = movie.backdropUrl ?: movie.posterUrl)

                // 1. Dynamic Ambient Glow at top
                AmbientGlow(
                    color = dominantColor,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // 2. Main Scrollable Container
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 80.dp)
                ) {
                    // Header banner constrained to 220dp with dual scrims
                    AdaptiveBackdropHeader(
                        backdropUrl = movie.backdropUrl,
                        posterUrl = movie.posterUrl,
                        title = movie.title,
                        maxHeight = 220.dp,
                        backgroundColor = backgroundColor
                    )

                    // Content Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = JellyfinDimens.SpaceLarge)
                    ) {
                        // Title with BiDi protection
                        Text(
                            text = BidiFormatter.wrapLtr(movie.title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = JellyfinColors.TextPrimary
                        )

                        if (!movie.tagline.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = movie.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = JellyfinColors.TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Metadata Row: Rating • Year • Runtime • Official Rating
                        MovieMetadataRow(movie = movie)

                        // Interactive Genre Chips Row
                        if (movie.genres.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
                            GenreChipsRow(
                                genres = movie.genres,
                                onGenreClick = onNavigateToGenre
                            )
                        }

                        // Monochrome Technical Badges
                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceExtraSmall))
                        InfuseTechBadgesRow(mediaItem = movie)

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

                        // Big Primary Action Button (Play / Resume with Progress)
                        MoviePrimaryActionButton(
                            movie = movie,
                            onResumeClick = { viewModel.processIntent(MovieIntent.Resume) },
                            onPlayClick = { viewModel.processIntent(MovieIntent.Play) }
                        )

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

                        // Quick Action Buttons Row (Download, Trailer, Favorite, Watched, Options)
                        MovieQuickActionBar(
                            state = state,
                            onDownloadClick = { viewModel.processIntent(MovieIntent.ToggleDownload) },
                            onTrailerClick = { viewModel.processIntent(MovieIntent.OpenTrailer) },
                            onFavoriteClick = { viewModel.processIntent(MovieIntent.ToggleFavorite) },
                            onWatchedClick = { viewModel.processIntent(MovieIntent.ToggleWatched) },
                            onOptionsClick = { viewModel.processIntent(MovieIntent.OpenAudioSubtitleSheet) }
                        )

                        // Expandable Synopsis Section
                        if (movie.overview.isNotBlank()) {
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                            Text(
                                text = "القصة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = JellyfinColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                            ) {
                                Text(
                                    text = movie.overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = JellyfinColors.TextSecondary,
                                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isSynopsisExpanded) "إخفاء" else "قراءة المزيد...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JellyfinColors.TechSky,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Chapters Section (Safely hidden if empty per spec)
                    if (state.chapters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                        MovieChaptersRow(
                            chapters = state.chapters,
                            onChapterClick = { chapter ->
                                viewModel.processIntent(MovieIntent.PlayChapter(chapter))
                            }
                        )
                    }

                    // Cast Carousel
                    if (state.castList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                        CastCarousel(
                            castList = state.castList,
                            onCastClick = { member ->
                                viewModel.processIntent(
                                    MovieIntent.OpenActorFilmography(member.id, member.name)
                                )
                            }
                        )
                    }

                    // Similar / Recommended Movies Section
                    if (state.similarMovies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                        SimilarMoviesRow(
                            movies = state.similarMovies,
                            onMovieClick = { selectedMovie ->
                                viewModel.processIntent(MovieIntent.LoadMovie(selectedMovie.id))
                            }
                        )
                    }
                }

                // 3. Floating Transparent Top App Bar (Back, Share, Favorite)
                MovieTopBar(
                    isFavorite = state.isFavorite,
                    onBackClick = onNavigateBack,
                    onShareClick = { viewModel.processIntent(MovieIntent.ShareMovie) },
                    onFavoriteClick = { viewModel.processIntent(MovieIntent.ToggleFavorite) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                )

                // 4. Modal Options Bottom Sheet for Audio & Subtitles
                if (state.isAudioSubtitleSheetVisible) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.processIntent(MovieIntent.DismissAudioSubtitleSheet) },
                        sheetState = sheetState,
                        containerColor = JellyfinPrimitives.Graphite900,
                        scrimColor = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(topStart = JellyfinDimens.RadiusLarge, topEnd = JellyfinDimens.RadiusLarge)
                    ) {
                        AudioSubtitleBottomSheetContent(
                            audioTracks = state.audioTracks,
                            subtitleTracks = state.subtitleTracks,
                            selectedAudioTrack = state.selectedAudioTrack,
                            selectedSubtitleTrack = state.selectedSubtitleTrack,
                            onSelectAudio = { track ->
                                viewModel.processIntent(MovieIntent.SelectAudioTrack(track))
                            },
                            onSelectSubtitle = { track ->
                                viewModel.processIntent(MovieIntent.SelectSubtitleTrack(track))
                            },
                            onClose = {
                                viewModel.processIntent(MovieIntent.DismissAudioSubtitleSheet)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieMetadataRow(movie: org.jellyfin.mobile.domain.model.MediaDetails) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
    ) {
        if (movie.communityRating != null && movie.communityRating > 0f) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = JellyfinColors.RatingGold,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = String.format(Locale.US, "%.1f IMDb", movie.communityRating),
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.RatingGold,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(text = "•", color = JellyfinColors.TextMuted)
        }

        if (movie.productionYear != null) {
            Text(
                text = movie.productionYear.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = JellyfinColors.TextSecondary
            )
            Text(text = "•", color = JellyfinColors.TextMuted)
        }

        val runtimeFormatted = movie.runtimeFormatted
        if (!runtimeFormatted.isNullOrBlank()) {
            Text(
                text = runtimeFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = JellyfinColors.TextSecondary
            )
        }

        if (!movie.officialRating.isNullOrBlank()) {
            if (!runtimeFormatted.isNullOrBlank() || movie.productionYear != null) {
                Text(text = "•", color = JellyfinColors.TextMuted)
            }
            Text(
                text = movie.officialRating,
                style = MaterialTheme.typography.bodySmall,
                color = JellyfinColors.TextSecondary
            )
        }
    }
}

@Composable
private fun MoviePrimaryActionButton(
    movie: org.jellyfin.mobile.domain.model.MediaDetails,
    onResumeClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val isResume = movie.resumePositionMs > 0L
    val progress = if (movie.totalDurationMs > 0L) {
        (movie.resumePositionMs.toFloat() / movie.totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val remainingMinutes = if (isResume && movie.totalDurationMs > movie.resumePositionMs) {
        ((movie.totalDurationMs - movie.resumePositionMs) / 60000L).coerceAtLeast(1L)
    } else null

    val buttonText = when {
        isResume && remainingMinutes != null -> "استئناف الفيلم (متبقي $remainingMinutes دقيقة)"
        isResume -> "استئناف الفيلم"
        else -> "تشغيل الفيلم"
    }

    Button(
        onClick = if (isResume) onResumeClick else onPlayClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = JellyfinPrimitives.Ivory50,
            contentColor = JellyfinPrimitives.Obsidian950
        ),
        shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .shadow(
                elevation = JellyfinDimens.ElevationHigh,
                shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                ambientColor = Color.White.copy(alpha = 0.1f),
                spotColor = Color.White.copy(alpha = 0.2f)
            )
            .semantics {
                role = Role.Button
                contentDescription = buttonText
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = JellyfinPrimitives.Obsidian950,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinPrimitives.Obsidian950
                )
            }

            if (isResume && progress > 0f) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = JellyfinPrimitives.CrimsonMuted,
                    trackColor = JellyfinPrimitives.Slate700,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun MovieQuickActionBar(
    state: MovieUiState.Success,
    onDownloadClick: () -> Unit,
    onTrailerClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
    ) {
        // Download action button
        QuickActionItem(
            icon = {
                when (state.downloadStatus) {
                    is MovieDownloadStatus.Downloading -> {
                        CircularProgressIndicator(
                            color = JellyfinPrimitives.Ivory50,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is MovieDownloadStatus.Downloaded -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = JellyfinColors.SuccessEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is MovieDownloadStatus.Idle -> {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            tint = JellyfinColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            label = when (state.downloadStatus) {
                is MovieDownloadStatus.Downloading -> "جاري التنزيل"
                is MovieDownloadStatus.Downloaded -> "محمّل"
                is MovieDownloadStatus.Idle -> "تنزيل"
            },
            onClick = onDownloadClick,
            modifier = Modifier.weight(1f)
        )

        // Trailer action button
        QuickActionItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Movie,
                    contentDescription = null,
                    tint = JellyfinColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = "تريلر",
            onClick = onTrailerClick,
            modifier = Modifier.weight(1f)
        )

        // Favorite action button
        QuickActionItem(
            icon = {
                Icon(
                    imageVector = if (state.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (state.isFavorite) JellyfinColors.RatingGold else JellyfinColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = if (state.isFavorite) "مفضل" else "المفضلة",
            onClick = onFavoriteClick,
            modifier = Modifier.weight(1f)
        )

        // Watched action button
        QuickActionItem(
            icon = {
                Icon(
                    imageVector = if (state.isWatched) Icons.Rounded.Check else Icons.Outlined.Check,
                    contentDescription = null,
                    tint = if (state.isWatched) JellyfinColors.SuccessEmerald else JellyfinColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = if (state.isWatched) "شوهد" else "المشاهدة",
            onClick = onWatchedClick,
            modifier = Modifier.weight(1f)
        )

        // Options (Audio & Subtitles) action button
        QuickActionItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Subtitles,
                    contentDescription = null,
                    tint = JellyfinColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = "الخيارات",
            onClick = onOptionsClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        modifier = modifier.defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = JellyfinColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MovieChaptersRow(
    chapters: ImmutableList<MovieChapter>,
    onChapterClick: (MovieChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "فصول الفيلم المصورة",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = JellyfinColors.TextPrimary,
            modifier = Modifier.padding(horizontal = JellyfinDimens.SpaceLarge)
        )

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

        LazyRow(
            contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = chapters,
                key = { it.id }
            ) { chapter ->
                ChapterCard(
                    chapter = chapter,
                    onClick = { onChapterClick(chapter) }
                )
            }
        }
    }
}

@Composable
private fun ChapterCard(
    chapter: MovieChapter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(160.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(JellyfinDimens.RadiusDefault))
            .background(JellyfinPrimitives.Graphite800)
            .border(
                width = 1.dp,
                color = JellyfinColors.BorderGlass,
                shape = RoundedCornerShape(JellyfinDimens.RadiusDefault)
            )
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "${chapter.name}، التوقيت ${chapter.formattedTime}"
            }
    ) {
        if (!chapter.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = chapter.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Vignette gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Center Play Icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(28.dp)
            )
        }

        // Bottom text: Name & Timestamp
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = chapter.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = chapter.formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = JellyfinColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun SimilarMoviesRow(
    movies: ImmutableList<MediaItemSummary>,
    onMovieClick: (MediaItemSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "أفلام مقترحة",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = JellyfinColors.TextPrimary,
            modifier = Modifier.padding(horizontal = JellyfinDimens.SpaceLarge)
        )

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

        LazyRow(
            contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = movies,
                key = { it.id }
            ) { item ->
                SimilarMovieCard(
                    movie = item,
                    onClick = { onMovieClick(item) }
                )
            }
        }
    }
}

@Composable
private fun SimilarMovieCard(
    movie: MediaItemSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(110.dp)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "فيلم ${movie.title}"
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(JellyfinDimens.RadiusDefault))
                .background(JellyfinPrimitives.Graphite800)
                .border(
                    width = 1.dp,
                    color = JellyfinColors.BorderGlass,
                    shape = RoundedCornerShape(JellyfinDimens.RadiusDefault)
                )
        ) {
            if (!movie.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = movie.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = JellyfinColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MovieTopBar(
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = JellyfinDimens.SpaceLarge, vertical = JellyfinDimens.SpaceSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(JellyfinDimens.MinTouchTarget)
                .glassmorphism(
                    backgroundColor = JellyfinPrimitives.Graphite900.copy(alpha = 0.75f),
                    borderColor = JellyfinColors.BorderGlass,
                    cornerRadius = JellyfinDimens.RadiusPill
                )
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

        // Actions: Share & Favorite
        Row(horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)) {
            IconButton(
                onClick = onShareClick,
                modifier = Modifier
                    .size(JellyfinDimens.MinTouchTarget)
                    .glassmorphism(
                        backgroundColor = JellyfinPrimitives.Graphite900.copy(alpha = 0.75f),
                        borderColor = JellyfinColors.BorderGlass,
                        cornerRadius = JellyfinDimens.RadiusPill
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "مشاركة الفيلم"
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    tint = JellyfinColors.TextPrimary
                )
            }

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .size(JellyfinDimens.MinTouchTarget)
                    .glassmorphism(
                        backgroundColor = JellyfinPrimitives.Graphite900.copy(alpha = 0.75f),
                        borderColor = JellyfinColors.BorderGlass,
                        cornerRadius = JellyfinDimens.RadiusPill
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة"
                    }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) JellyfinColors.RatingGold else JellyfinColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun AudioSubtitleBottomSheetContent(
    audioTracks: ImmutableList<MovieAudioTrack>,
    subtitleTracks: ImmutableList<MovieSubtitleTrack>,
    selectedAudioTrack: MovieAudioTrack?,
    selectedSubtitleTrack: MovieSubtitleTrack?,
    onSelectAudio: (MovieAudioTrack) -> Unit,
    onSelectSubtitle: (MovieSubtitleTrack?) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = JellyfinDimens.SpaceLarge)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "خيارات الصوت والترجمة",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = JellyfinColors.TextPrimary,
            modifier = Modifier.padding(vertical = JellyfinDimens.SpaceSmall)
        )

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

        // Audio Tracks Section
        Text(
            text = "مسار الصوت",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = JellyfinPrimitives.Ivory50
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (audioTracks.isEmpty()) {
            Text(
                text = "لا تتوفر مسارات صوتية إضافية",
                style = MaterialTheme.typography.bodySmall,
                color = JellyfinColors.TextMuted
            )
        } else {
            audioTracks.forEach { track ->
                val isSelected = track.index == selectedAudioTrack?.index
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                        .clickable(role = Role.RadioButton) { onSelectAudio(track) }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) JellyfinPrimitives.Ivory50 else JellyfinColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) JellyfinColors.TextPrimary else JellyfinColors.TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

        // Subtitles Section
        Text(
            text = "ملف الترجمة",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = JellyfinPrimitives.Ivory50
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Option: No Subtitles
        val isNoneSelected = selectedSubtitleTrack == null
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                .clickable(role = Role.RadioButton) { onSelectSubtitle(null) }
                .padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (isNoneSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isNoneSelected) JellyfinPrimitives.Ivory50 else JellyfinColors.TextSecondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "إيقاف الترجمة",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isNoneSelected) JellyfinColors.TextPrimary else JellyfinColors.TextSecondary,
                fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Normal
            )
        }

        subtitleTracks.forEach { track ->
            val isSelected = track.index == selectedSubtitleTrack?.index
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                    .clickable(role = Role.RadioButton) { onSelectSubtitle(track) }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) JellyfinPrimitives.Ivory50 else JellyfinColors.TextSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) JellyfinColors.TextPrimary else JellyfinColors.TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(
                containerColor = JellyfinPrimitives.Graphite800,
                contentColor = JellyfinColors.TextPrimary
            ),
            shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
        ) {
            Text(
                text = "إغلاق",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MovieErrorView(
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
                text = "تعذر تحميل تفاصيل الفيلم",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = JellyfinColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JellyfinPrimitives.Ivory50,
                    contentColor = JellyfinPrimitives.Obsidian950
                ),
                shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                modifier = Modifier.defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            ) {
                Text(text = "إعادة المحاولة", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
