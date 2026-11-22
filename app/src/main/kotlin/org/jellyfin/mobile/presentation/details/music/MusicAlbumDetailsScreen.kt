package org.jellyfin.mobile.presentation.details.music

import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * Mobile Music Album Details Screen.
 * Implements docs/current/screens/media_details/05_music_album_details_screen.md:
 * - Square 1:1 Album Artwork Hero with subtle reflection and pure OLED black integration.
 * - Primary action buttons: Play All (Ivory) and Shuffle.
 * - Interactive numbered tracklist with cinema gold indicator for currently playing track.
 * - Persistent sticky bottom Mini-Player (64dp height).
 * - Synchronized lyrics bottom sheet with dynamic highlighting.
 * Zero emojis across code, comments, and strings.
 */
@Composable
fun MusicAlbumDetailsScreen(
    albumId: String,
    viewModel: MusicAlbumViewModel,
    onNavigateBack: () -> Unit,
    onLaunchPlayer: (String, Long) -> Unit,
    onNavigateToArtist: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinPrimitives.Obsidian950
    val context = LocalContext.current

    LaunchedEffect(albumId) {
        viewModel.processIntent(MusicAlbumIntent.LoadAlbum(albumId))
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is MusicAlbumEffect.LaunchAudioPlayer -> {
                    onLaunchPlayer(effect.trackId, effect.positionMs)
                }
                is MusicAlbumEffect.NavigateBack -> {
                    onNavigateBack()
                }
                is MusicAlbumEffect.OpenArtistDetails -> {
                    onNavigateToArtist(effect.artistId, effect.artistName)
                }
                is MusicAlbumEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MusicAlbumEffect.ShowSnackbar -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is MusicAlbumEffect.ShareAlbum -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "${effect.title} - ${effect.artist}")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "مشاركة الألبوم")
                    context.startActivity(shareIntent)
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.processIntent(MusicAlbumIntent.RefreshPlaybackState)
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
            is MusicAlbumUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = JellyfinColors.RatingGold,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            is MusicAlbumUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = JellyfinColors.TextSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.processIntent(MusicAlbumIntent.RetryLoad) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JellyfinColors.SurfaceElevated,
                                contentColor = JellyfinColors.TextPrimary
                            )
                        ) {
                            Text(text = "إعادة المحاولة")
                        }
                    }
                }
            }

            is MusicAlbumUiState.Success -> {
                MusicAlbumSuccessContent(
                    state = state,
                    onIntent = viewModel::processIntent,
                    onNavigateBack = onNavigateBack,
                    onNavigateToArtist = onNavigateToArtist
                )
            }
        }
    }
}

