package org.jellyfin.mobile.presentation.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.jellyfin.mobile.presentation.player.AspectRatioMode
import org.jellyfin.mobile.presentation.player.AudioOption
import org.jellyfin.mobile.presentation.player.PlayerUiState
import org.jellyfin.mobile.presentation.player.SubtitleOption
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * PlayerHudOverlay provides an ultra-refined, cinema-grade Heads-Up-Display
 * inspired by MX Player Pro and Infuse:
 * - Top Bar: Deep gradient scrim, crisp title typography, quick settings chips, and lock button.
 * - Center Triad: Smooth, glassmorphic Rewind -10s, Play/Pause/Buffering, and Fast-Forward +10s controls.
 * - Bottom Bar: Pro-grade scrubber bar with buffered range and interactive thumb, plus a floating
 *   quick-actions capsule (Subtitles, Audio, Speed, Aspect Ratio, Next Episode, PiP).
 * - Screen Lock Mode: Hides all UI and shows a floating lock icon with gentle feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerHudOverlay(
    uiState: PlayerUiState,
    onNavigateBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onFastSeekBy: (Long) -> Unit,
    onToggleLock: () -> Unit,
    onSelectSubtitle: (SubtitleOption?) -> Unit,
    onAdjustSubtitleOffset: (Long) -> Unit,
    onSelectAudio: (AudioOption) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetAspectRatio: (AspectRatioMode) -> Unit,
    onEnterPiP: () -> Unit,
    onToggleOrientation: () -> Unit = {},
    onPlayNextEpisode: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(true) }

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
    // INTERACTIVE HUD OVERLAY (MX Player Triad + Pro Scrubber)
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

                    // Top Quick Action Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Screen Flip / Rotation Button
                        IconButton(
                            onClick = onToggleOrientation,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x44000000))
                                .semantics { contentDescription = "قلب اتجاه الشاشة" }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ScreenRotation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Subtitle Dialog Button
                        IconButton(
                            onClick = { showSubtitleDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (uiState.selectedSubtitle != null) Color(0x446366F1) else Color(0x44000000))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Subtitles,
                                contentDescription = "الترجمة",
                                tint = if (uiState.selectedSubtitle != null) JellyfinColors.TechSky else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Screen Lock Button
                        IconButton(
                            onClick = onToggleLock,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x44000000))
                                .semantics { contentDescription = "قفل الشاشة لمنع اللمس العرضي" }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(76.dp)
                ) {
                    if (uiState.isBuffering) {
                        CircularProgressIndicator(
                            color = JellyfinColors.TechSky,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(68.dp)
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
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
                                    contentDescription = if (uiState.isPlaying) "إيقاف مؤقت" else "تشغيل"
                                }
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
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
            // 3. BOTTOM BAR (Cinema Scrubber & Quick Actions Capsule)
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
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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

                        var sliderPosition by remember(uiState.currentPositionMs) {
                            mutableFloatStateOf(uiState.currentPositionMs.toFloat())
                        }
                        var isUserSeeking by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Slider(
                                value = if (isUserSeeking) sliderPosition else uiState.currentPositionMs.toFloat(),
                                onValueChange = {
                                    isUserSeeking = true
                                    sliderPosition = it
                                },
                                onValueChangeFinished = {
                                    isUserSeeking = false
                                    onSeekTo(sliderPosition.toLong())
                                },
                                valueRange = 0f..uiState.totalDurationMs.toFloat().coerceAtLeast(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = JellyfinColors.TechSky,
                                    activeTrackColor = JellyfinColors.PrimaryIndigo,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.semantics {
                                    contentDescription = "شريط تقدم الفيديو السينمائي"
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Floating Quick Actions Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphism(
                                backgroundColor = Color(0xDD0F172A),
                                borderColor = Color(0x336366F1),
                                cornerRadius = JellyfinDimens.RadiusPill
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Subtitle Chip
                            MxQuickChip(
                                icon = Icons.Rounded.Subtitles,
                                label = uiState.selectedSubtitle?.name ?: "الترجمة",
                                isActive = uiState.selectedSubtitle != null,
                                onClick = { showSubtitleDialog = true }
                            )

                            // Aspect Ratio Chip (Toggle between Fit and Fill)
                            val isFill = uiState.aspectRatioMode == AspectRatioMode.Fill
                            MxQuickChip(
                                icon = Icons.Rounded.AspectRatio,
                                label = if (isFill) "ملء الشاشة" else "العرض الأصلي",
                                isActive = isFill,
                                onClick = {
                                    val nextMode = if (isFill) AspectRatioMode.Fit else AspectRatioMode.Fill
                                    onSetAspectRatio(nextMode)
                                }
                            )

                            // Next Episode Button (Visible only when series has next episode)
                            if (!uiState.nextEpisodeId.isNullOrBlank() && onPlayNextEpisode != null) {
                                MxQuickChip(
                                    icon = Icons.Rounded.SkipNext,
                                    label = "الحلقة التالية",
                                    isActive = true,
                                    onClick = onPlayNextEpisode
                                )
                            }

                            // PiP Button
                            IconButton(
                                onClick = onEnterPiP,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                                    .semantics { contentDescription = "وضع صورة داخل صورة" }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PictureInPictureAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DIALOGS: Subtitles, Audio, Speed
    // ══════════════════════════════════════════════════════════════

    if (showSubtitleDialog) {
        SubtitleSelectionDialog(
            subtitles = uiState.availableSubtitles,
            selectedSubtitle = uiState.selectedSubtitle,
            currentOffsetMs = uiState.subtitleOffsetMs,
            onSelectSubtitle = {
                onSelectSubtitle(it)
                showSubtitleDialog = false
            },
            onAdjustOffset = onAdjustSubtitleOffset,
            onDismiss = { showSubtitleDialog = false }
        )
    }

    if (showAudioDialog) {
        AudioTrackDialog(
            audioTracks = uiState.availableAudioTracks,
            selectedAudioTrack = uiState.selectedAudioTrack,
            onSelectAudio = {
                onSelectAudio(it)
                showAudioDialog = false
            },
            onDismiss = { showAudioDialog = false }
        )
    }

    if (showSpeedDialog) {
        SpeedSelectionDialog(
            currentSpeed = uiState.playbackSpeed,
            onSelectSpeed = {
                onSetPlaybackSpeed(it)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }
}

@Composable
private fun MxQuickChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(JellyfinDimens.RadiusPill))
            .background(if (isActive) Color(0x336366F1) else Color(0x22FFFFFF))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) JellyfinColors.TechSky else Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) Color.White else Color(0xFFCBD5E1),
            maxLines = 1
        )
    }
}

@Composable
private fun SubtitleSelectionDialog(
    subtitles: List<SubtitleOption>,
    selectedSubtitle: SubtitleOption?,
    currentOffsetMs: Long,
    onSelectSubtitle: (SubtitleOption?) -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(JellyfinDimens.SpaceLarge)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "💬 مسارات الترجمة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Subtitle offset adjuster (+/- 100ms)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x44000000))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "مزامنة الترجمة:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { onAdjustOffset(-100L) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(text = "-100ms", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${currentOffsetMs}ms",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = JellyfinColors.TechSky
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { onAdjustOffset(100L) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(text = "+100ms", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(modifier = Modifier.height(180.dp)) {
                    item {
                        MxTrackSelectionItem(
                            title = "إيقاف الترجمة (بدون)",
                            isSelected = selectedSubtitle == null,
                            onClick = { onSelectSubtitle(null) }
                        )
                    }
                    items(subtitles) { sub ->
                        MxTrackSelectionItem(
                            title = sub.name,
                            isSelected = selectedSubtitle?.index == sub.index,
                            onClick = { onSelectSubtitle(sub) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTrackDialog(
    audioTracks: List<AudioOption>,
    selectedAudioTrack: AudioOption?,
    onSelectAudio: (AudioOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(JellyfinDimens.SpaceLarge)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🔊 المسار الصوتي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(modifier = Modifier.height(160.dp)) {
                    items(audioTracks) { track ->
                        MxTrackSelectionItem(
                            title = track.name,
                            isSelected = selectedAudioTrack?.index == track.index,
                            onClick = { onSelectAudio(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(JellyfinDimens.SpaceLarge)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "⚡ سرعة التشغيل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(speeds) { speed ->
                        MxTrackSelectionItem(
                            title = "${speed}x" + if (speed == 1.0f) " (السرعة الطبيعية)" else "",
                            isSelected = currentSpeed == speed,
                            onClick = { onSelectSpeed(speed) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MxTrackSelectionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0x336366F1) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF94A3B8)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = JellyfinColors.TechSky,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}