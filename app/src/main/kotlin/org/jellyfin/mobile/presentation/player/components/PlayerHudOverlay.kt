package org.jellyfin.mobile.presentation.player.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.jellyfin.mobile.presentation.player.AspectRatioMode
import org.jellyfin.mobile.presentation.player.AudioOption
import org.jellyfin.mobile.presentation.player.PlayerUiState
import org.jellyfin.mobile.presentation.player.SubtitleOption
import org.jellyfin.mobile.ui.components.GlassmorphicCard
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * PlayerHudOverlay provides the floating frosted-glass cinematic Heads-Up-Display:
 * - Top Bar: Back, Title, Tech Badges ([🟢 Direct Play], [HEVC 4K]), Lock button.
 * - Center: Large circular glowing Play/Pause/Buffering button.
 * - Bottom Bar: Interactive scrubber bar, timestamps, subtitle chip ("💬 عربي"),
 *   audio selector, speed chip (1.0x, 1.25x, 1.5x), aspect ratio toggle, PiP button.
 * - Touch Lock mode: locks HUD and gesture interactions against accidental touches.
 */
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
    modifier: Modifier = Modifier
) {
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // When controls are locked, only show an unobtrusive unlock capsule
    if (uiState.isControlsLocked) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .border(1.dp, JellyfinColors.BorderGlass, CircleShape)
                    .clickable(role = Role.Button, onClick = onToggleLock)
                    .padding(14.dp)
                    .semantics {
                        contentDescription = "إلغاء قفل شاشة المشغل"
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        return
    }

    AnimatedVisibility(
        visible = uiState.isHudVisible && !uiState.isInPictureInPicture,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xAA000000),
                            Color.Transparent,
                            Color(0xCC000000)
                        )
                    )
                )
        ) {
            // ══════════════════════════════════════════════════════════
            // 1. TOP BAR
            // ══════════════════════════════════════════════════════════
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = JellyfinDimens.SpaceLarge, vertical = JellyfinDimens.SpaceMedium)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(JellyfinDimens.MinTouchTarget)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                        .semantics { contentDescription = "الخروج من المشغل" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(JellyfinDimens.SpaceMedium))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = uiState.mediaTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = JellyfinColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Direct Play Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x3310B981))
                                .border(1.dp, Color(0x6610B981), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🟢 Direct Play",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = JellyfinColors.SuccessEmerald
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Video Codec Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x3338BDF8))
                                .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = uiState.videoCodecInfo,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = JellyfinColors.TechSky
                            )
                        }

                        if (uiState.mediaSubtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.mediaSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = JellyfinColors.TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(JellyfinDimens.SpaceMedium))

                // Lock Screen Button
                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier
                        .size(JellyfinDimens.MinTouchTarget)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                        .semantics { contentDescription = "قفل شاشة المشغل لمنع اللمس" }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            // ══════════════════════════════════════════════════════════
            // 2. CENTER PLAY / PAUSE BUTTON
            // ══════════════════════════════════════════════════════════
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (uiState.isBuffering) {
                    CircularProgressIndicator(
                        color = JellyfinColors.PrimaryIndigo,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(68.dp)
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(JellyfinColors.SurfaceGlass)
                            .border(1.5.dp, JellyfinColors.BorderGlass, CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClick = onTogglePlayPause
                            )
                            .semantics {
                                contentDescription = if (uiState.isPlaying) "إيقاف التشغيل مؤقتاً" else "تشغيل الفيديو"
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

            // ══════════════════════════════════════════════════════════
            // 3. BOTTOM HUD BAR (Floating Capsule)
            // ══════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = JellyfinDimens.SpaceLarge, vertical = JellyfinDimens.SpaceLarge)
            ) {
                // Scrubber Bar & Timestamps
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.formattedCurrentTime,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = JellyfinColors.TextPrimary
                    )

                    var sliderPosition by remember(uiState.currentPositionMs) {
                        mutableFloatStateOf(uiState.currentPositionMs.toFloat())
                    }
                    var isUserSeeking by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = JellyfinDimens.SpaceSmall)
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
                                inactiveTrackColor = Color(0x44FFFFFF)
                            ),
                            modifier = Modifier.semantics {
                                contentDescription = "شريط تقدم الفيديو"
                            }
                        )
                    }

                    Text(
                        text = uiState.formattedRemainingTime,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = JellyfinColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                // Quick Action Floating Capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphism(
                            backgroundColor = JellyfinColors.SurfaceGlass,
                            borderColor = JellyfinColors.BorderGlass,
                            cornerRadius = JellyfinDimens.RadiusPill
                        )
                        .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = JellyfinDimens.SpaceSmall)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Subtitle Selector Chip
                        QuickChip(
                            icon = Icons.Rounded.Subtitles,
                            label = uiState.selectedSubtitle?.name ?: "الترجمة",
                            isActive = uiState.selectedSubtitle != null,
                            onClick = { showSubtitleDialog = true }
                        )

                        // Audio Track Selector Chip
                        QuickChip(
                            icon = Icons.AutoMirrored.Rounded.VolumeUp,
                            label = uiState.selectedAudioTrack?.name ?: "الصوت",
                            isActive = true,
                            onClick = { showAudioDialog = true }
                        )

                        // Speed Chip
                        QuickChip(
                            icon = Icons.Rounded.Speed,
                            label = "${uiState.playbackSpeed}x",
                            isActive = uiState.playbackSpeed != 1.0f,
                            onClick = { showSpeedDialog = true }
                        )

                        // Aspect Ratio Chip
                        QuickChip(
                            icon = Icons.Rounded.AspectRatio,
                            label = uiState.aspectRatioMode.name,
                            isActive = uiState.aspectRatioMode != AspectRatioMode.Fit,
                            onClick = {
                                val nextMode = when (uiState.aspectRatioMode) {
                                    AspectRatioMode.Fit -> AspectRatioMode.Fill
                                    AspectRatioMode.Fill -> AspectRatioMode.Stretch
                                    AspectRatioMode.Stretch -> AspectRatioMode.Fit
                                    AspectRatioMode.Original -> AspectRatioMode.Fit
                                }
                                onSetAspectRatio(nextMode)
                            }
                        )

                        // PiP Button
                        IconButton(
                            onClick = onEnterPiP,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .semantics { contentDescription = "وضع صورة داخل صورة" }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureInPictureAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // DIALOGS: Subtitles, Audio, Speed
    // ══════════════════════════════════════════════════════════

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
private fun QuickChip(
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
            fontWeight = FontWeight.Medium,
            color = if (isActive) Color.White else JellyfinColors.TextSecondary,
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
            Column(
                modifier = Modifier.padding(JellyfinDimens.SpaceLarge)
            ) {
                Text(
                    text = "💬 مسارات الترجمة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle offset adjuster (+/- 100ms)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33000000))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "مزامنة الترجمة:",
                        style = MaterialTheme.typography.bodySmall,
                        color = JellyfinColors.TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { onAdjustOffset(-100L) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "-100ms", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${currentOffsetMs}ms",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = JellyfinColors.TechSky
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { onAdjustOffset(100L) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "+100ms", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.height(180.dp)
                ) {
                    item {
                        TrackSelectionItem(
                            title = "إيقاف الترجمة (بدون)",
                            isSelected = selectedSubtitle == null,
                            onClick = { onSelectSubtitle(null) }
                        )
                    }
                    items(subtitles) { sub ->
                        TrackSelectionItem(
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
            Column(
                modifier = Modifier.padding(JellyfinDimens.SpaceLarge)
            ) {
                Text(
                    text = "🔊 المسار الصوتي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.height(160.dp)
                ) {
                    items(audioTracks) { track ->
                        TrackSelectionItem(
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
            Column(
                modifier = Modifier.padding(JellyfinDimens.SpaceLarge)
            ) {
                Text(
                    text = "⚡ سرعة التشغيل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(speeds) { speed ->
                        TrackSelectionItem(
                            title = "${speed}x" + if (speed == 1.0f) " (طبيعي)" else "",
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
private fun TrackSelectionItem(
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) JellyfinColors.TextPrimary else JellyfinColors.TextSecondary
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