@Composable
private fun MusicAlbumSuccessContent(
    state: MusicAlbumUiState.Success,
    onIntent: (MusicAlbumIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToArtist: (String, String) -> Unit
) {
    val listState = rememberLazyListState()
    val album = state.album
    val hasMiniPlayer = state.miniPlayer != null

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = if (hasMiniPlayer) 88.dp else 24.dp
            )
        ) {
            // 1. Top Bar & 1:1 Album Artwork Hero Header
            item(key = "album_hero_header") {
                AlbumHeroHeader(
                    album = album,
                    onNavigateBack = onNavigateBack,
                    onToggleFavorite = { onIntent(MusicAlbumIntent.ToggleFavoriteAlbum) },
                    onNavigateToArtist = onNavigateToArtist
                )
            }

            // 2. Play All & Shuffle Buttons
            item(key = "album_action_buttons") {
                AlbumPrimaryActionsRow(
                    isPlaying = state.isPlaying,
                    onPlayAll = { onIntent(MusicAlbumIntent.PlayAll) },
                    onShuffleAll = { onIntent(MusicAlbumIntent.ShuffleAll) }
                )
            }

            // 3. Quick Actions Row (Download, Favorite, Lyrics, Radio)
            item(key = "album_quick_actions") {
                AlbumQuickActionsBar(
                    isFavorite = state.isFavorite,
                    onToggleFavorite = { onIntent(MusicAlbumIntent.ToggleFavoriteAlbum) },
                    onOpenLyrics = { onIntent(MusicAlbumIntent.OpenLyrics(null)) }
                )
            }

            // 4. Section Title: Numbered Tracklist
            item(key = "tracklist_header") {
                TracklistHeader(trackCount = state.tracks.size)
            }

            // 5. Interactive Track Items
            itemsIndexed(
                items = state.tracks,
                key = { _, track -> track.id }
            ) { index, track ->
                TrackRowItem(
                    index = index + 1,
                    track = track,
                    isCurrentTrack = track.id == state.currentPlayingTrackId,
                    isPlaying = state.isPlaying && track.id == state.currentPlayingTrackId,
                    onClick = { onIntent(MusicAlbumIntent.PlayTrack(track.id)) },
                    onOptionsClick = { onIntent(MusicAlbumIntent.OpenTrackOptions(track.id)) }
                )
            }
        }

        // 6. Persistent Sticky Bottom Mini-Player (64dp)
        AnimatedVisibility(
            visible = state.miniPlayer != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            state.miniPlayer?.let { mini ->
                BottomStickyMiniPlayer(
                    miniPlayer = mini,
                    onTogglePlayPause = { onIntent(MusicAlbumIntent.TogglePlayPause) },
                    onNextTrack = { onIntent(MusicAlbumIntent.NextTrack) },
                    onOpenLyrics = { onIntent(MusicAlbumIntent.OpenLyrics(mini.trackId)) }
                )
            }
        }
    }

    // Synchronized Lyrics Bottom Sheet
    if (state.showLyricsSheet && state.lyrics != null) {
        LyricsModalBottomSheet(
            lyrics = state.lyrics,
            onDismiss = { onIntent(MusicAlbumIntent.DismissLyrics) }
        )
    }

    // Track Options Bottom Sheet
    state.selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            onDismiss = { onIntent(MusicAlbumIntent.DismissTrackOptions) },
            onToggleFavorite = {
                onIntent(MusicAlbumIntent.ToggleFavoriteTrack(track.id))
                onIntent(MusicAlbumIntent.DismissTrackOptions)
            },
            onOpenLyrics = {
                onIntent(MusicAlbumIntent.DismissTrackOptions)
                onIntent(MusicAlbumIntent.OpenLyrics(track.id))
            }
        )
    }
}

/**
 * Centered Square 1:1 Album Artwork Hero Header with Top Bar, metadata, and quality badge.
 */
@Composable
private fun AlbumHeroHeader(
    album: MusicAlbumUiModel,
    onNavigateBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onNavigateToArtist: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(JellyfinDimens.MinTouchTarget)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "الرجوع",
                    tint = JellyfinColors.TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(JellyfinDimens.MinTouchTarget)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = if (album.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "المفضلة",
                        tint = if (album.isFavorite) JellyfinColors.RatingGold else JellyfinColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Square 1:1 Album Artwork with smooth shadow and hairline border
        Box(
            modifier = Modifier
                .size(240.dp)
                .aspectRatio(1f)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                    spotColor = Color.Black
                )
                .clip(RoundedCornerShape(JellyfinDimens.RadiusMedium))
                .border(
                    width = JellyfinDimens.BorderHairline,
                    color = JellyfinColors.BorderHairline,
                    shape = RoundedCornerShape(JellyfinDimens.RadiusMedium)
                )
                .background(JellyfinPrimitives.Graphite900),
            contentAlignment = Alignment.Center
        ) {
            if (!album.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = JellyfinColors.TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Album Title
        Text(
            text = album.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = JellyfinPrimitives.Ivory50,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Artist Name
        Text(
            text = "الفنان: ${album.artist}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = JellyfinColors.RatingGold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clickable {
                    album.artistId?.let { id -> onNavigateToArtist(id, album.artist) }
                }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Metadata line & Badges (Year, tracks count, total duration, Hi-Res badge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            val metaParts = buildList {
                album.year?.let { add(it.toString()) }
                if (album.trackCount > 0) add("${album.trackCount} مقطوعة")
                if (album.totalDurationText.isNotBlank()) add(album.totalDurationText)
            }

            Text(
                text = metaParts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = JellyfinColors.TextSecondary
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Quality Badge [Hi-Res / Lossless]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(JellyfinDimens.RadiusMicro))
                    .border(
                        width = JellyfinDimens.BorderHairline,
                        color = JellyfinPrimitives.Slate700,
                        shape = RoundedCornerShape(JellyfinDimens.RadiusMicro)
                    )
                    .background(JellyfinPrimitives.Graphite800)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = album.qualityBadge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = JellyfinColors.RatingGold
                )
            }
        }
    }
}

