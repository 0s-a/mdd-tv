package org.jellyfin.mobile.presentation.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.mobile.presentation.player.formatPlayerTime
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * PlayerGestureLayer provides standard-defining gestures inspired by MX Player & Infuse:
 * - Fluid vertical drag: system brightness (0% → 100%) and audio volume (0% → 200% with Boost)
 *   calibrated according to world-class gesture ergonomics (stable pointer pipeline, zero recomposition restarts).
 * - Horizontal drag anywhere: precise timeline scrubbing showing target time and delta `[+00:30]`.
 * - Double tap left/right: fast seek ±10s with animated glowing ripples.
 * - Single tap: toggle HUD with 3-second auto-dismiss.
 */
@Composable
fun PlayerGestureLayer(
    isControlsLocked: Boolean,
    isHudVisible: Boolean,
    isPlaying: Boolean,
    currentVolume: Float,
    currentBrightness: Float,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onFastSeekBy: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val viewConfig = LocalViewConfiguration.current
    val touchSlop = viewConfig.touchSlop

    // Stabilize callbacks and values to prevent pointerInput cancellation during drag
    val currentBrightnessState by rememberUpdatedState(currentBrightness)
    val currentVolumeState by rememberUpdatedState(currentVolume)
    val currentPositionMsState by rememberUpdatedState(currentPositionMs)
    val totalDurationMsState by rememberUpdatedState(totalDurationMs)
    val onBrightnessChangeState by rememberUpdatedState(onBrightnessChange)
    val onVolumeChangeState by rememberUpdatedState(onVolumeChange)
    val onFastSeekByState by rememberUpdatedState(onFastSeekBy)
    val onSeekToState by rememberUpdatedState(onSeekTo)
    val onToggleHudState by rememberUpdatedState(onToggleHud)

    var isDraggingBrightness by remember { mutableStateOf(false) }
    var isDraggingVolume by remember { mutableStateOf(false) }
    var isDraggingSeek by remember { mutableStateOf(false) }

    // Local drag values for instantaneous response without frame delay
    var activeBrightness by remember { mutableFloatStateOf(currentBrightness) }
    var activeVolume by remember { mutableFloatStateOf(currentVolume) }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }
    var initialPositionMs by remember { mutableLongStateOf(0L) }

    var showRewindRipple by remember { mutableStateOf(false) }
    var showForwardRipple by remember { mutableStateOf(false) }

    val rewindScale = remember { Animatable(0.7f) }
    val forwardScale = remember { Animatable(0.7f) }

    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    var lastTapX by remember { mutableFloatStateOf(0f) }
    var userInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 3-second HUD auto-dismiss timer
    LaunchedEffect(isHudVisible, isPlaying, isControlsLocked, userInteractionTime) {
        if (isHudVisible && isPlaying && !isControlsLocked) {
            delay(3000L)
            onToggleHudState()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Key ONLY on isControlsLocked so drag gestures never get interrupted or restarted by state updates
            .pointerInput(isControlsLocked) {
                if (isControlsLocked) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    val isLeftHalf = startX < (size.width / 2f)

                    var isDrag = false
                    var dragMode: Int = 0 // 0: uncommitted, 1: horizontal seek, 2: vertical brightness, 3: vertical volume

                    activeBrightness = currentBrightnessState
                    activeVolume = currentVolumeState
                    initialPositionMs = currentPositionMsState
                    seekDeltaMs = 0L

                    var lastHapticStep = -1

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            if (isDrag) {
                                if (dragMode == 1) {
                                    val targetMs = (initialPositionMs + seekDeltaMs).coerceIn(0L, totalDurationMsState)
                                    onSeekToState(targetMs)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                isDrag = false
                                dragMode = 0
                                isDraggingBrightness = false
                                isDraggingVolume = false
                                isDraggingSeek = false
                            } else {
                                // Tap evaluation
                                userInteractionTime = System.currentTimeMillis()
                                val now = System.currentTimeMillis()
                                val isDoubleTap = (now - lastTapTimeMs) < 320L && abs(startX - lastTapX) < 180f

                                if (isDoubleTap) {
                                    lastTapTimeMs = 0L
                                    if (startX < (size.width / 2f)) {
                                        // Double tap left: Rewind -10s
                                        onFastSeekByState(-10_000L)
                                        coroutineScope.launch {
                                            showRewindRipple = true
                                            rewindScale.snapTo(0.6f)
                                            rewindScale.animateTo(
                                                targetValue = 1.25f,
                                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                                            )
                                            showRewindRipple = false
                                        }
                                    } else {
                                        // Double tap right: Forward +10s
                                        onFastSeekByState(10_000L)
                                        coroutineScope.launch {
                                            showForwardRipple = true
                                            forwardScale.snapTo(0.6f)
                                            forwardScale.animateTo(
                                                targetValue = 1.25f,
                                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                                            )
                                            showForwardRipple = false
                                        }
                                    }
                                } else {
                                    lastTapTimeMs = now
                                    lastTapX = startX
                                    coroutineScope.launch {
                                        delay(320L)
                                        if (lastTapTimeMs == now) {
                                            onToggleHudState()
                                        }
                                    }
                                }
                            }
                            break
                        }

                        val totalDx = change.position.x - startX
                        val totalDy = change.position.y - startY

                        if (!isDrag) {
                            if (abs(totalDx) > touchSlop && abs(totalDx) > abs(totalDy) * 1.25f) {
                                isDrag = true
                                dragMode = 1
                                isDraggingSeek = true
                            } else if (abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx) * 1.25f) {
                                isDrag = true
                                if (isLeftHalf) {
                                    dragMode = 2
                                    isDraggingBrightness = true
                                    lastHapticStep = (activeBrightness * 10).toInt()
                                } else {
                                    dragMode = 3
                                    isDraggingVolume = true
                                    lastHapticStep = (activeVolume * 10).toInt()
                                }
                            }
                        }

                        if (isDrag) {
                            change.consume()
                            userInteractionTime = System.currentTimeMillis()

                            val deltaX = change.position.x - change.previousPosition.x
                            val deltaY = change.position.y - change.previousPosition.y

                            when (dragMode) {
                                1 -> {
                                    // Horizontal Scrubbing
                                    val scaleFactor = (totalDurationMsState.toFloat() / 3600_000f).coerceIn(1.0f, 2.5f)
                                    val scrubStep = (deltaX / size.width.toFloat()) * 90_000L * scaleFactor
                                    seekDeltaMs += scrubStep.toLong()
                                }
                                2 -> {
                                    // World-class smooth vertical brightness drag
                                    // Calibrated to 70% screen height for effortless thumb throw
                                    val strokeRange = (size.height * 0.70f).coerceAtLeast(300f)
                                    val deltaNormalized = -deltaY / strokeRange
                                    activeBrightness = (activeBrightness + deltaNormalized).coerceIn(0.01f, 1.0f)
                                    onBrightnessChangeState(activeBrightness)

                                    // Rhythmic tactile feedback every 10%
                                    val step = (activeBrightness * 10).toInt()
                                    if (step != lastHapticStep) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        lastHapticStep = step
                                    }
                                }
                                3 -> {
                                    // World-class smooth vertical volume drag (up to 200% with Boost)
                                    val strokeRange = (size.height * 0.70f).coerceAtLeast(300f)
                                    val deltaNormalized = -deltaY / strokeRange
                                    val prevVol = activeVolume
                                    activeVolume = (activeVolume + deltaNormalized * 2.0f).coerceIn(0.0f, 2.0f)
                                    onVolumeChangeState(activeVolume)

                                    // Tactile threshold for audio boost crossing
                                    if ((prevVol <= 1.0f && activeVolume > 1.0f) || (prevVol > 1.0f && activeVolume <= 1.0f)) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else {
                                        val step = (activeVolume * 10).toInt()
                                        if (step != lastHapticStep) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            lastHapticStep = step
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // ══════════════════════════════════════════════════════════════
        // 1. MX PLAYER HORIZONTAL SEEK HUD (Center Screen Overlay)
        // ══════════════════════════════════════════════════════════════
        AnimatedVisibility(
            visible = isDraggingSeek,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val targetMs = (initialPositionMs + seekDeltaMs).coerceIn(0L, totalDurationMs)
            val deltaSeconds = (seekDeltaMs / 1000L)
            val sign = if (deltaSeconds >= 0) "+" else ""
            val deltaFormatted = "$sign${deltaSeconds}s"

            Box(
                modifier = Modifier
                    .width(280.dp)
                    .glassmorphism(
                        backgroundColor = Color(0xDD111827),
                        borderColor = Color(0x446366F1),
                        cornerRadius = 20.dp
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
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
                            imageVector = if (seekDeltaMs >= 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                            contentDescription = null,
                            tint = if (seekDeltaMs >= 0) JellyfinColors.TechSky else Color(0xFFF97316),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = deltaFormatted,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${formatPlayerTime(targetMs)} / ${formatPlayerTime(totalDurationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val progress = if (totalDurationMs > 0) targetMs.toFloat() / totalDurationMs.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = JellyfinColors.PrimaryIndigo,
                        trackColor = Color(0x33FFFFFF)
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════════════
        // 2. MX PLAYER BRIGHTNESS INDICATOR (Left Edge Vertical Capsule)
        // ══════════════════════════════════════════════════════════════
        AnimatedVisibility(
            visible = isDraggingBrightness,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 36.dp)
        ) {
            MxVerticalIndicatorPill(
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = "السطوع",
                        tint = JellyfinColors.TechSky,
                        modifier = Modifier.size(24.dp)
                    )
                },
                percentage = (activeBrightness * 100f).roundToInt(),
                progress = activeBrightness,
                activeColor = JellyfinColors.TechSky,
                isBoost = false
            )
        }

        // ══════════════════════════════════════════════════════════════
        // 3. MX PLAYER VOLUME INDICATOR (Right Edge Vertical Capsule)
        // ══════════════════════════════════════════════════════════════
        AnimatedVisibility(
            visible = isDraggingVolume,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 36.dp)
        ) {
            val volumePercent = (activeVolume * 100f).roundToInt()
            val isBoost = activeVolume > 1.0f
            val volumeIcon = when {
                activeVolume <= 0.01f -> Icons.AutoMirrored.Rounded.VolumeMute
                activeVolume <= 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                else -> Icons.AutoMirrored.Rounded.VolumeUp
            }

            MxVerticalIndicatorPill(
                icon = {
                    Icon(
                        imageVector = if (isBoost) Icons.Rounded.Bolt else volumeIcon,
                        contentDescription = "الصوت",
                        tint = if (isBoost) Color(0xFFF97316) else JellyfinColors.PrimaryIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                },
                percentage = volumePercent,
                progress = (activeVolume / 2.0f).coerceIn(0f, 1f),
                activeColor = if (isBoost) Color(0xFFF97316) else JellyfinColors.PrimaryIndigo,
                isBoost = isBoost
            )
        }

        // ══════════════════════════════════════════════════════════════
        // 4. DOUBLE-TAP RIPPLES (-10s / +10s)
        // ══════════════════════════════════════════════════════════════
        if (showRewindRipple) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxSize(0.5f)
                    .align(Alignment.CenterStart)
                    .scale(rewindScale.value)
                    .alpha((1.25f - rewindScale.value).coerceIn(0f, 1f))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x886366F1), Color(0x33000000))
                            )
                        )
                        .border(1.5.dp, Color(0x666366F1), CircleShape)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.FastRewind,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-10s",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (showForwardRipple) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxSize(0.5f)
                    .align(Alignment.CenterEnd)
                    .scale(forwardScale.value)
                    .alpha((1.25f - forwardScale.value).coerceIn(0f, 1f))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x886366F1), Color(0x33000000))
                            )
                        )
                        .border(1.5.dp, Color(0x666366F1), CircleShape)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+10s",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern MX Player style vertical slider capsule with liquid-smooth interpolated height animation.
 */
@Composable
private fun MxVerticalIndicatorPill(
    icon: @Composable () -> Unit,
    percentage: Int,
    progress: Float,
    activeColor: Color,
    isBoost: Boolean,
    modifier: Modifier = Modifier
) {
    // Liquid smooth progress interpolation (60ms linear glide) eliminates digitizer jitter
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        label = "pill_progress_smooth"
    )

    Box(
        modifier = modifier
            .width(58.dp)
            .height(210.dp)
            .glassmorphism(
                backgroundColor = Color(0xDD0F172A),
                borderColor = if (isBoost) Color(0x88F97316) else JellyfinColors.BorderGlass,
                cornerRadius = 29.dp
            )
            .padding(vertical = 14.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            icon()

            // Vertical custom track with liquid fill
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isBoost) {
                                Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFF97316)))
                            } else {
                                Brush.verticalGradient(listOf(activeColor, activeColor.copy(alpha = 0.7f)))
                            }
                        )
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isBoost) Color(0xFFF97316) else Color.White
                )
                if (isBoost) {
                    Text(
                        text = "BOOST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        color = Color(0xFFF97316)
                    )
                }
            }
        }
    }
}