package org.jellyfin.mobile.presentation.player.components

import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.mobile.presentation.player.AudioOption
import org.jellyfin.mobile.presentation.player.PlayerIntent
import org.jellyfin.mobile.presentation.player.PlayerUiState
import org.jellyfin.mobile.presentation.player.SubtitleOption
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens

// ─────────────────────────────────────────────────────────────────────────────
// Auto-dim / OLED burn-in constants
// ─────────────────────────────────────────────────────────────────────────────

private const val OLED_DIM_PAUSE_THRESHOLD_MS = 120_000L  // 2 minutes of continuous pause
private const val CHROME_VANISH_IDLE_MS = 3_000L           // 3 seconds of inactivity
private const val SUBTITLE_STEP_MS = 100L                  // +/- 100ms per increment
private const val AUDIO_DELAY_STEP_MS = 50L                // +/- 50ms per increment

// ─────────────────────────────────────────────────────────────────────────────
// EQ band labels matching AudioBoostEngine band order
// ─────────────────────────────────────────────────────────────────────────────

private val EQ_BAND_LABELS = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

/**
 * Subtle launch button displayed in the player chrome that opens [PlayerActionsModal].
 *
 * Renders as a small matte icon button that blends with the HUD without cluttering it.
 * Vanishes with the rest of the chrome after [CHROME_VANISH_IDLE_MS] of inactivity.
 */
