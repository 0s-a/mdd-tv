package org.jellyfin.mobile.presentation.details.series

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jellyfin.mobile.core.player.external.ExternalPlayerIntentBuilder
import org.jellyfin.mobile.core.player.external.ExternalPlayerPayload
import org.jellyfin.mobile.core.player.external.ExternalPlayerResultParser
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.core.util.BidiFormatter
import org.jellyfin.mobile.domain.model.EpisodeSummary
import org.jellyfin.mobile.domain.model.MediaDetails
import org.jellyfin.mobile.domain.model.SeasonSummary
import org.jellyfin.mobile.presentation.core.components.AmbientGlow
import org.jellyfin.mobile.presentation.core.components.rememberDominantColor
import org.jellyfin.mobile.presentation.details.components.AdaptiveBackdropHeader
import org.jellyfin.mobile.presentation.details.components.CastCarousel
import org.jellyfin.mobile.presentation.details.components.GenreChipsRow
import org.jellyfin.mobile.presentation.details.components.InfuseTechBadgesRow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import org.jellyfin.mobile.presentation.details.components.SaveModalBottomSheet
import org.jellyfin.mobile.presentation.details.components.TopActionsDropdownMenu
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * SeriesDetailsScreen provides the dedicated, pure TV Series details experience.
 * Incorporates a 220dp AdaptiveBackdropHeader, NextUpCard hero button,
 * horizontal and dropdown season selectors, vertical episodes list, and cast carousel.
 * Strictly adheres to 03_series_details_screen.md and Obsidian 2.2.0 tokens.
 */
