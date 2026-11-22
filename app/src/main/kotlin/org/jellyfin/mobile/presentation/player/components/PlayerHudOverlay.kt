package org.jellyfin.mobile.presentation.player.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jellyfin.mobile.presentation.player.AspectRatioMode
import org.jellyfin.mobile.presentation.player.AudioOption
import org.jellyfin.mobile.presentation.player.PlayerUiState
import org.jellyfin.mobile.presentation.player.SubtitleOption
import org.jellyfin.mobile.ui.theme.JellyfinColors

/**
 * PlayerHudOverlay provides an ultra-minimalist, cinema-grade Heads-Up-Display:
 * - Top Bar: Back navigation, title & subtitle, orientation toggle, touch lock, and unified actions button.
 * - Center Triad: Rewind 10s, Play/Pause/Buffering center master button (76dp), Fast-Forward 10s.
 * - Bottom Bar: Pro-grade scrubber bar with chapter tick marks and monospace timecodes (no cluttering chips).
 * - Screen Lock Mode: Hides all UI and displays a floating lock icon with bounce feedback.
 */
@Composable
fun PlayerHudOverlay(
    uiState: PlayerUiState,
    onNavigateBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onFastSeekBy: (Long) -> Unit,
    onToggleLock: () -> Unit,
    onSelectSubtitle: (SubtitleOption?) -> Unit = {},
    onAdjustSubtitleOffset: (Long) -> Unit = {},
    onSelectAudio: (AudioOption) -> Unit = {},
    onSetPlaybackSpeed: (Float) -> Unit = {},
    onSetAspectRatio: (AspectRatioMode) -> Unit = {},
    onEnterPiP: () -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    onOpenActionsModal: () -> Unit = {},
    onPlayNextEpisode: (() -> Unit)? = null,
    onGetThumbnail: (suspend (Long) -> Bitmap?)? = null,
    modifier: Modifier = Modifier
) {
    var showRemainingTime by remember { mutableStateOf(true) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var scrubTouchX by remember { mutableFloatStateOf(0f) }
    var scrubContainerWidth by remember { mutableFloatStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()
    val lockBounceScale = remember { Animatable(1f) }

    // ══════════════════════════════════════════════════════════════
    // SCREEN LOCK MODE (MX Player style)
    // ══════════════════════════════════════════════════════════════
    if (uiState.isControlsLocked) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(28.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .scale(lockBounceScale.value)
                    .clip(CircleShape)
                    .background(Color(0xCC0F172A))
                    .border(1.5.dp, Color(0x666366F1), CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            coroutineScope.launch {
                                lockBounceScale.animateTo(0.85f, tween(100))
                                lockBounceScale.animateTo(1.1f, tween(150, easing = FastOutSlowInEasing))
                                lockBounceScale.animateTo(1f, tween(100))
                            }
                            onToggleLock()
                        }
                    )
                    .padding(16.dp)
                    .semantics { contentDescription = "إلغاء قفل شاشة المشغل" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        return
    }

    // ══════════════════════════════════════════════════════════════
    // INTERACTIVE HUD OVERLAY (Clean Cinema Triad + Timeline)
    // ══════════════════════════════════════════════════════════════
    AnimatedVisibility(
        visible = uiState.isHudVisible && !uiState.isInPictureInPicture,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(250)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ══════════════════════════════════════════════════════════
            // 1. TOP BAR (Cinematic Gradient Scrim + Clean Typography)
            // ══════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xEE000000),
                                Color(0xAA000000),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Back Button
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x551E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .semantics { contentDescription = "الخروج من المشغل" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Title & Subtitle block
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.mediaTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.mediaSubtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.mediaSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Top Action Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Screen Flip / Rotation Button
                        IconButton(
                            onClick = onToggleOrientation,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x44000000))
                                .border(1.dp, Color(0x22FFFFFF), CircleShape)
                                .semantics { contentDescription = "قلب اتجاه الشاشة" }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ScreenRotation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Screen Lock Button
                        IconButton(
                            onClick = onToggleLock,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x44000000))
                                .border(1.dp, Color(0x22FFFFFF), CircleShape)
                                .semantics { contentDescription = "قفل الشاشة لمنع اللمس العرضي" }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Unified Actions Button (Opens the Modular Actions Modal)
                        IconButton(
                            onClick = onOpenActionsModal,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x551E293B))
                                .border(1.5.dp, Color(0x666366F1), CircleShape)
                                .semantics { contentDescription = "خيارات المشغل والإجراءات" }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════
            // 2. CENTER CONTROLS TRIAD (Rewind 10s • Play/Pause • Forward 10s)
            // ══════════════════════════════════════════════════════════
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
            ) {
                // Rewind 10s Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x660F172A))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable(role = Role.Button, onClick = { onFastSeekBy(-10_000L) })
                        .semantics { contentDescription = "ترجيع 10 ثوانٍ" }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.FastRewind,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "10s",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(36.dp))

                // Center Play / Pause / Buffering Master Button
                val isPlayingOrReady = uiState.playWhenReady || uiState.isPlaying
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xEE1E293B), Color(0xCC0F172A))
                            )
                        )
                        .border(2.dp, Color(0x886366F1), CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = onTogglePlayPause
                        )
                        .semantics {
                            contentDescription = if (isPlayingOrReady) "إيقاف مؤقت" else "تشغيل"
                        }
                ) {
                    if (uiState.isBuffering) {
                        CircularProgressIndicator(
                            color = JellyfinColors.TechSky,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(68.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isPlayingOrReady) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.width(36.dp))

                // Fast-Forward 10s Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x660F172A))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable(role = Role.Button, onClick = { onFastSeekBy(10_000L) })
                        .semantics { contentDescription = "تقديم 10 ثوانٍ" }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "10s",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════════
            // 3. BOTTOM BAR (Ultra-Clean Timeline Scrubber Only)
            // ══════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xAA000000),
                                Color(0xF2000000)
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Floating Thumbnail Preview Card while scrubbing
                    AnimatedVisibility(
                        visible = isScrubbing,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150))
                    ) {
                        var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
                        LaunchedEffect(scrubPositionMs, isScrubbing) {
                            if (isScrubbing && onGetThumbnail != null) {
                                previewBitmap = onGetThumbnail(scrubPositionMs)
                            } else {
                                previewBitmap = null
                            }
                        }
                        ThumbnailPreviewBox(
                            positionMs = scrubPositionMs,
                            thumbnail = previewBitmap,
                            touchX = scrubTouchX,
                            containerWidth = scrubContainerWidth,
                            bottomOffsetDp = 30.dp
                        )
                    }

                    // Timeline Scrubber Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.formattedCurrentTime,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )

                        val chapterMarkers = remember(uiState.chapters) {
                            uiState.chapters.map { ChapterMarker(it.startPositionMs, it.name) }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            CinemaScrubber(
                                positionMs = uiState.currentPositionMs,
                                durationMs = uiState.totalDurationMs,
                                chapters = chapterMarkers,
                                onSeek = { /* non-blocking scrub tracking */ },
                                onSeekFinished = { onSeekTo(it) },
                                onScrubProgress = { scrubbing, scrubMs, touchX, totalWidth ->
                                    isScrubbing = scrubbing
                                    scrubPositionMs = scrubMs
                                    scrubTouchX = touchX
                                    scrubContainerWidth = totalWidth
                                }
                            )
                        }

                        Text(
                            text = if (showRemainingTime) uiState.formattedRemainingTime else uiState.formattedTotalTime,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.clickable {
                                showRemainingTime = !showRemainingTime
                            }
                        )
                    }
                }
            }
        }
    }
}