@Composable
fun PlayerActionsButton(
    isHudVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isHudVisible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = modifier
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(JellyfinDimens.MinTouchTarget)
                .semantics { contentDescription = "More actions" }
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * PlayerActionsModal is a quiet bottom-sheet style overlay providing advanced
 * playback controls without cluttering the main player HUD.
 *
 * Tabs:
 *   - Audio: Bitstream Passthrough toggle, 5-Band Equalizer with Vocal Booster preset,
 *            and Audio Delay sync slider (+/- 50ms increments).
 *   - Subtitles: Track selection list and Subtitle Delay sync slider (+/- 100ms increments).
 *
 * Additional features:
 *   - OLED burn-in protection: when playback has been paused for over 2 minutes,
 *     the host window brightness is reduced to near-zero and keepScreenOn is released.
 *   - Auto-vanishing chrome: the modal resets its own inactivity timer on every
 *     user interaction; after 3 seconds of silence it dismisses itself.
 *
 * @param uiState          Current player state snapshot.
 * @param isVisible        Whether the sheet is visible.
 * @param isPlaying        Whether playback is currently active (drives OLED dim logic).
 * @param audioBands       Current equalizer band gains in millibels per band (5 values, index 0-4).
 * @param isBitstreamPassthrough Whether HDMI/Optical bitstream passthrough is active.
 * @param onDismiss        Called when the sheet should be hidden.
 * @param onIntent         Dispatch a [PlayerIntent] to the ViewModel.
 * @param onSetEqualizerBand   Set a specific EQ band gain (bandIndex, gainMb in range -1500..1500).
 * @param onApplyVocalBoost    Apply the Vocal Booster preset via AudioBoostEngine.
 * @param onSetBitstreamPassthrough Toggle HDMI/Optical passthrough on/off.
 * @param onSetAudioDelay   Set audio delay in milliseconds (negative = audio leads).
 */
@Composable
fun PlayerActionsModal(
    uiState: PlayerUiState,
    isVisible: Boolean,
    isPlaying: Boolean,
    audioBands: List<Float>,
    isBitstreamPassthrough: Boolean,
    onDismiss: () -> Unit,
    onIntent: (PlayerIntent) -> Unit,
    onSetEqualizerBand: (bandIndex: Int, gainMb: Int) -> Unit,
    onApplyVocalBoost: () -> Unit,
    onSetBitstreamPassthrough: (Boolean) -> Unit,
    onSetAudioDelay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Tab state ──────────────────────────────────────────────────────────
    var selectedTab by remember { mutableIntStateOf(0) }

    // ── Audio delay local state (displayed in ms) ──────────────────────────
    var audioDelayMs by remember { mutableLongStateOf(0L) }

    // ── Subtitle delay mirrors the ViewModel offset for display ───────────
    var subtitleDelayMs by remember(uiState.subtitleOffsetMs) {
        mutableLongStateOf(uiState.subtitleOffsetMs)
    }

    // ── Auto-vanish inactivity timer ──────────────────────────────────────
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isVisible) {
        if (!isVisible) return@LaunchedEffect
        while (true) {
            delay(500L)
            val idle = System.currentTimeMillis() - lastInteractionMs
            if (idle >= CHROME_VANISH_IDLE_MS) {
                onDismiss()
                break
            }
        }
    }

    fun resetIdleTimer() {
        lastInteractionMs = System.currentTimeMillis()
    }

    // ── OLED burn-in protection ────────────────────────────────────────────
    // When paused, start tracking elapsed pause time. After 2 minutes,
    // dim the window brightness to 0.01f and release the keep-screen-on flag.
    var pauseStartMs by remember { mutableLongStateOf(0L) }
    var oledDimActive by remember { mutableStateOf(false) }

    DisposableEffect(isPlaying) {
        if (!isPlaying) {
            pauseStartMs = System.currentTimeMillis()
            val handler = Handler(Looper.getMainLooper())
            val dimRunnable = Runnable {
                val activity = (context as? android.app.Activity)
                    ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
                activity?.let { act ->
                    val lp = act.window.attributes
                    lp.screenBrightness = 0.01f
                    act.window.attributes = lp
                    act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                oledDimActive = true
            }
            handler.postDelayed(dimRunnable, OLED_DIM_PAUSE_THRESHOLD_MS)
            onDispose {
                handler.removeCallbacks(dimRunnable)
                if (oledDimActive) {
                    // Restore brightness and wake-lock when playback resumes
                    val activity = (context as? android.app.Activity)
                        ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
                    activity?.let { act ->
                        val lp = act.window.attributes
                        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        act.window.attributes = lp
                        act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    oledDimActive = false
                }
            }
        } else {
            if (oledDimActive) {
                val activity = (context as? android.app.Activity)
                    ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
                activity?.let { act ->
                    val lp = act.window.attributes
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    act.window.attributes = lp
                    act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                oledDimActive = false
            }
            onDispose {}
        }
    }

    // ── Sheet scrim + slide animation ─────────────────────────────────────
    if (!isVisible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(280)) +
                    fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)) +
                    fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ActionsSheetContent(
                uiState = uiState,
                selectedTab = selectedTab,
                audioBands = audioBands,
                isBitstreamPassthrough = isBitstreamPassthrough,
                audioDelayMs = audioDelayMs,
                subtitleDelayMs = subtitleDelayMs,
                onTabSelected = { tab ->
                    selectedTab = tab
                    resetIdleTimer()
                },
                onSelectSubtitle = { track ->
                    onIntent(PlayerIntent.SelectSubtitleTrack(track))
                    resetIdleTimer()
                },
                onSubtitleDelayStep = { step ->
                    val newOffset = subtitleDelayMs + step
                    subtitleDelayMs = newOffset
                    onIntent(PlayerIntent.AdjustSubtitleOffset(newOffset))
                    resetIdleTimer()
                },
                onSetEqualizerBand = { bandIndex, gainMb ->
                    onSetEqualizerBand(bandIndex, gainMb)
                    resetIdleTimer()
                },
                onApplyVocalBoost = {
                    onApplyVocalBoost()
                    resetIdleTimer()
                },
                onSetBitstreamPassthrough = { enabled ->
                    onSetBitstreamPassthrough(enabled)
                    resetIdleTimer()
                },
                onAudioDelayStep = { step ->
                    val newDelay = audioDelayMs + step
                    audioDelayMs = newDelay
                    onSetAudioDelay(newDelay)
                    resetIdleTimer()
                },
                onUserInteraction = { resetIdleTimer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        // Consume taps inside the sheet so they do not dismiss it
                        detectTapGestures(onTap = { resetIdleTimer() })
                    }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Inner sheet content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionsSheetContent(
    uiState: PlayerUiState,
    selectedTab: Int,
    audioBands: List<Float>,
    isBitstreamPassthrough: Boolean,
    audioDelayMs: Long,
    subtitleDelayMs: Long,
    onTabSelected: (Int) -> Unit,
    onSelectSubtitle: (SubtitleOption?) -> Unit,
    onSubtitleDelayStep: (Long) -> Unit,
    onSetEqualizerBand: (Int, Int) -> Unit,
    onApplyVocalBoost: () -> Unit,
    onSetBitstreamPassthrough: (Boolean) -> Unit,
    onAudioDelayStep: (Long) -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetBackground = Color(0xFF101216)
    val tabTitles = listOf("Audio", "Subtitles")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = JellyfinDimens.RadiusLarge, topEnd = JellyfinDimens.RadiusLarge))
            .background(sheetBackground)
            .navigationBarsPadding()
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
        )

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSm))

        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(2.dp)
                            .padding(horizontal = 24.dp)
                            .background(
                                color = JellyfinColors.AccentAmber,
                                shape = RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp)
                            )
                    )
                }
            },
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == index) Color.White else Color(0xFF6B7280)
                        )
                    }
                )
            }
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.06f),
            thickness = 1.dp
        )

        // Tab content
        when (selectedTab) {
            0 -> AudioTabContent(
                audioBands = audioBands,
                isBitstreamPassthrough = isBitstreamPassthrough,
                audioDelayMs = audioDelayMs,
                onSetEqualizerBand = onSetEqualizerBand,
                onApplyVocalBoost = onApplyVocalBoost,
                onSetBitstreamPassthrough = onSetBitstreamPassthrough,
                onAudioDelayStep = onAudioDelayStep,
                onUserInteraction = onUserInteraction,
                modifier = Modifier.fillMaxWidth()
            )
            1 -> SubtitleTabContent(
                subtitles = uiState.availableSubtitles,
                selectedSubtitle = uiState.selectedSubtitle,
                subtitleDelayMs = subtitleDelayMs,
                onSelectSubtitle = onSelectSubtitle,
                onSubtitleDelayStep = onSubtitleDelayStep,
                onUserInteraction = onUserInteraction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Audio tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AudioTabContent(
    audioBands: List<Float>,
    isBitstreamPassthrough: Boolean,
    audioDelayMs: Long,
    onSetEqualizerBand: (Int, Int) -> Unit,
    onApplyVocalBoost: () -> Unit,
    onSetBitstreamPassthrough: (Boolean) -> Unit,
    onAudioDelayStep: (Long) -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = JellyfinDimens.SpaceMd, vertical = JellyfinDimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSm)
    ) {
        // ── Bitstream Passthrough toggle ───────────────────────────────────
        SectionLabel(text = "Output")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(horizontal = JellyfinDimens.SpaceMd, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bitstream Passthrough",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = "HDMI / Optical (Dolby, DTS)",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )
            }
            Switch(
                checked = isBitstreamPassthrough,
                onCheckedChange = { checked ->
                    onSetBitstreamPassthrough(checked)
                    onUserInteraction()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = JellyfinColors.AccentAmber,
                    checkedTrackColor = JellyfinColors.AccentAmber.copy(alpha = 0.4f),
                    uncheckedThumbColor = Color(0xFF6B7280),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                ),
                modifier = Modifier.semantics { contentDescription = "Bitstream passthrough toggle" }
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

        // ── 5-Band Equalizer ───────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(text = "Equalizer")
            VocalBoostButton(
                enabled = !isBitstreamPassthrough,
                onClick = {
                    onApplyVocalBoost()
                    onUserInteraction()
                }
            )
        }

        if (isBitstreamPassthrough) {
            Text(
                text = "EQ is disabled in bitstream passthrough mode.",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(horizontal = JellyfinDimens.SpaceXS)
            )
        } else {
            EqualizerBands(
                bands = audioBands,
                onBandChanged = { bandIndex, gainMb ->
                    onSetEqualizerBand(bandIndex, gainMb)
                    onUserInteraction()
                }
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

        // ── Audio Delay ────────────────────────────────────────────────────
        SectionLabel(text = "Audio Sync")
        DelayStepControl(
            labelPrefix = "Audio Delay",
            valueMs = audioDelayMs,
            stepMs = AUDIO_DELAY_STEP_MS,
            onStep = { step ->
                onAudioDelayStep(step)
                onUserInteraction()
            }
        )

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceXS))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Subtitle tab
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SubtitleTabContent(
    subtitles: kotlinx.collections.immutable.ImmutableList<SubtitleOption>,
    selectedSubtitle: SubtitleOption?,
    subtitleDelayMs: Long,
    onSelectSubtitle: (SubtitleOption?) -> Unit,
    onSubtitleDelayStep: (Long) -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = JellyfinDimens.SpaceMd, vertical = JellyfinDimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceXS)
    ) {
        SectionLabel(text = "Track")

        // Disable / no subtitle option
        SubtitleTrackItem(
            title = "Off",
            isSelected = selectedSubtitle == null,
            onClick = {
                onSelectSubtitle(null)
                onUserInteraction()
            }
        )

        if (subtitles.isEmpty()) {
            Text(
                text = "No subtitle tracks found in this media.",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = JellyfinDimens.SpaceXS),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(modifier = Modifier.height((subtitles.size.coerceAtMost(5) * 52).dp)) {
                items(subtitles, key = { it.index }) { track ->
                    SubtitleTrackItem(
                        title = track.name,
                        isSelected = selectedSubtitle?.index == track.index,
                        onClick = {
                            onSelectSubtitle(track)
                            onUserInteraction()
                        }
                    )
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

        SectionLabel(text = "Subtitle Sync")
        DelayStepControl(
            labelPrefix = "Subtitle Delay",
            valueMs = subtitleDelayMs,
            stepMs = SUBTITLE_STEP_MS,
            onStep = { step ->
                onSubtitleDelayStep(step)
                onUserInteraction()
            }
        )

        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceXS))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Equalizer band strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EqualizerBands(
    bands: List<Float>,
    onBandChanged: (bandIndex: Int, gainMb: Int) -> Unit
) {
    // bands list may arrive with less than 5 entries if hardware EQ is unavailable
    val bandCount = bands.size.coerceAtMost(EQ_BAND_LABELS.size)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        for (i in 0 until bandCount) {
            val gainMb = bands.getOrElse(i) { 0f }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${if (gainMb >= 0) "+" else ""}${(gainMb / 100).toInt()}",
                    fontSize = 10.sp,
                    color = if (gainMb > 0) JellyfinColors.AccentAmber else Color(0xFF6B7280),
                    fontWeight = FontWeight.SemiBold
                )
                // Vertical slider for each EQ band.
                // Compose does not natively support vertical Slider without custom drawing;
                // we approximate with a horizontal Slider rotated 270 degrees and constrained.
                Slider(
                    value = gainMb,
                    onValueChange = { newGain ->
                        onBandChanged(i, newGain.toInt())
                    },
                    valueRange = -1500f..1500f,
                    steps = 29,  // 100 mB steps within -1500..1500
                    colors = SliderDefaults.colors(
                        thumbColor = JellyfinColors.AccentAmber,
                        activeTrackColor = JellyfinColors.AccentAmber,
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .width(80.dp)
                        .semantics { contentDescription = "EQ ${EQ_BAND_LABELS.getOrElse(i) { "" }}" }
                )
                Text(
                    text = EQ_BAND_LABELS.getOrElse(i) { "" },
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vocal boost one-tap button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VocalBoostButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(JellyfinDimens.RadiusMicro))
            .background(
                if (enabled) JellyfinColors.AccentAmber.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.04f)
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .semantics { contentDescription = "Apply Vocal Booster preset" }
    ) {
        Text(
            text = "Vocal Boost",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) JellyfinColors.AccentAmber else Color(0xFF4B5563)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Delay step control (+/- buttons with current value display)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DelayStepControl(
    labelPrefix: String,
    valueMs: Long,
    stepMs: Long,
    onStep: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = JellyfinDimens.SpaceMd, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decrement button
        StepButton(label = "-${stepMs}ms", onClick = { onStep(-stepMs) })

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatDelayMs(valueMs),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = labelPrefix,
                fontSize = 10.sp,
                color = Color(0xFF6B7280)
            )
        }

        // Increment button
        StepButton(label = "+${stepMs}ms", onClick = { onStep(+stepMs) })
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(JellyfinDimens.RadiusMicro))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = label }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

private fun formatDelayMs(ms: Long): String {
    return when {
        ms == 0L -> "0 ms"
        ms > 0L -> "+$ms ms"
        else -> "$ms ms"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Subtitle track selection row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SubtitleTrackItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(JellyfinDimens.RadiusSmall))
            .background(if (isSelected) Color(0x22D4A373) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = JellyfinDimens.SpaceMd, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF9CA3AF)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = JellyfinColors.AccentAmber,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = Color(0xFF6B7280),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}
