package org.jellyfin.mobile.presentation.details.boxset

import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.core.player.external.ExternalPlayerIntentBuilder
import org.jellyfin.mobile.core.player.external.ExternalPlayerPayload
import org.jellyfin.mobile.core.player.external.ExternalPlayerResultParser
import org.jellyfin.mobile.core.util.BidiFormatter
import org.jellyfin.mobile.presentation.core.components.AmbientGlow
import org.jellyfin.mobile.presentation.core.components.rememberDominantColor
import org.jellyfin.mobile.presentation.details.components.AdaptiveBackdropHeader
import org.jellyfin.mobile.presentation.details.components.SaveModalBottomSheet
import org.jellyfin.mobile.presentation.details.components.TopActionsDropdownMenu
import org.jellyfin.mobile.ui.components.ObsidianCard
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

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

    LaunchedEffect(boxSetId) {
        viewModel.processIntent(BoxSetIntent.LoadBoxSet(boxSetId))
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is BoxSetEffect.LaunchPlayer -> {
                    onPlayMovie(effect.mediaId, effect.resumePositionMs)
                }
                is BoxSetEffect.LaunchExternalPlayer -> {
                    activeExternalPayload = effect.payload
                    externalLaunchTimestamp = SystemClock.elapsedRealtime()
                    val chooserIntent = ExternalPlayerIntentBuilder.buildChooserIntent(effect.payload)
                    externalPlayerLauncher.launch(chooserIntent)
                }
                is BoxSetEffect.NavigateToMovieDetails -> {
                    onOpenMovieDetails(effect.movieId)
                }
                is BoxSetEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is BoxSetEffect.ShowSnackbar -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is BoxSetEffect.ShareText -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "مشاركة السلسلة")
                    context.startActivity(shareIntent)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        when (val state = uiState) {
            is BoxSetUiState.Loading -> {
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

            is BoxSetUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(JellyfinDimens.SpaceLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = JellyfinColors.StatusAlert,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
                    Button(onClick = { viewModel.processIntent(BoxSetIntent.RetryLoad) }) {
                        Text(text = "إعادة المحاولة")
                    }
                }
            }

            is BoxSetUiState.Success -> {
                val boxSet = state.boxSetDetails
                val dominantColor by rememberDominantColor(imageUrl = boxSet.backdropUrl ?: boxSet.posterUrl)
                val configuration = LocalConfiguration.current
                val heroHeight = (configuration.screenHeightDp * 0.50f).dp.coerceIn(240.dp, 440.dp)

                // 1. Ambient Glow
                AmbientGlow(
                    color = dominantColor,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // 2. Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 80.dp)
                ) {
                    // Panoramic Backdrop Header (50% Hero Canvas)
                    AdaptiveBackdropHeader(
                        backdropUrl = boxSet.backdropUrl,
                        posterUrl = boxSet.posterUrl,
                        title = boxSet.title,
                        maxHeight = heroHeight,
                        backgroundColor = backgroundColor
                    )

                    // Details Content Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = JellyfinDimens.SpaceLarge)
                    ) {
                        // Title
                        Text(
                            text = BidiFormatter.wrapLtr(boxSet.title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = JellyfinColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Cumulative Metadata Row
                        BoxSetMetadataRow(
                            totalPartsCount = state.totalPartsCount,
                            formattedTotalRuntime = state.formattedTotalRuntime,
                            communityRating = boxSet.communityRating
                        )

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

                        // Single Smart Franchise Play Button (54dp Ivory50)
                        BoxSetPrimaryActionButton(
                            nextUnwatchedPart = state.nextUnwatchedPart,
                            allWatched = state.isWatched,
                            partsCount = state.parts.size,
                            onPlayClick = {
                                if (state.nextUnwatchedPart != null) {
                                    viewModel.processIntent(BoxSetIntent.ResumeFranchise)
                                } else {
                                    viewModel.processIntent(BoxSetIntent.PlayFromBeginning)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                        // Secondary Action Duo: Save in Lists (50%) + External Player (50%)
                        BoxSetSecondaryActionDuo(
                            isSaved = state.isFavorite || state.isWatchlist,
                            onSaveClick = { viewModel.processIntent(BoxSetIntent.OpenSaveModal) },
                            onExternalPlayerClick = { viewModel.processIntent(BoxSetIntent.PlayExternalNext) }
                        )

                        // Expandable Synopsis Section
                        if (boxSet.overview.isNotBlank()) {
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))
                            Text(
                                text = "عن السلسلة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = JellyfinColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .clickable { isOverviewExpanded = !isOverviewExpanded }
                            ) {
                                Text(
                                    text = boxSet.overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = JellyfinColors.TextSecondary,
                                    maxLines = if (isOverviewExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isOverviewExpanded) "إخفاء" else "قراءة المزيد...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JellyfinColors.TechSky,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceLarge))

                        // Chronological Movies Header
                        Text(
                            text = "أجزاء السلسلة مرتبة زمنياً",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = JellyfinColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                        // Parts List
                        state.parts.forEach { part ->
                            BoxSetPartCard(
                                part = part,
                                onPlay = {
                                    viewModel.processIntent(
                                        BoxSetIntent.PlayMovie(part.id, part.resumePositionMs)
                                    )
                                },
                                onOpen = {
                                    viewModel.processIntent(
                                        BoxSetIntent.OpenMovieDetails(part.id)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // 3. Floating Transparent Top App Bar
                BoxSetTopBar(
                    onBackClick = onNavigateBack,
                    onPlaybackOptionsClick = {
                        viewModel.processIntent(BoxSetIntent.PlayExternalNext)
                    },
                    onShareClick = {
                        viewModel.processIntent(BoxSetIntent.ShareFranchise)
                    },
                    isWatched = state.isWatched,
                    onToggleWatchedClick = {
                        viewModel.processIntent(BoxSetIntent.ToggleEntireCollectionWatched)
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // 4. Save Modal Bottom Sheet
                if (state.isSaveModalVisible) {
                    SaveModalBottomSheet(
                        isFavorite = state.isFavorite,
                        isWatchlist = state.isWatchlist,
                        onToggleFavorite = { viewModel.processIntent(BoxSetIntent.ToggleFavorite) },
                        onToggleWatchlist = { viewModel.processIntent(BoxSetIntent.ToggleWatchlist) },
                        onDismiss = { viewModel.processIntent(BoxSetIntent.DismissSaveModal) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxSetMetadataRow(
    totalPartsCount: Int,
    formattedTotalRuntime: String,
    communityRating: Float?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (communityRating != null && communityRating > 0f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(JellyfinPrimitives.Graphite800)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = JellyfinColors.RatingGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f", communityRating),
                    style = MaterialTheme.typography.labelSmall,
                    color = JellyfinColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val metaText = buildList {
            add("$totalPartsCount أجزاء")
            if (formattedTotalRuntime.isNotBlank()) {
                add(formattedTotalRuntime)
            }
        }.joinToString("  •  ")

        Text(
            text = metaText,
            style = MaterialTheme.typography.bodySmall,
            color = JellyfinColors.TextSecondary
        )
    }
}

@Composable
private fun BoxSetPrimaryActionButton(
    nextUnwatchedPart: BoxSetPartItem?,
    allWatched: Boolean,
    partsCount: Int,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonText = when {
        nextUnwatchedPart != null -> "متابعة السلسلة: ${nextUnwatchedPart.title}"
        allWatched -> "إعادة مشاهدة السلسلة"
        partsCount > 0 -> "بدء السلسلة: الجزء 1"
        else -> "تشغيل السلسلة"
    }

    val iconVector = if (allWatched && nextUnwatchedPart == null) {
        Icons.Rounded.Replay
    } else {
        Icons.Rounded.PlayArrow
    }

    Button(
        onClick = onPlayClick,
        shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
        colors = ButtonDefaults.buttonColors(
            containerColor = JellyfinPrimitives.Ivory50,
            contentColor = JellyfinPrimitives.Obsidian950
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .semantics {
                role = Role.Button
                contentDescription = buttonText
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BoxSetSecondaryActionDuo(
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    onExternalPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
    ) {
        // Save in Lists (50%)
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

        // External Player (50%)
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
                tint = JellyfinColors.TechSky,
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

@Composable
private fun BoxSetTopBar(
    onBackClick: () -> Unit,
    onPlaybackOptionsClick: () -> Unit,
    onShareClick: () -> Unit,
    isWatched: Boolean,
    onToggleWatchedClick: () -> Unit,
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
            hasTrailer = false,
            onTrailerClick = {}
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
                .padding(JellyfinDimens.SpaceSmall),
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

            Spacer(modifier = Modifier.width(JellyfinDimens.SpaceSmall))

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
                    part.communityRating?.let { add(String.format("%.1f", it)) }
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

            // Quick Play Button (48dp touch target)
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