/**
 * Primary Actions: Wide Play All button (Ivory) and Shuffle button.
 */
@Composable
private fun AlbumPrimaryActionsRow(
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play All Button (Ivory50 surface with bold black text)
        Button(
            onClick = onPlayAll,
            modifier = Modifier
                .weight(1.2f)
                .height(52.dp)
                .semantics { role = Role.Button },
            shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
            colors = ButtonDefaults.buttonColors(
                containerColor = JellyfinPrimitives.Ivory50,
                contentColor = JellyfinPrimitives.BlackOled
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPlaying) "استئناف الألبوم" else "تشغيل الألبوم",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
        }

        // Shuffle Button (Obsidian elevated container)
        OutlinedButton(
            onClick = onShuffleAll,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .semantics { role = Role.Button },
            shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = JellyfinPrimitives.Graphite800,
                contentColor = JellyfinColors.TextPrimary
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                brush = Brush.horizontalGradient(
                    listOf(JellyfinColors.BorderHairline, JellyfinColors.BorderHairline)
                )
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = JellyfinColors.RatingGold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "خلط عشوائي",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            )
        }
    }
}

/**
 * Quick Action Bar: Secondary actions (Download, Favorite, Lyrics shortcut, Radio Mix).
 */
@Composable
private fun AlbumQuickActionsBar(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenLyrics: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickActionButton(
            icon = Icons.Rounded.Download,
            label = "تنزيل",
            onClick = { /* Offline download action */ }
        )

        QuickActionButton(
            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            label = "المفضلة",
            tint = if (isFavorite) JellyfinColors.RatingGold else JellyfinColors.TextSecondary,
            onClick = onToggleFavorite
        )

        QuickActionButton(
            icon = Icons.Rounded.Mic,
            label = "الكلمات",
            tint = JellyfinPrimitives.Ivory50,
            onClick = onOpenLyrics
        )

        QuickActionButton(
            icon = Icons.Rounded.Radio,
            label = "راديو Mix",
            onClick = { /* Mix station */ }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = JellyfinColors.TextSecondary,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .defaultMinSize(minWidth = JellyfinDimens.MinTouchTarget, minHeight = JellyfinDimens.MinTouchTarget)
            .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = JellyfinColors.TextSecondary
        )
    }
}

/**
 * Tracklist Section Header.
 */
@Composable
private fun TracklistHeader(trackCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "مسارات الألبوم المرقمة",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = JellyfinColors.TextPrimary
        )

        Text(
            text = "$trackCount مقطوعة",
            style = MaterialTheme.typography.labelMedium,
            color = JellyfinColors.TextMuted
        )
    }
}

/**
 * Individual Track Row Item with 01, 02 numbering, title, duration, and options button.
 * Currently active track is highlighted in Cinema Gold with an animated audio equalizer indicator.
 */
@Composable
private fun TrackRowItem(
    index: Int,
    track: MusicTrackUiModel,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isCurrentTrack) {
            JellyfinPrimitives.Graphite800.copy(alpha = 0.7f)
        } else {
            Color.Transparent
        },
        label = "trackBgColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(animatedBgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Number (01, 02) or Animated Audio Equalizer
        Box(
            modifier = Modifier.width(36.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isCurrentTrack && isPlaying) {
                AnimatedAudioEqualizer()
            } else {
                Text(
                    text = String.format(java.util.Locale.ROOT, "%02d", index),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    ),
                    color = if (isCurrentTrack) JellyfinColors.RatingGold else JellyfinColors.TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Track Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = if (isCurrentTrack) JellyfinColors.RatingGold else JellyfinPrimitives.Ivory50,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!track.artist.isNullOrBlank()) {
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = JellyfinColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Duration formatted (e.g. 05:20)
        Text(
            text = track.durationFormatted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = if (isCurrentTrack) JellyfinColors.RatingGold else JellyfinColors.TextSecondary
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Options Button (...)
        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier
                .size(JellyfinDimens.MinTouchTarget)
                .semantics { role = Role.Button }
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "خيارات إضافية للمقطوعة",
                tint = JellyfinColors.TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Animated Audio Equalizer indicator for the currently active track.
 */
@Composable
private fun AnimatedAudioEqualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizerTransition")

    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = Modifier
            .size(width = 18.dp, height = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * bar1Height).dp)
                .clip(RoundedCornerShape(1.dp))
                .background(JellyfinColors.RatingGold)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * bar2Height).dp)
                .clip(RoundedCornerShape(1.dp))
                .background(JellyfinColors.RatingGold)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * bar3Height).dp)
                .clip(RoundedCornerShape(1.dp))
                .background(JellyfinColors.RatingGold)
        )
    }
}

