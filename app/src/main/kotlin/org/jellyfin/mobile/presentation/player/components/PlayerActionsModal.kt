package org.jellyfin.mobile.presentation.player.components

import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import org.jellyfin.mobile.presentation.player.AspectRatioMode
import org.jellyfin.mobile.presentation.player.AudioOption
import org.jellyfin.mobile.presentation.player.PlayerIntent
import org.jellyfin.mobile.presentation.player.PlayerUiState
import org.jellyfin.mobile.presentation.player.SubtitleOption
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

private const val OLED_DIM_PAUSE_THRESHOLD_MS = 120_000L
private const val SUBTITLE_STEP_MS = 100L
private const val AUDIO_DELAY_STEP_MS = 50L

/**
 * Screen state enum for navigating inside the modular Actions Modal.
 */
private enum class ActionsModalScreen {
    Root,
    Subtitles,
    Audio,
    Speed,
    AspectRatio
}

/**
 * PlayerActionsModal provides an ultra-clean, unified bottom-sheet actions hub:
 * - Root level: Minimal vertical menu with clear Arabic titles and current status badges.
 * - Sub-levels: Dedicated, focused mini-windows tailored to the exact nature of each action.
 */
@Composable
fun PlayerActionsModal(
    uiState: PlayerUiState,
    isVisible: Boolean,
    isPlaying: Boolean,
    audioBands: List<Float> = emptyList(),
    isBitstreamPassthrough: Boolean,
    onDismiss: () -> Unit,
    onIntent: (PlayerIntent) -> Unit,
    onSetEqualizerBand: (bandIndex: Int, gainMb: Int) -> Unit = { _, _ -> },
    onApplyVocalBoost: () -> Unit = {},
    onSetBitstreamPassthrough: (Boolean) -> Unit,
    onSetAudioDelay: (Long) -> Unit,
    onLaunchExternalPlayer: () -> Unit = {},
    onEnterPiP: () -> Unit = {},
    onSetAspectRatio: (AspectRatioMode) -> Unit = {},
    onSetPlaybackSpeed: (Float) -> Unit = {},
    onPlayNextEpisode: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(ActionsModalScreen.Root) }

    // Reset to root menu whenever modal becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            currentScreen = ActionsModalScreen.Root
        }
    }

    var audioDelayMs by remember(isVisible) { mutableLongStateOf(0L) }
    var subtitleDelayMs by remember(uiState.subtitleOffsetMs) {
        mutableLongStateOf(uiState.subtitleOffsetMs)
    }

    // OLED burn-in protection: dim after 2 minutes of pause
    var oledDimActive by remember { mutableStateOf(false) }
    DisposableEffect(isPlaying) {
        val window = (context as? ComponentActivity)?.window
        var cancelProtection = false

        if (!isPlaying) {
            val startTime = SystemClock.elapsedRealtime()
            val checkIntervalMs = 15_000L

            fun scheduleDimCheck() {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                if (elapsed >= OLED_DIM_PAUSE_THRESHOLD_MS && !cancelProtection) {
                    oledDimActive = true
                    window?.let { win ->
                        val attrs = win.attributes
                        attrs.screenBrightness = 0.01f
                        win.attributes = attrs
                        win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
            scheduleDimCheck()
        } else {
            if (oledDimActive) {
                oledDimActive = false
                window?.let { win ->
                    val attrs = win.attributes
                    attrs.screenBrightness = uiState.brightness
                    win.attributes = attrs
                    win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        onDispose {
            cancelProtection = true
        }
    }

    if (!isVisible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(260)) + fadeIn(tween(180)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)) + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0x336366F1), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .navigationBarsPadding()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { /* consume taps inside sheet */ })
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Top drag indicator handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 38.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                        },
                        label = "modal_screens"
                    ) { screen ->
                        when (screen) {
                            ActionsModalScreen.Root -> RootActionsList(
                                uiState = uiState,
                                onOpenSubtitles = { currentScreen = ActionsModalScreen.Subtitles },
                                onOpenAudio = { currentScreen = ActionsModalScreen.Audio },
                                onOpenAspectRatio = { currentScreen = ActionsModalScreen.AspectRatio },
                                onOpenSpeed = { currentScreen = ActionsModalScreen.Speed },
                                onEnterPiP = {
                                    onEnterPiP()
                                    onDismiss()
                                },
                                onPlayNextEpisode = if (onPlayNextEpisode != null) {
                                    {
                                        onPlayNextEpisode()
                                        onDismiss()
                                    }
                                } else null,
                                onLaunchExternalPlayer = {
                                    onLaunchExternalPlayer()
                                    onDismiss()
                                },
                                onClose = onDismiss
                            )
                            ActionsModalScreen.Subtitles -> SubtitlesMiniSheet(
                                subtitles = uiState.availableSubtitles,
                                selectedSubtitle = uiState.selectedSubtitle,
                                currentOffsetMs = subtitleDelayMs,
                                onSelectSubtitle = {
                                    onIntent(PlayerIntent.SelectSubtitleTrack(it))
                                    currentScreen = ActionsModalScreen.Root
                                },
                                onAdjustOffset = { step ->
                                    val newOffset = subtitleDelayMs + step
                                    subtitleDelayMs = newOffset
                                    onIntent(PlayerIntent.AdjustSubtitleOffset(newOffset))
                                },
                                onBack = { currentScreen = ActionsModalScreen.Root },
                                onClose = onDismiss
                            )
                            ActionsModalScreen.Audio -> AudioMiniSheet(
                                audioTracks = uiState.availableAudioTracks,
                                selectedAudioTrack = uiState.selectedAudioTrack,
                                isBitstreamPassthrough = isBitstreamPassthrough,
                                audioDelayMs = audioDelayMs,
                                onSelectAudio = {
                                    onIntent(PlayerIntent.SelectAudioTrack(it))
                                    currentScreen = ActionsModalScreen.Root
                                },
                                onSetBitstreamPassthrough = onSetBitstreamPassthrough,
                                onAdjustAudioDelay = { step ->
                                    val newDelay = audioDelayMs + step
                                    audioDelayMs = newDelay
                                    onSetAudioDelay(newDelay)
                                },
                                onBack = { currentScreen = ActionsModalScreen.Root },
                                onClose = onDismiss
                            )
                            ActionsModalScreen.Speed -> SpeedMiniSheet(
                                currentSpeed = uiState.playbackSpeed,
                                onSelectSpeed = {
                                    onSetPlaybackSpeed(it)
                                    currentScreen = ActionsModalScreen.Root
                                },
                                onBack = { currentScreen = ActionsModalScreen.Root },
                                onClose = onDismiss
                            )
                            ActionsModalScreen.AspectRatio -> AspectRatioMiniSheet(
                                currentMode = uiState.aspectRatioMode,
                                onSelectMode = {
                                    onSetAspectRatio(it)
                                    currentScreen = ActionsModalScreen.Root
                                },
                                onBack = { currentScreen = ActionsModalScreen.Root },
                                onClose = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Root Actions List (Unified Minimal Entry Point)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RootActionsList(
    uiState: PlayerUiState,
    onOpenSubtitles: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenAspectRatio: () -> Unit,
    onOpenSpeed: () -> Unit,
    onEnterPiP: () -> Unit,
    onPlayNextEpisode: (() -> Unit)?,
    onLaunchExternalPlayer: () -> Unit,
    onClose: () -> Unit
) {
    Column {
        SheetHeader(title = "خيارات المشغل والإجراءات", onClose = onClose)

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitles
        ActionItemRow(
            icon = Icons.Rounded.Subtitles,
            title = "الترجمة",
            subtitleBadge = uiState.selectedSubtitle?.name ?: "إيقاف الترجمة",
            onClick = onOpenSubtitles
        )

        // Audio
        ActionItemRow(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            title = "مسار الصوت",
            subtitleBadge = uiState.selectedAudioTrack?.name ?: "تلقائي",
            onClick = onOpenAudio
        )

        // Aspect ratio
        val aspectLabel = when (uiState.aspectRatioMode) {
            AspectRatioMode.Fill -> "ملء الشاشة"
            AspectRatioMode.Stretch -> "تمديد"
            else -> "العرض الأصلي"
        }
        ActionItemRow(
            icon = Icons.Rounded.AspectRatio,
            title = "أبعاد العرض",
            subtitleBadge = aspectLabel,
            onClick = onOpenAspectRatio
        )

        // Speed
        ActionItemRow(
            icon = Icons.Rounded.Speed,
            title = "سرعة التشغيل",
            subtitleBadge = "${uiState.playbackSpeed}x",
            onClick = onOpenSpeed
        )

        // Next Episode (Series)
        if (onPlayNextEpisode != null) {
            ActionItemRow(
                icon = Icons.Rounded.SkipNext,
                title = "تشغيل الحلقة التالية",
                subtitleBadge = "الانتقال المباشر للحلقة القادمة",
                accentTint = JellyfinColors.AccentAmber,
                onClick = onPlayNextEpisode
            )
        }

        // Picture in Picture
        ActionItemRow(
            icon = Icons.Rounded.PictureInPictureAlt,
            title = "صورة داخل صورة (PiP)",
            subtitleBadge = "متابعة العرض في نافذة عائمة",
            onClick = onEnterPiP
        )

        // External Player
        ActionItemRow(
            icon = Icons.AutoMirrored.Rounded.OpenInNew,
            title = "المشاهدة في مشغل خارجي",
            subtitleBadge = "VLC, MX Player, Just Player...",
            onClick = onLaunchExternalPlayer
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Subtitles Mini Sheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SubtitlesMiniSheet(
    subtitles: ImmutableList<SubtitleOption>,
    selectedSubtitle: SubtitleOption?,
    currentOffsetMs: Long,
    onSelectSubtitle: (SubtitleOption?) -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Column {
        SheetHeader(title = "مسارات الترجمة", onBack = onBack, onClose = onClose)

        Spacer(modifier = Modifier.height(12.dp))

        // Subtitle Sync Stepper
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33000000))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "مزامنة توقيت الترجمة:",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onAdjustOffset(-SUBTITLE_STEP_MS) }
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
                        .clickable { onAdjustOffset(SUBTITLE_STEP_MS) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(text = "+100ms", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.height((((subtitles.size + 1).coerceAtMost(6)) * 48).dp)) {
            item {
                OptionSelectionItem(
                    title = "إيقاف الترجمة (بدون)",
                    isSelected = selectedSubtitle == null,
                    onClick = { onSelectSubtitle(null) }
                )
            }
            items(subtitles, key = { it.index }) { sub ->
                OptionSelectionItem(
                    title = sub.name,
                    isSelected = selectedSubtitle?.index == sub.index,
                    onClick = { onSelectSubtitle(sub) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Audio Mini Sheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AudioMiniSheet(
    audioTracks: ImmutableList<AudioOption>,
    selectedAudioTrack: AudioOption?,
    isBitstreamPassthrough: Boolean,
    audioDelayMs: Long,
    onSelectAudio: (AudioOption) -> Unit,
    onSetBitstreamPassthrough: (Boolean) -> Unit,
    onAdjustAudioDelay: (Long) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Column {
        SheetHeader(title = "المسار الصوتي وإخراج الصوت", onBack = onBack, onClose = onClose)

        Spacer(modifier = Modifier.height(12.dp))

        // Bitstream Passthrough Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33000000))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "تمرير الصوت المباشر (Bitstream)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "عبر منفذ HDMI / بصري (Dolby, DTS)",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Switch(
                checked = isBitstreamPassthrough,
                onCheckedChange = onSetBitstreamPassthrough,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = JellyfinColors.AccentAmber,
                    checkedTrackColor = JellyfinColors.AccentAmber.copy(alpha = 0.4f),
                    uncheckedThumbColor = Color(0xFF6B7280),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Audio Delay Stepper
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33000000))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "مزامنة الصوت:",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onAdjustAudioDelay(-AUDIO_DELAY_STEP_MS) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(text = "-50ms", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${audioDelayMs}ms",
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
                        .clickable { onAdjustAudioDelay(AUDIO_DELAY_STEP_MS) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(text = "+50ms", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.height((audioTracks.size.coerceAtMost(5) * 48).dp)) {
            items(audioTracks, key = { it.index }) { track ->
                OptionSelectionItem(
                    title = track.name,
                    isSelected = selectedAudioTrack?.index == track.index,
                    onClick = { onSelectAudio(track) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Playback Speed Mini Sheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpeedMiniSheet(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    Column {
        SheetHeader(title = "سرعة التشغيل", onBack = onBack, onClose = onClose)

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            speeds.forEach { speed ->
                OptionSelectionItem(
                    title = "${speed}x" + if (speed == 1.0f) " (السرعة الطبيعية)" else "",
                    isSelected = currentSpeed == speed,
                    onClick = { onSelectSpeed(speed) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Aspect Ratio Mini Sheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AspectRatioMiniSheet(
    currentMode: AspectRatioMode,
    onSelectMode: (AspectRatioMode) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val modes = listOf(
        AspectRatioMode.Fit to "العرض الأصلي (Fit)",
        AspectRatioMode.Fill to "ملء الشاشة (Fill)",
        AspectRatioMode.Stretch to "تمديد لتغطية الشاشة (Stretch)"
    )
    Column {
        SheetHeader(title = "أبعاد العرض ونسبة الشاشة", onBack = onBack, onClose = onClose)

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            modes.forEach { (mode, label) ->
                OptionSelectionItem(
                    title = label,
                    isSelected = currentMode == mode,
                    onClick = { onSelectMode(mode) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable UI Components for Sheets
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SheetHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF))
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "إغلاق",
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ActionItemRow(
    icon: ImageVector,
    title: String,
    subtitleBadge: String,
    accentTint: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitleBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun OptionSelectionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0x336366F1) else Color(0x11FFFFFF))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
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
