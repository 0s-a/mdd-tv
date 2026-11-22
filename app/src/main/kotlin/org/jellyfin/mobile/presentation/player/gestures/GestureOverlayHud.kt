package org.jellyfin.mobile.presentation.player.gestures

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jellyfin.mobile.presentation.player.formatPlayerTime

// ---------------------------------------------------------------------------
// Design tokens
// ---------------------------------------------------------------------------

/** True black background for OLED zero-pixel power consumption. */
private val HUD_BACKGROUND = Color(0xFF000000)

/** Semi-transparent scrim over the black base for legibility. */
private val HUD_SCRIM = Color(0xCC000000)

/** Primary text and icon tint on HUD panels. */
private val HUD_FOREGROUND = Color(0xFFFFFFFF)

/** Accent colour for active progress fill (white at reduced alpha). */
private val HUD_ACCENT_FILL = Color(0xBBFFFFFF)

/** Unfilled track colour. */
private val HUD_TRACK_BACKGROUND = Color(0x44FFFFFF)

/** Orange/amber accent for volume boost indication (>100%). */
private val HUD_BOOST_ACCENT = Color(0xFFFFAA00)

/** Speed boost label colour. */
private val HUD_SPEED_ACCENT = Color(0xFF00CCFF)

/** HUD corner radius. */
private val HUD_CORNER_RADIUS = 12.dp

/** Padding inside each HUD pill/card. */
private val HUD_PADDING_HORIZONTAL = 20.dp
private val HUD_PADDING_VERTICAL = 14.dp

/** Progress bar width for side-panel indicators. */
private val PROGRESS_BAR_WIDTH = 4.dp

/** Progress bar height for side-panel indicators. */
private val PROGRESS_BAR_HEIGHT = 160.dp

/** Delay in milliseconds before the HUD auto-vanishes after the gesture ends. */
private const val HUD_VANISH_DELAY_MS = 380L

/** Duration of the fade-in animation (ms). */
private const val HUD_FADE_IN_MS = 100

/** Duration of the fade-out animation (ms). */
private const val HUD_FADE_OUT_MS = 280

// ---------------------------------------------------------------------------
// GestureOverlayHud: main orchestrator composable
// ---------------------------------------------------------------------------

/**
 * GestureOverlayHud renders ephemeral floating indicator overlays for active
 * player gestures.  Each overlay type appears and vanishes independently,
 * with auto-dismiss happening within [HUD_VANISH_DELAY_MS] ms after the
 * gesture input stops.
 *
 * OLED-first design:
 * - True black (#000000) backgrounds ensure zero pixel emission on OLED panels.
 * - Semi-transparent scrim used sparingly; primary surface remains pure black.
 * - Fade-out is fast (<400ms) to never obstruct the video content.
 *
 * @param state observable gesture state produced by [PlayerGestureDetector].
 * @param modifier layout modifier applied to the full-screen overlay Box.
 */