/**
 * Persistent Sticky Bottom Mini-Player (64dp height).
 * Pinned at the bottom of the screen with square artwork, title/artist, play/pause, next track, and lyrics button.
 */
@Composable
private fun BottomStickyMiniPlayer(
    miniPlayer: MiniPlayerUiModel,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onOpenLyrics: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Thin progress indicator along top edge
        LinearProgressIndicator(
            progress = { miniPlayer.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = JellyfinColors.RatingGold,
            trackColor = JellyfinColors.BorderHairline,
        )

        // Mini player bar container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(JellyfinPrimitives.Graphite900)
                .border(
                    width = JellyfinDimens.BorderHairline,
                    color = JellyfinColors.BorderHairline
                )
                .clickable(onClick = onOpenLyrics)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Album Artwork
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                    .border(
                        width = JellyfinDimens.BorderHairline,
                        color = JellyfinColors.BorderHairline,
                        shape = RoundedCornerShape(JellyfinDimens.RadiusSmall)
                    )
                    .background(JellyfinPrimitives.Graphite800),
                contentAlignment = Alignment.Center
            ) {
                if (!miniPlayer.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = miniPlayer.coverUrl,
                        contentDescription = miniPlayer.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = null,
                        tint = JellyfinColors.TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track title & Artist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = miniPlayer.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = JellyfinPrimitives.Ivory50,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = miniPlayer.artist,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = JellyfinColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Lyrics Button
            IconButton(
                onClick = onOpenLyrics,
                modifier = Modifier.size(JellyfinDimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "عرض كلمات الأغنية",
                    tint = JellyfinColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Play / Pause Toggle Button
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(JellyfinDimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = if (miniPlayer.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (miniPlayer.isPlaying) "إيقاف مؤقت" else "تشغيل",
                    tint = JellyfinColors.RatingGold,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Skip Next Button
            IconButton(
                onClick = onNextTrack,
                modifier = Modifier.size(JellyfinDimens.MinTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "المقطوعة التالية",
                    tint = JellyfinColors.TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Synchronized Lyrics Modal Bottom Sheet.
 * Displays real-time scrolling lines with highlight on active lyric lines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsModalBottomSheet(
    lyrics: LyricsUiModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JellyfinPrimitives.Obsidian950,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(JellyfinPrimitives.Slate700)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = lyrics.trackTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = JellyfinPrimitives.Ivory50,
                textAlign = TextAlign.Center
            )

            Text(
                text = lyrics.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = JellyfinColors.RatingGold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Lyrics Lines List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    val isLineActive = index == 0 || line.isActive
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isLineActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isLineActive) 18.sp else 15.sp
                        ),
                        color = if (isLineActive) JellyfinPrimitives.Ivory50 else JellyfinColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Track Options Bottom Sheet: Playlist, Favorite, Lyrics, Share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackOptionsBottomSheet(
    track: MusicTrackUiModel,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenLyrics: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JellyfinPrimitives.Graphite900,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(JellyfinPrimitives.Slate700)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = JellyfinColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!track.artist.isNullOrBlank()) {
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = JellyfinColors.TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TrackOptionItem(
                icon = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                title = if (track.isFavorite) "إزالة المقطوعة من المفضلة" else "إضافة المقطوعة إلى المفضلة",
                onClick = onToggleFavorite
            )

            TrackOptionItem(
                icon = Icons.Rounded.Mic,
                title = "عرض كلمات الأغنية",
                onClick = onOpenLyrics
            )

            TrackOptionItem(
                icon = Icons.Rounded.Share,
                title = "مشاركة المقطوعة",
                onClick = onDismiss
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TrackOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
            .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = JellyfinColors.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = JellyfinColors.TextPrimary
        )
    }
}
