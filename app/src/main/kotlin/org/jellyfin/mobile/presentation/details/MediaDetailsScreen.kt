package org.jellyfin.mobile.presentation.details

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.domain.model.ContentType
import org.jellyfin.mobile.presentation.core.components.AmbientGlow
import org.jellyfin.mobile.presentation.core.components.rememberDominantColor
import org.jellyfin.mobile.presentation.details.components.CastCarousel
import org.jellyfin.mobile.presentation.details.components.EpisodesList
import org.jellyfin.mobile.presentation.details.components.InfuseTechBadgesRow
import org.jellyfin.mobile.presentation.details.components.NextUpCard
import org.jellyfin.mobile.presentation.details.components.SeasonTabs
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * MediaDetailsScreen is the dual-nature cinematic media detail experience.
 * Automatically adapts between Movie and Series layouts.
 * Strictly adheres to docs/screens/03_MEDIA_DETAILS.md and docs/DESIGN_SYSTEM.md.
 */
@Composable
fun MediaDetailsScreen(
    mediaId: String,
    viewModel: MediaDetailsViewModel,
    onNavigateBack: () -> Unit,
    onLaunchPlayer: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background

    var isSynopsisExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(mediaId) {
        viewModel.processIntent(MediaDetailsIntent.LoadMedia(mediaId))
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is MediaDetailsEffect.LaunchPlayer -> onLaunchPlayer(effect.mediaId, effect.resumePositionMs)
                is MediaDetailsEffect.NavigateToDetails -> { /* Sub-navigation */ }
                is MediaDetailsEffect.ShowToast -> { /* Notification */ }
                is MediaDetailsEffect.ShowSnackbar -> { /* Notification */ }
                is MediaDetailsEffect.NavigateToActorFilmography -> { /* Filmography */ }
                is MediaDetailsEffect.OpenTrailerPlayer -> { /* Trailer */ }
                MediaDetailsEffect.ShareMedia -> { /* Share */ }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        when (val state = uiState) {
            is MediaDetailsUiState.Loading -> {
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

            is MediaDetailsUiState.Error -> {
                DetailsErrorState(
                    message = state.message,
                    onRetry = { viewModel.processIntent(MediaDetailsIntent.LoadMedia(mediaId)) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is MediaDetailsUiState.Success -> {
                val media = state.mediaItem
                val ambientColor by rememberDominantColor(
                    imageUrl = media.backdropUrl ?: media.posterUrl
                )

                // 1. Dynamic Ambient Glow at top
                AmbientGlow(
                    color = ambientColor,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // 2. Main Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 80.dp)
                ) {
                    // Parallax Backdrop Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) {
                        AsyncImage(
                            model = media.backdropUrl ?: media.posterUrl,
                            contentDescription = "خلفية ${media.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // OLED Scrim Gradient (fades to pure black at bottom)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to JellyfinColors.PureBlack.copy(alpha = 0.5f),
                                        0.4f to Color.Transparent,
                                        0.75f to backgroundColor.copy(alpha = 0.85f),
                                        1.0f to backgroundColor
                                    )
                                )
                        )
                    }

                    // Content details body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = JellyfinDimens.SpaceLarge)
                    ) {
                        // Title
                        Text(
                            text = media.title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = JellyfinColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Metadata Badges (IMDb Rating + Year + Runtime + Rating)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
                        ) {
                            if (media.communityRating != null && media.communityRating > 0f) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = JellyfinColors.RatingGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f IMDb", media.communityRating),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = JellyfinColors.RatingGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(text = "•", color = JellyfinColors.TextSecondary)
                            }

                            if (media.productionYear != null) {
                                Text(
                                    text = media.productionYear.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = JellyfinColors.TextSecondary
                                )
                                Text(text = "•", color = JellyfinColors.TextSecondary)
                            }

                            val runtimeFormatted = media.runtimeFormatted
                            if (!runtimeFormatted.isNullOrBlank()) {
                                Text(
                                    text = runtimeFormatted,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = JellyfinColors.TextSecondary
                                )
                            }

                            if (!media.officialRating.isNullOrBlank()) {
                                Text(
                                    text = media.officialRating,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JellyfinColors.TextMuted,
                                    modifier = Modifier
                                        .border(1.dp, JellyfinColors.BorderGlass, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Genres
                        if (media.genres.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = media.genres.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = JellyfinColors.TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

                        // Infuse Tech Badges Row & Inspector
                        InfuseTechBadgesRow(mediaItem = media)

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

                        // Grand Action Play / Resume Button
                        GrandActionButton(
                            media = media,
                            onResumeClick = { viewModel.processIntent(MediaDetailsIntent.ResumeMedia) },
                            onPlayClick = { viewModel.processIntent(MediaDetailsIntent.PlayMedia) }
                        )

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                        // Secondary Action Buttons Row (Replay from start / Watched toggle)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.processIntent(MediaDetailsIntent.PlayFromStart) },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Replay,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "من البداية", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = { viewModel.processIntent(MediaDetailsIntent.ToggleWatched) },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
                            ) {
                                Icon(
                                    imageVector = if (state.isWatched) Icons.Rounded.Check else Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = if (state.isWatched) JellyfinColors.SuccessEmerald else JellyfinColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.isWatched) "تمت المشاهدة" else "تحديد كمشاهد",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (state.isWatched) JellyfinColors.SuccessEmerald else JellyfinColors.TextSecondary
                                )
                            }
                        }

                        // Expandable Synopsis Section
                        if (media.overview.isNotBlank()) {
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
                                    text = media.overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = JellyfinColors.TextSecondary,
                                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isSynopsisExpanded) "إخفاء ▲" else "اقرأ المزيد ▼",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JellyfinColors.TechSky,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Series Specific Sections: NextUp Hero Card & Seasons
                    if (media.contentType == ContentType.Series) {
                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

                        // Next Up Episode Hero Card
                        val nextEp = state.nextUpEpisode
                        if (nextEp != null) {
                            NextUpCard(
                                episode = nextEp,
                                onResumeClick = { epId ->
                                    viewModel.processIntent(MediaDetailsIntent.PlayEpisode(epId))
                                }
                            )
                        }

                        // Season Selector Tabs
                        if (state.seasons.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                            SeasonTabs(
                                seasons = state.seasons,
                                selectedSeasonNumber = state.selectedSeasonNumber,
                                onSeasonSelect = { seasonNum ->
                                    viewModel.processIntent(MediaDetailsIntent.SelectSeason(seasonNum))
                                }
                            )
                        }

                        // Episodes List for Selected Season
                        if (state.episodesForSeason.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
                            EpisodesList(
                                episodes = state.episodesForSeason,
                                onEpisodeClick = { epId ->
                                    viewModel.processIntent(MediaDetailsIntent.PlayEpisode(epId))
                                },
                                onToggleWatched = { epId ->
                                    viewModel.processIntent(MediaDetailsIntent.ToggleEpisodeWatched(epId))
                                },
                                onDownloadClick = { epId ->
                                    viewModel.processIntent(MediaDetailsIntent.DownloadEpisode(epId))
                                }
                            )
                        }
                    }

                    // Cast Carousel
                    if (state.castList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                        CastCarousel(
                            castList = state.castList,
                            onCastClick = { /* Actor filmography deep link */ }
                        )
                    }
                }

                // 3. Floating Transparent Top App Bar (Back & Favorite Buttons)
                DetailsTopBar(
                    isFavorite = state.isFavorite,
                    onBackClick = onNavigateBack,
                    onFavoriteClick = { viewModel.processIntent(MediaDetailsIntent.ToggleFavorite) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                )
            }
        }
    }
}

@Composable
private fun GrandActionButton(
    media: org.jellyfin.mobile.domain.model.MediaDetailDomainModel,
    onResumeClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val isResume = media.resumePositionMs > 0L
    val progress = if (media.totalDurationMs > 0L) {
        media.resumePositionMs.toFloat() / media.totalDurationMs.toFloat()
    } else 0f

    Button(
        onClick = if (isResume) onResumeClick else onPlayClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = JellyfinColors.PrimaryIndigo,
            contentColor = JellyfinColors.TextPrimary
        ),
        shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .shadow(
                elevation = JellyfinDimens.ElevationHigh,
                shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                ambientColor = JellyfinColors.IndigoGlow,
                spotColor = JellyfinColors.PrimaryIndigo
            )
            .semantics {
                role = Role.Button
                contentDescription = if (isResume) "استئناف مشاهدة ${media.title}" else "تشغيل ${media.title}"
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
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isResume) "استئناف المشاهدة" else "تشغيل العمل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isResume && progress > 0f) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = JellyfinColors.TechSky,
                    trackColor = JellyfinColors.PrimaryIndigo.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun DetailsTopBar(
    isFavorite: Boolean,
    onBackClick: () -> Unit,
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
                    backgroundColor = JellyfinColors.SurfaceDark.copy(alpha = 0.7f),
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

        // Favorite Button
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier
                .size(JellyfinDimens.MinTouchTarget)
                .glassmorphism(
                    backgroundColor = JellyfinColors.SurfaceDark.copy(alpha = 0.7f),
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

@Composable
private fun DetailsErrorState(
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
                text = "تعذر تحميل تفاصيل العمل",
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
