package org.jellyfin.mobile.presentation.details.boxset

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.presentation.core.components.CinemaTopBar
import org.jellyfin.mobile.presentation.details.components.AdaptiveBackdropHeader
import org.jellyfin.mobile.ui.components.ObsidianCard
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives

/**
 * Mobile BoxSet & Movie Franchises Details Screen.
 * Strictly adheres to docs/current/screens/media_details/04_boxset_details_screen.md.
 * Zero emojis, 100% True Black OLED.
 */
@Composable
fun BoxSetDetailsScreen(
    boxSetId: String,
    viewModel: BoxSetViewModel,
    onNavigateBack: () -> Unit,
    onPlayMovie: (movieId: String, resumeMs: Long) -> Unit,
    onOpenMovieDetails: (movieId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(boxSetId) {
        viewModel.processIntent(BoxSetIntent.LoadBoxSet(boxSetId))
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is BoxSetEffect.LaunchPlayer -> onPlayMovie(effect.mediaId, effect.resumePositionMs)
                is BoxSetEffect.NavigateToMovieDetails -> onOpenMovieDetails(effect.movieId)
                is BoxSetEffect.ShowToast -> { /* Show feedback */ }
                is BoxSetEffect.ShowSnackbar -> { /* Show feedback */ }
                is BoxSetEffect.ShareText -> { /* Share */ }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JellyfinPrimitives.BlackOled)
    ) {
        when (val state = uiState) {
            is BoxSetUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = JellyfinColors.TextPrimary
                )
            }
            is BoxSetUiState.Error -> {
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
                    Button(onClick = { viewModel.processIntent(BoxSetIntent.RetryLoad) }) {
                        Text(text = "إعادة المحاولة")
                    }
                }
            }
            is BoxSetUiState.Success -> {
                val boxSet = state.boxSetDetails

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // 1. Panoramic Backdrop Header
                    item {
                        AdaptiveBackdropHeader(
                            backdropUrl = boxSet.backdropUrl,
                            posterUrl = boxSet.posterUrl,
                            title = boxSet.title
                        )
                    }

                    // 2. Metadata Summary
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = JellyfinDimens.SpaceMd, vertical = JellyfinDimens.SpaceSm)
                        ) {
                            Text(
                                text = boxSet.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = JellyfinColors.TextPrimary
                            )

                            Spacer(modifier = Modifier.height(JellyfinDimens.Space2XS))

                            val meta = buildList {
                                add("${state.totalPartsCount} أجزاء")
                                if (state.formattedTotalRuntime.isNotBlank()) {
                                    add(state.formattedTotalRuntime)
                                }
                            }.joinToString("  •  ")

                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodyMedium,
                                color = JellyfinColors.TextSecondary
                            )

                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMd))

                            // 3. Smart Resume Button
                            val nextPart = state.nextUnwatchedPart
                            if (nextPart != null) {
                                Button(
                                    onClick = { viewModel.processIntent(BoxSetIntent.ResumeFranchise) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JellyfinPrimitives.Ivory50,
                                        contentColor = JellyfinPrimitives.Obsidian950
                                    ),
                                    shape = RoundedCornerShape(JellyfinDimens.RadiusSmall)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(JellyfinDimens.SpaceXS))
                                    Text(
                                        text = "متابعة السلسلة: ${nextPart.title}",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (state.parts.isNotEmpty()) {
                                Button(
                                    onClick = { viewModel.processIntent(BoxSetIntent.PlayFromBeginning) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JellyfinPrimitives.Ivory50,
                                        contentColor = JellyfinPrimitives.Obsidian950
                                    ),
                                    shape = RoundedCornerShape(JellyfinDimens.RadiusSmall)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(JellyfinDimens.SpaceXS))
                                    Text(
                                        text = "بدء السلسلة من الجزء الأول",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSm))

                            // Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSm)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.processIntent(BoxSetIntent.ToggleFavorite) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(JellyfinDimens.RadiusSmall)
                                ) {
                                    Icon(
                                        imageVector = if (state.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (state.isFavorite) JellyfinColors.StatusAlert else JellyfinColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(JellyfinDimens.Space2XS))
                                    Text(text = if (state.isFavorite) "المفضلة" else "إضافة للمفضلة")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.processIntent(BoxSetIntent.ShareFranchise) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(JellyfinDimens.RadiusSmall)
                                ) {
                                    Icon(imageVector = Icons.Rounded.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(JellyfinDimens.Space2XS))
                                    Text(text = "مشاركة")
                                }
                            }

                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLg))

                            Text(
                                text = "أجزاء السلسلة مرتبة زمنياً",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = JellyfinColors.TextPrimary
                            )
                        }
                    }

                    // 4. Chronological Parts List
                    items(state.parts, key = { it.id }) { part ->
                        BoxSetPartCard(
                            part = part,
                            onPlay = { viewModel.processIntent(BoxSetIntent.PlayMovie(part.id, part.resumePositionMs)) },
                            onOpen = { viewModel.processIntent(BoxSetIntent.OpenMovieDetails(part.id)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = JellyfinDimens.SpaceMd, vertical = JellyfinDimens.Space2XS)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(JellyfinDimens.Space3XL))
                    }
                }
            }
        }

        // Top Header Overlay
        CinemaTopBar.Child(
            title = "",
            onBackClick = onNavigateBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun BoxSetPartCard(
    part: BoxSetPartItem,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    ObsidianCard(
        modifier = modifier.clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(JellyfinDimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster 2:3
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(JellyfinDimens.RadiusMicro))
                    .background(JellyfinPrimitives.Graphite800)
            ) {
                AsyncImage(
                    model = part.posterUrl,
                    contentDescription = part.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSm))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "الجزء ${part.partIndex}: ${part.title}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                val subInfo = buildList {
                    part.productionYear?.let { add(it.toString()) }
                    part.durationText?.let { add(it) }
                }.joinToString("  •  ")

                Text(
                    text = subInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.TextSecondary
                )

                if (part.isWatched) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = JellyfinColors.StatusSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تمت المشاهدة",
                            style = MaterialTheme.typography.labelSmall,
                            color = JellyfinColors.StatusSuccess
                        )
                    }
                }
            }

            // Quick Play Button
            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(JellyfinDimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = JellyfinPrimitives.Ivory50,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