@Composable
fun GestureOverlayHud(
    state: PlayerGestureState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Seek time card: centred horizontally, vertically centred
        SeekTimeCard(state = state)

        // Brightness panel: left-centre edge
        BrightnessHud(
            state = state,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        // Volume panel: right-centre edge
        VolumeHud(
            state = state,
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        // Long-press speed boost banner: top-centre
        SpeedBoostHud(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

// ---------------------------------------------------------------------------
// Seek time card
// ---------------------------------------------------------------------------

/**
 * Displays the seek delta and estimated seek position while horizontal
 * scrubbing is active.  Vanishes within [HUD_VANISH_DELAY_MS] after seek ends.
 */
@Composable
private fun SeekTimeCard(state: PlayerGestureState) {
    var visible by remember { mutableStateOf(false) }
    var capturedDeltaMs by remember { mutableLongStateOf(0L) }
    var capturedPositionMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.isSeekActive, state.seekDeltaMs) {
        if (state.isSeekActive) {
            visible = true
            capturedDeltaMs = state.seekDeltaMs
            capturedPositionMs = state.seekPreviewPositionMs
        } else {
            delay(HUD_VANISH_DELAY_MS)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(HUD_FADE_IN_MS, easing = LinearEasing)),
        exit = fadeOut(tween(HUD_FADE_OUT_MS, easing = FastOutSlowInEasing)),
    ) {
        val sign = if (capturedDeltaMs >= 0) "+" else ""
        val deltaSeconds = capturedDeltaMs / 1000L
        val previewTime = formatPlayerTime(capturedPositionMs)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(HUD_CORNER_RADIUS))
                .background(HUD_SCRIM)
                .padding(horizontal = HUD_PADDING_HORIZONTAL, vertical = HUD_PADDING_VERTICAL),
        ) {
            Text(
                text = "$sign${deltaSeconds}s",
                color = HUD_FOREGROUND,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = previewTime,
                color = HUD_FOREGROUND.copy(alpha = 0.75f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Brightness HUD panel
// ---------------------------------------------------------------------------

/**
 * Left-side vertical progress panel showing current brightness level
 * (0.01 to 1.0).  Anchored to the centre-left of the player area.
 */
@Composable
private fun BrightnessHud(
    state: PlayerGestureState,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    var capturedLevel by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.isBrightnessActive, state.brightnessLevel) {
        if (state.isBrightnessActive) {
            visible = true
            capturedLevel = state.brightnessLevel
        } else {
            delay(HUD_VANISH_DELAY_MS)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(HUD_FADE_IN_MS, easing = LinearEasing)),
        exit = fadeOut(tween(HUD_FADE_OUT_MS, easing = FastOutSlowInEasing)),
        modifier = modifier.padding(start = 32.dp),
    ) {
        val fillFraction by animateFloatAsState(
            targetValue = capturedLevel.coerceIn(0.01f, 1.0f),
            animationSpec = tween(80),
            label = "brightness_fill",
        )
        val percent = (capturedLevel * 100f).toInt().coerceIn(1, 100)

        SideProgressPanel(
            icon = android.R.drawable.ic_menu_gallery,
            label = "$percent%",
            fillFraction = fillFraction,
            accent = HUD_ACCENT_FILL,
        )
    }
}

// ---------------------------------------------------------------------------
// Volume HUD panel
// ---------------------------------------------------------------------------

/**
 * Right-side vertical progress panel showing current volume level
 * (0.0 to 2.0).  When volume exceeds 100% (1.0), the accent colour shifts
 * to [HUD_BOOST_ACCENT] and a "BOOST" badge appears.
 */
@Composable
private fun VolumeHud(
    state: PlayerGestureState,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    var capturedLevel by remember { mutableFloatStateOf(0f) }
    var capturedBoost by remember { mutableStateOf(false) }

    LaunchedEffect(state.isVolumeActive, state.volumeLevel, state.isVolumeBoostActive) {
        if (state.isVolumeActive) {
            visible = true
            capturedLevel = state.volumeLevel
            capturedBoost = state.isVolumeBoostActive
        } else {
            delay(HUD_VANISH_DELAY_MS)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(HUD_FADE_IN_MS, easing = LinearEasing)),
        exit = fadeOut(tween(HUD_FADE_OUT_MS, easing = FastOutSlowInEasing)),
        modifier = modifier.padding(end = 32.dp),
    ) {
        // Normalize 0.0–2.0 range into 0.0–1.0 fill fraction
        val fillFraction by animateFloatAsState(
            targetValue = (capturedLevel / 2.0f).coerceIn(0f, 1f),
            animationSpec = tween(80),
            label = "volume_fill",
        )
        val percent = (capturedLevel * 100f).toInt().coerceIn(0, 200)
        val accent = if (capturedBoost) HUD_BOOST_ACCENT else HUD_ACCENT_FILL
        val label = if (capturedBoost) "$percent% BOOST" else "$percent%"

        SideProgressPanel(
            icon = android.R.drawable.ic_lock_silent_mode_off,
            label = label,
            fillFraction = fillFraction,
            accent = accent,
        )
    }
}

// ---------------------------------------------------------------------------
// Speed boost HUD banner
// ---------------------------------------------------------------------------

/**
 * Top-centre banner displayed while a long-press is active.
 * Shows "2.0x" in an accent colour to signal fast-forward mode.
 */
@Composable
private fun SpeedBoostHud(
    state: PlayerGestureState,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLongPressActive) {
        if (state.isLongPressActive) {
            visible = true
        } else {
            delay(HUD_VANISH_DELAY_MS)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(HUD_FADE_IN_MS, easing = LinearEasing)),
        exit = fadeOut(tween(HUD_FADE_OUT_MS, easing = FastOutSlowInEasing)),
        modifier = modifier.padding(top = 48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(HUD_CORNER_RADIUS))
                .background(HUD_SCRIM)
                .padding(horizontal = HUD_PADDING_HORIZONTAL, vertical = HUD_PADDING_VERTICAL),
        ) {
            Text(
                text = "2.0x",
                color = HUD_SPEED_ACCENT,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FAST",
                color = HUD_FOREGROUND.copy(alpha = 0.75f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared side progress panel component
// ---------------------------------------------------------------------------

/**
 * Reusable vertical indicator pill used for both brightness and volume displays.
 * Contains:
 * - A system icon at the top.
 * - A vertical progress track filling bottom-to-top.
 * - A percentage label below the track.
 *
 * @param icon Android resource id for the icon drawable.
 * @param label text shown below the track (e.g., "75%" or "150% BOOST").
 * @param fillFraction progress fraction [0.0, 1.0] driving the bar fill.
 * @param accent fill colour for the active portion of the track.
 */
@Composable
private fun SideProgressPanel(
    icon: Int,
    label: String,
    fillFraction: Float,
    accent: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(HUD_CORNER_RADIUS))
            .background(HUD_SCRIM)
            .padding(horizontal = 14.dp, vertical = 18.dp),
    ) {
        // Icon placeholder using a simple coloured circle since we cannot depend
        // on specific icon resources being available. Host screens may replace
        // this with their own icon composable via slot API if needed.
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(HUD_FOREGROUND.copy(alpha = 0.8f)),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Vertical progress track
        Box(
            modifier = Modifier
                .width(PROGRESS_BAR_WIDTH)
                .height(PROGRESS_BAR_HEIGHT)
                .clip(RoundedCornerShape(2.dp))
                .background(HUD_TRACK_BACKGROUND),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(PROGRESS_BAR_WIDTH)
                    .fillMaxHeight(fillFraction)
                    .background(accent),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = label,
            color = HUD_FOREGROUND,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