@Composable
fun SeriesDetailsScreen(
    seriesId: String,
    viewModel: SeriesViewModel,
    onNavigateBack: () -> Unit,
    onLaunchPlayer: (String, Long) -> Unit,
    onNavigateToGenre: (String) -> Unit = {},
    onShare: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background
    val context = LocalContext.current

    var isOverviewExpanded by remember { mutableStateOf(false) }

    // External Player Activity Result Launcher
    var activeExternalPayload by remember { mutableStateOf<ExternalPlayerPayload?>(null) }
    var externalLaunchTimestamp by remember { mutableLongStateOf(0L) }

    val externalPlayerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val payload = activeExternalPayload
        val elapsedMs = SystemClock.elapsedRealtime() - externalLaunchTimestamp
        if (payload != null) {
            val playbackResult = ExternalPlayerResultParser.parse(
                payload = payload,
                resultCode = result.resultCode,
                data = result.data,
                elapsedMs = elapsedMs
            )
            viewModel.onExternalPlaybackResult(playbackResult, payload.title)
        }
        activeExternalPayload = null
    }

    LaunchedEffect(seriesId) {
        viewModel.processIntent(SeriesIntent.LoadSeries(seriesId))
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is SeriesEffect.LaunchPlayer -> {
                    onLaunchPlayer(effect.mediaId, effect.resumePositionMs)
                }
                is SeriesEffect.ShareSeriesContent -> {
                    onShare?.invoke(effect.title, effect.summary)
                }
                is SeriesEffect.NavigateToActorFilmography -> {
                    // Navigate to actor filmography when screen route is available
                }
                is SeriesEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SeriesEffect.ShowSnackbar -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is SeriesEffect.LaunchExternalPlayer -> {
                    activeExternalPayload = effect.payload
                    externalLaunchTimestamp = SystemClock.elapsedRealtime()
                    val chooserIntent = ExternalPlayerIntentBuilder.buildChooserIntent(effect.payload)
                    externalPlayerLauncher.launch(chooserIntent)
                }
            }
        }
    }

    // Refresh playback and watch state upon returning from player
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.processIntent(SeriesIntent.RefreshPlaybackState)
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
            is SeriesUiState.Loading -> {
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

            is SeriesUiState.Error -> {
                SeriesErrorView(
                    message = state.message,
                    onRetry = { viewModel.processIntent(SeriesIntent.LoadSeries(seriesId)) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is SeriesUiState.Success -> {
                val series = state.seriesDetails
                val ambientColor by rememberDominantColor(
                    imageUrl = series.backdropUrl ?: series.posterUrl
                )

                // 1. Dynamic Top Ambient Glow
                AmbientGlow(
                    color = ambientColor,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // 2. Main Scrollable Content
                val configuration = LocalConfiguration.current
                val heroHeight = (configuration.screenHeightDp * 0.50f).dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 80.dp)
                ) {
                    // 50% Screen Height Golden Ratio Adaptive Backdrop Header with Dual Scrims
                    AdaptiveBackdropHeader(
                        backdropUrl = series.backdropUrl,
                        posterUrl = series.posterUrl,
                        title = series.title,
                        maxHeight = heroHeight,
                        backgroundColor = backgroundColor
                    )

                    // Series Details Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = JellyfinDimens.SpaceLarge)
                    ) {
                        // Title with BiDi protection
                        Text(
                            text = BidiFormatter.wrapLtr(series.title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = JellyfinColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Series Metadata Row: Rating, Year range, Seasons count, Content rating
                        SeriesMetadataRow(series = series, seasonsCount = state.seasons.size)

                        // Genre Chips
                        if (series.genres.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
                            GenreChipsRow(
                                genres = series.genres,
                                onGenreClick = onNavigateToGenre
                            )
                        }

                        // Subtle Technical Outlined Badges
                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceExtraSmall))
                        InfuseTechBadgesRow(mediaItem = series)

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

                        // Primary Action Button (Next Up Hero Button - 54dp)
                        SeriesPrimaryActionButton(
                            nextUpEpisode = state.nextUpEpisode,
                            allWatched = state.allEpisodesWatched,
                            onClick = { viewModel.processIntent(SeriesIntent.ResumeNextUp) }
                        )

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                        // Secondary Action Duo: Save in lists + External player (48dp, 50%/50%)
                        SeriesSecondaryActionDuo(
                            isSaved = state.isFavorite || state.isWatchlist,
                            onSaveClick = { viewModel.processIntent(SeriesIntent.OpenSaveModal) },
                            onExternalPlayerClick = { viewModel.processIntent(SeriesIntent.PlayNextUpExternal) }
                        )

                        // Expandable Synopsis Section
                        if (series.overview.isNotBlank()) {
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                            SeriesOverviewSection(
                                overview = series.overview,
                                isExpanded = isOverviewExpanded,
                                onToggleExpand = { isOverviewExpanded = !isOverviewExpanded }
                            )
                        }
                    }

                    // Season Selector: Horizontal Tabs and Dropdown Picker
                    if (state.seasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                        SeasonSelectorSection(
                            seasons = state.seasons,
                            selectedSeasonId = state.selectedSeasonId,
                            selectedSeasonNumber = state.selectedSeasonNumber,
                            onSeasonSelect = { seasonId ->
                                viewModel.processIntent(SeriesIntent.SelectSeasonById(seasonId))
                            }
                        )
                    }

                    // Episodes List for Selected Season
                    if (state.isLoadingEpisodes) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            CircularProgressIndicator(
                                color = JellyfinColors.PrimaryIndigo,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    } else if (state.episodesForSelectedSeason.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
                        SeriesEpisodesVerticalList(
                            episodes = state.episodesForSelectedSeason,
                            onEpisodeClick = { episodeId ->
                                viewModel.processIntent(SeriesIntent.PlayEpisode(episodeId))
                            },
                            onToggleWatched = { episodeId ->
                                viewModel.processIntent(SeriesIntent.ToggleEpisodeWatched(episodeId))
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
                                    SeriesIntent.OpenActorFilmography(
                                        personId = member.id,
                                        personName = member.name
                                    )
                                )
                            }
                        )
                    }
                }

                // 3. Floating Transparent Top App Bar (Back, More Actions Menu)
                SeriesTopBar(
                    onBackClick = onNavigateBack,
                    onPlaybackOptionsClick = {
                        Toast.makeText(context, "خيارات التشغيل الافتراضية نشطة", Toast.LENGTH_SHORT).show()
                    },
                    onShareClick = { viewModel.processIntent(SeriesIntent.ShareSeries) },
                    isWatched = state.isWatched,
                    onToggleWatchedClick = { viewModel.processIntent(SeriesIntent.ToggleSeriesWatched) },
                    hasTrailer = false,
                    onTrailerClick = {},
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                )

                // 4. Save Modal Bottom Sheet (Favorites + Watchlist)
                SaveModalBottomSheet(
                    isVisible = state.isSaveModalVisible,
                    isFavorite = state.isFavorite,
                    isWatchlist = state.isWatchlist,
                    onToggleFavorite = { viewModel.processIntent(SeriesIntent.ToggleSeriesFavorite) },
                    onToggleWatchlist = { viewModel.processIntent(SeriesIntent.ToggleSeriesWatchlist) },
                    onDismiss = { viewModel.processIntent(SeriesIntent.DismissSaveModal) }
                )
            }
        }
    }
}

/**
 * Metadata row presenting rating, year range, seasons count, and content rating.
 */
@Composable
private fun SeriesMetadataRow(
    series: MediaDetails,
    seasonsCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
        modifier = modifier
    ) {
        if (series.communityRating != null && series.communityRating > 0f) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = JellyfinColors.RatingGold,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = String.format(java.util.Locale.ROOT, "%.1f IMDb", series.communityRating),
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.RatingGold,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(text = "•", color = JellyfinColors.TextMuted)
        }

        if (series.productionYear != null) {
            Text(
                text = series.productionYear.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = JellyfinColors.TextSecondary
            )
            Text(text = "•", color = JellyfinColors.TextMuted)
        }

        if (seasonsCount > 0) {
            val totalEpisodes = series.episodes.size
            val countLabel = if (totalEpisodes > 0) {
                "$seasonsCount مواسم ($totalEpisodes حلقة)"
            } else {
                "$seasonsCount مواسم"
            }
            Text(
                text = countLabel,
                style = MaterialTheme.typography.bodySmall,
                color = JellyfinColors.TextSecondary
            )
        }

        if (!series.officialRating.isNullOrBlank()) {
            Text(text = "•", color = JellyfinColors.TextMuted)
            Text(
                text = series.officialRating,
                style = MaterialTheme.typography.bodySmall,
                color = JellyfinColors.TextSecondary
            )
        }
    }
}

/**
 * NextUpHeroCard renders the prominent Thumb Zone action card.
 * Allows one-touch immediate playback of the next pending episode.
 */
@Composable
private fun SeriesNextUpHeroCard(
    nextUpEpisode: EpisodeSummary?,
    allWatched: Boolean,
    onResumeClick: () -> Unit,
    onPlayEpisode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (nextUpEpisode == null) return

    val isResume = nextUpEpisode.playbackPositionPercentage > 0f
    val titlePrefix = when {
        allWatched -> "إعادة المشاهدة من البداية"
        isResume -> "استئناف المشاهدة"
        else -> "الحلقة التالية"
    }

    val subtitle = "الموسم ${nextUpEpisode.seasonNumber} • الحلقة ${nextUpEpisode.episodeNumber}: ${nextUpEpisode.name}"
    val remainingMinutes = nextUpEpisode.durationText

    val talkBackDesc = "$titlePrefix: $subtitle. انقر للتشغيل."

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceDark,
                borderColor = JellyfinColors.BorderGlass,
                cornerRadius = JellyfinDimens.RadiusMedium
            )
            .clickable(
                role = Role.Button,
                onClick = {
                    if (isResume) onResumeClick() else onPlayEpisode(nextUpEpisode.id)
                }
            )
            .padding(JellyfinDimens.SpaceMedium)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = talkBackDesc
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 16:9 Thumbnail preview with play overlay
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                    .background(JellyfinColors.Background)
            ) {
                if (!nextUpEpisode.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = nextUpEpisode.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tv,
                            contentDescription = null,
                            tint = JellyfinColors.TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Vignette scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(JellyfinColors.PureBlack.copy(alpha = 0.35f))
                )

                // Play icon button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(JellyfinColors.PrimaryIndigo.copy(alpha = 0.9f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = JellyfinColors.PureBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Progress Bar
                if (nextUpEpisode.playbackPositionPercentage > 0f) {
                    LinearProgressIndicator(
                        progress = { (nextUpEpisode.playbackPositionPercentage / 100f).coerceIn(0f, 1f) },
                        color = JellyfinColors.RatingGold,
                        trackColor = JellyfinColors.SurfaceDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            // Episode info details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = titlePrefix,
                    style = MaterialTheme.typography.labelSmall,
                    color = JellyfinColors.TechSky,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = BidiFormatter.wrapLtr(nextUpEpisode.name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "م ${nextUpEpisode.seasonNumber} • ح ${nextUpEpisode.episodeNumber}" +
                        if (!remainingMinutes.isNullOrBlank()) " • $remainingMinutes" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (isResume) onResumeClick() else onPlayEpisode(nextUpEpisode.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JellyfinColors.PrimaryIndigo,
                        contentColor = JellyfinColors.PureBlack
                    ),
                    shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isResume) "استئناف" else "تشغيل",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Next Up Hero Primary Action Button (54dp) in Ivory50.
 */
@Composable
private fun SeriesPrimaryActionButton(
    nextUpEpisode: EpisodeSummary?,
    allWatched: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isResume = nextUpEpisode != null && nextUpEpisode.playbackPositionPercentage > 0f
    val buttonText = when {
        allWatched -> "إعادة من الحلقة الأولى"
        nextUpEpisode == null -> "بدء الحلقة الأولى"
        isResume -> "استئناف: ح ${nextUpEpisode.episodeNumber} · م ${nextUpEpisode.seasonNumber}"
        else -> "تشغيل: ح ${nextUpEpisode.episodeNumber} · م ${nextUpEpisode.seasonNumber}"
    }

    val progress = (nextUpEpisode?.playbackPositionPercentage ?: 0f) / 100f

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = JellyfinPrimitives.Ivory50,
            contentColor = JellyfinPrimitives.Obsidian950
        ),
        shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
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
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = JellyfinPrimitives.CrimsonMuted,
                    trackColor = JellyfinPrimitives.Slate700,
                    modifier = Modifier
                        .fillMaxWidth(0.50f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

/**
 * Secondary Action Duo (48dp): Save in lists (50%) + External player (50%).
 */
@Composable
private fun SeriesSecondaryActionDuo(
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    onExternalPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
    ) {
        // 1. Save in Lists button (50%)
        OutlinedButton(
            onClick = onSaveClick,
            shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = JellyfinPrimitives.Graphite800.copy(alpha = 0.6f)
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = Brush.horizontalGradient(
                    listOf(JellyfinColors.BorderGlass, JellyfinColors.BorderGlass)
                )
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkAdd,
                contentDescription = null,
                tint = if (isSaved) JellyfinColors.RatingGold else JellyfinColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "حفظ في القوائم",
                style = MaterialTheme.typography.labelMedium,
                color = if (isSaved) JellyfinColors.RatingGold else JellyfinColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 2. External Player button (50%)
        OutlinedButton(
            onClick = onExternalPlayerClick,
            shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = JellyfinPrimitives.Graphite800.copy(alpha = 0.6f)
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = Brush.horizontalGradient(
                    listOf(JellyfinColors.BorderGlass, JellyfinColors.BorderGlass)
                )
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = JellyfinColors.AccentAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "مشغل خارجي",
                style = MaterialTheme.typography.labelMedium,
                color = JellyfinColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Overview section with expand/collapse capability.
 */
@Composable
private fun SeriesOverviewSection(
    overview: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
                .clickable { onToggleExpand() }
        ) {
            Text(
                text = overview,
                style = MaterialTheme.typography.bodyMedium,
                color = JellyfinColors.TextSecondary,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isExpanded) "إخفاء التفاصيل" else "قراءة المزيد",
                style = MaterialTheme.typography.labelSmall,
                color = JellyfinColors.TechSky,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Season selector featuring both horizontal capsule tabs and a quick dropdown selector.
 */
@Composable
private fun SeasonSelectorSection(
    seasons: ImmutableList<SeasonSummary>,
    selectedSeasonId: String?,
    selectedSeasonNumber: Int,
    onSeasonSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDropdownOpen by remember { mutableStateOf(false) }
    val currentSeason = seasons.find { it.id == selectedSeasonId }
        ?: seasons.find { it.indexNumber == selectedSeasonNumber }
        ?: seasons.firstOrNull()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = JellyfinDimens.SpaceLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "المواسم",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )

            // Dropdown menu anchor button
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                        .background(JellyfinColors.SurfaceDark)
                        .clickable { isDropdownOpen = true }
                        .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = 6.dp)
                ) {
                    Text(
                        text = currentSeason?.name ?: "اختر الموسم",
                        style = MaterialTheme.typography.labelMedium,
                        color = JellyfinColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = "قائمة اختيار المواسم",
                        tint = JellyfinColors.TextSecondary
                    )
                }

                DropdownMenu(
                    expanded = isDropdownOpen,
                    onDismissRequest = { isDropdownOpen = false },
                    modifier = Modifier.background(JellyfinColors.SurfaceElevated)
                ) {
                    seasons.forEach { season ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${season.name} (${season.episodeCount} حلقة)",
                                    color = if (season.id == currentSeason?.id) JellyfinColors.PrimaryIndigo else JellyfinColors.TextPrimary,
                                    fontWeight = if (season.id == currentSeason?.id) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                isDropdownOpen = false
                                onSeasonSelect(season.id)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

        // Horizontal Season Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = JellyfinDimens.SpaceLarge),
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = seasons,
                key = { it.id.ifBlank { it.seasonNumber.toString() } }
            ) { season ->
                val isSelected = season.id == currentSeason?.id
                val tabBorder = if (isSelected) JellyfinColors.PrimaryIndigo else JellyfinColors.BorderGlass
                val tabBg = if (isSelected) JellyfinColors.SurfaceDark else JellyfinColors.SurfaceDark.copy(alpha = 0.5f)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                        .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                        .background(tabBg)
                        .border(
                            width = if (isSelected) 1.5.dp else JellyfinDimens.BorderThin,
                            color = tabBorder,
                            shape = RoundedCornerShape(JellyfinDimens.RadiusMedium)
                        )
                        .clickable(role = Role.Tab) {
                            onSeasonSelect(season.id)
                        }
                        .padding(horizontal = JellyfinDimens.SpaceLarge, vertical = JellyfinDimens.SpaceSmall)
                        .semantics {
                            role = Role.Tab
                            contentDescription = "${season.name}، ${season.episodeCount} حلقات"
                        }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = season.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) JellyfinColors.TextPrimary else JellyfinColors.TextSecondary
                        )
                        Text(
                            text = "${season.episodeCount} حلقات",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) JellyfinColors.TechSky else JellyfinColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vertical list displaying episodes for the selected season.
 */
@Composable
private fun SeriesEpisodesVerticalList(
    episodes: ImmutableList<EpisodeSummary>,
    onEpisodeClick: (String) -> Unit,
    onToggleWatched: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = JellyfinDimens.SpaceLarge)
    ) {
        episodes.forEachIndexed { index, episode ->
            SeriesEpisodeRowItem(
                episode = episode,
                onItemClick = { onEpisodeClick(episode.id) },
                onToggleWatched = { onToggleWatched(episode.id) }
            )

            if (index < episodes.lastIndex) {
                HorizontalDivider(
                    color = JellyfinColors.SurfaceDark,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = JellyfinDimens.SpaceSmall)
                )
            }
        }
    }
}

/**
 * Individual episode card row with 16:9 thumbnail, progress bar, duration, and watched toggle.
 */
@Composable
private fun SeriesEpisodeRowItem(
    episode: EpisodeSummary,
    onItemClick: () -> Unit,
    onToggleWatched: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val talkBackDesc = "الحلقة ${episode.episodeNumber}: ${episode.name}، ${episode.durationText ?: ""}، ${if (episode.isWatched) "تمت المشاهدة" else "غير مشاهدة"}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
            .clickable(
                role = Role.Button,
                onClick = onItemClick
            )
            .padding(vertical = JellyfinDimens.SpaceExtraSmall)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = talkBackDesc
            },
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceMedium),
        verticalAlignment = Alignment.Top
    ) {
        // 16:9 Thumbnail container with fallback carbon card
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                .background(JellyfinColors.SurfaceDark)
        ) {
            if (!episode.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = episode.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Carbon fallback card showing episode number clearly
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(JellyfinColors.SurfaceElevated)
                ) {
                    Text(
                        text = "ح ${episode.episodeNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = JellyfinColors.TextSecondary
                    )
                }
            }

            // Dark vignette scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(JellyfinColors.PureBlack.copy(alpha = 0.25f))
            )

            // Play or Watched badge indicator
            if (episode.isWatched) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(JellyfinColors.SuccessEmerald)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = JellyfinColors.PureBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(JellyfinColors.PrimaryIndigo.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = JellyfinColors.PureBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Progress bar
            if (episode.playbackPositionPercentage > 0f && !episode.isWatched) {
                LinearProgressIndicator(
                    progress = { (episode.playbackPositionPercentage / 100f).coerceIn(0f, 1f) },
                    color = JellyfinColors.PrimaryIndigo,
                    trackColor = JellyfinColors.SurfaceDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        // Details column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${BidiFormatter.wrapLtr("E" + episode.episodeNumber.toString().padStart(2, '0'))} • ${BidiFormatter.wrapLtr(episode.name)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val duration = episode.durationText
            if (!duration.isNullOrBlank()) {
                Text(
                    text = duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = JellyfinColors.TechSky
                )
            }

            val overview = episode.overview.orEmpty()
            if (overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.TextSecondary,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }
        }

        // Watched toggle button with min 48dp touch target
        IconButton(
            onClick = onToggleWatched,
            modifier = Modifier
                .size(JellyfinDimens.MinTouchTarget)
                .semantics {
                    role = Role.Checkbox
                    contentDescription = if (episode.isWatched) "تحديد كغير مشاهد" else "تحديد كتمت المشاهدة"
                }
        ) {
            Icon(
                imageVector = if (episode.isWatched) Icons.Rounded.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (episode.isWatched) JellyfinColors.SuccessEmerald else JellyfinColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Minimalist Top App Bar with back button and overflow menu.
 */
@Composable
private fun SeriesTopBar(
    onBackClick: () -> Unit,
    onPlaybackOptionsClick: () -> Unit,
    onShareClick: () -> Unit,
    isWatched: Boolean,
    onToggleWatchedClick: () -> Unit,
    hasTrailer: Boolean,
    onTrailerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = JellyfinDimens.SpaceLarge, vertical = JellyfinDimens.SpaceSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        TopActionsDropdownMenu(
            onPlaybackOptionsClick = onPlaybackOptionsClick,
            onShareClick = onShareClick,
            isWatched = isWatched,
            onToggleWatchedClick = onToggleWatchedClick,
            hasTrailer = hasTrailer,
            onTrailerClick = onTrailerClick
        )
    }
}

/**
 * Error state display with retry button.
 */
@Composable
private fun SeriesErrorView(
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
                text = "تعذر تحميل تفاصيل المسلسل",
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
                    contentColor = JellyfinColors.PureBlack
                ),
                shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                modifier = Modifier.defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            ) {
                Text(text = "إعادة المحاولة", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
