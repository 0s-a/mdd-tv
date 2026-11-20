package org.jellyfin.mobile.presentation.player.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

/** Touch slop distance (dp) before axis lock is evaluated. */
private const val TOUCH_SLOP_DP = 18f

/** Axis lock ratio: dominant axis must exceed the other by this factor. */
private const val AXIS_LOCK_RATIO = 1.5f

/** Outer edge margin (dp) reserved for Android system back gesture. */
private const val SYSTEM_INSET_MARGIN_DP = 24f

/** Seek window (seconds) in landscape orientation. */
private const val SEEK_WINDOW_LANDSCAPE_S = 90

/** Seek window (seconds) in portrait orientation. */
private const val SEEK_WINDOW_PORTRAIT_S = 45

/** Brightness sensitivity scalar applied to normalized vertical delta. */
private const val BRIGHTNESS_SENSITIVITY = 0.8f

/** Volume sensitivity scalar applied to normalized vertical delta. */
private const val VOLUME_SENSITIVITY = 0.8f

/** Volume range including software boost (0.0 = muted, 2.0 = 200% boost). */
private const val VOLUME_MAX_BOOST = 2.0f

/** Long press activation threshold in milliseconds. */
private const val LONG_PRESS_TIMEOUT_MS = 500L

/** Fast-forward/rewind amount for a single-side double tap (ms). */
private const val DOUBLE_TAP_SEEK_MS = 10_000L

/** Double tap detection window in milliseconds. */
private const val DOUBLE_TAP_WINDOW_MS = 300L

/** Speed multiplier applied during a long-press hold. */
private const val LONG_PRESS_SPEED = 2.0f

/** Maximum zoom scale factor. */
private const val ZOOM_MAX = 5.0f

/** Minimum zoom scale factor (fully zoomed out). */
private const val ZOOM_MIN = 1.0f

// ---------------------------------------------------------------------------
// Gesture axis lock enumeration
// ---------------------------------------------------------------------------

/**
 * Tracks which axis has been exclusively locked for the current drag gesture.
 * NONE means the touch slop has not yet been resolved.
 */
private enum class AxisLock { NONE, HORIZONTAL, VERTICAL }

// ---------------------------------------------------------------------------
// Gesture callbacks interface
// ---------------------------------------------------------------------------

/**
 * Callbacks delivered by [PlayerGestureDetector] to the hosting composable.
 * All callbacks are invoked on the composition thread.
 */
interface PlayerGestureCallbacks {

    /**
     * Seek delta requested.
     * @param deltaMs positive = forward, negative = backward.
     */
    fun onSeekDelta(deltaMs: Long)

    /**
     * Brightness change requested.
     * @param brightness target value clamped to [0.01, 1.0].
     */
    fun onBrightnessChange(brightness: Float)

    /**
     * Volume change requested.
     * @param volume target value clamped to [0.0, 2.0] (>1.0 = software boost).
     */
    fun onVolumeChange(volume: Float)

    /**
     * Long press started: apply 2x speed with pitch correction.
     */
    fun onLongPressStart()

    /**
     * Long press ended: restore normal playback speed.
     */
    fun onLongPressEnd()

    /**
     * Double tap on left third of screen: seek backward by 10 seconds.
     */
    fun onDoubleTapSeekBackward()

    /**
     * Double tap on right third of screen: seek forward by 10 seconds.
     */
    fun onDoubleTapSeekForward()

    /**
     * Double tap on centre third of screen: toggle play/pause.
     */
    fun onDoubleTapCenter()

    /**
     * Pinch zoom scale changed.
     * @param scale cumulative scale factor clamped to [1.0, 5.0].
     * @param panOffset cumulative two-finger pan offset in pixels.
     */
    fun onZoomChange(scale: Float, panOffset: Offset)

    /**
     * Touch lock state toggled.
     * @param locked true = controls locked, false = unlocked.
     */
    fun onTouchLockToggle(locked: Boolean)

    /**
     * Raw single tap (used internally to toggle HUD visibility).
     */
    fun onSingleTap()
}

// ---------------------------------------------------------------------------
// Gesture state holder (stable, survives recomposition)
// ---------------------------------------------------------------------------

/**
 * Mutable gesture state shared across the pointer input coroutine and
 * read by [GestureOverlayHud].  Uses primitive state holders to avoid
 * boxing and reduce garbage in hot drag paths.
 */
@Stable
class PlayerGestureState {

    // --- Seek HUD ---
    var isSeekActive by mutableStateOf(false)
    var seekDeltaMs by mutableLongStateOf(0L)
    var seekPreviewPositionMs by mutableLongStateOf(0L)

    // --- Brightness HUD ---
    var isBrightnessActive by mutableStateOf(false)
    var brightnessLevel by mutableFloatStateOf(0.5f)

    // --- Volume HUD ---
    var isVolumeActive by mutableStateOf(false)
    var volumeLevel by mutableFloatStateOf(1.0f)
    var isVolumeBoostActive by mutableStateOf(false)

    // --- Long press / speed boost HUD ---
    var isLongPressActive by mutableStateOf(false)

    // --- Zoom ---
    var zoomScale by mutableFloatStateOf(1.0f)
    var zoomPanOffset by mutableStateOf(Offset.Zero)

    // --- Touch lock ---
    var isTouchLocked by mutableStateOf(false)

    // --- Double tap feedback region ---
    var doubleTapRegion by mutableIntStateOf(0)       // -1 = left, 0 = none, 1 = right, 2 = center
    var doubleTapTriggerCount by mutableIntStateOf(0)  // incremented to trigger animations
}

// ---------------------------------------------------------------------------
// Modifier extension: attach gesture detector
// ---------------------------------------------------------------------------

/**
 * Attaches a high-performance, zero-allocation gesture detector to any composable.
 *
 * Design decisions:
 * - Axis lock is resolved once per gesture (after [TOUCH_SLOP_DP] travel) and
 *   never reconsidered until the finger is lifted, preventing jitter.
 * - [SYSTEM_INSET_MARGIN_DP] on both left/right edges is excluded from vertical
 *   gesture recognition to avoid interference with Android edge-back gestures.
 * - All drag arithmetic uses primitive Floats accumulated in-place with no
 *   intermediate object allocation, keeping inner loops GC-free.
 * - Long press is handled by a coroutine launched alongside the main gesture
 *   consumer; it cancels automatically if the finger moves enough before
 *   the timeout expires.
 * - Pinch zoom runs in a separate pointer-input scope so single-touch and
 *   multi-touch paths are fully decoupled.
 *
 * @param state observable gesture state updated during active gestures.
 * @param callbacks event sink for the host player screen.
 * @param isLandscape true when the device orientation is landscape.
 * @param containerWidthPx active layout width in pixels (from BoxWithConstraints).
 * @param containerHeightPx active layout height in pixels.
 * @param currentPositionMs current playback position for seek preview calculation.
 * @param currentBrightness current screen brightness [0.01, 1.0].
 * @param currentVolume current volume [0.0, 2.0].
 */
fun Modifier.playerGestureDetector(
    state: PlayerGestureState,
    callbacks: PlayerGestureCallbacks,
    isLandscape: Boolean,
    containerWidthPx: Float,
    containerHeightPx: Float,
    currentPositionMs: Long,
    currentBrightness: Float,
    currentVolume: Float,
    density: Float,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
): Modifier = this
    .pointerInput(isLandscape, containerWidthPx, containerHeightPx) {
        detectPlayerGestures(
            state = state,
            callbacks = callbacks,
            isLandscape = isLandscape,
            containerWidthPx = containerWidthPx,
            containerHeightPx = containerHeightPx,
            currentPositionMs = currentPositionMs,
            currentBrightness = currentBrightness,
            currentVolume = currentVolume,
            density = density,
            coroutineScope = coroutineScope,
        )
    }

// ---------------------------------------------------------------------------
// Core gesture detection logic
// ---------------------------------------------------------------------------

private suspend fun PointerInputScope.detectPlayerGestures(
    state: PlayerGestureState,
    callbacks: PlayerGestureCallbacks,
    isLandscape: Boolean,
    containerWidthPx: Float,
    containerHeightPx: Float,
    currentPositionMs: Long,
    currentBrightness: Float,
    currentVolume: Float,
    density: Float,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
) {
    val touchSlopPx = TOUCH_SLOP_DP * density
    val systemInsetPx = SYSTEM_INSET_MARGIN_DP * density
    val seekWindowMs = (if (isLandscape) SEEK_WINDOW_LANDSCAPE_S else SEEK_WINDOW_PORTRAIT_S) * 1000L

    // Track last tap for double-tap detection (use primitive Long to avoid boxing)
    var lastTapTimeMs: Long = 0L
    var lastTapX: Float = 0f

    // Accumulated gesture state (primitives, zero allocation in hot path)
    var accumulatedBrightness: Float = currentBrightness
    var accumulatedVolume: Float = currentVolume

    awaitEachGesture {
        // ---- Wait for initial down contact ----
        val firstDown: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
        val downTime: Long = System.currentTimeMillis()
        val downX: Float = firstDown.position.x
        val downY: Float = firstDown.position.y

        // ---- Double-tap detection ----
        val timeSinceLastTap: Long = downTime - lastTapTimeMs
        if (timeSinceLastTap < DOUBLE_TAP_WINDOW_MS && !state.isTouchLocked) {
            handleDoubleTap(downX, containerWidthPx, state, callbacks)
            lastTapTimeMs = 0L
            lastTapX = 0f
            consumeAllPointerEvents()
            return@awaitEachGesture
        }

        // ---- Touch lock: only double-tap on centre unlocks ----
        if (state.isTouchLocked) {
            // Consume all events silently while locked
            consumeAllPointerEvents()
            return@awaitEachGesture
        }

        // ---- Check if inside system inset margins ----
        val inSystemInset: Boolean = downX < systemInsetPx || downX > containerWidthPx - systemInsetPx

        // ---- Start long-press timer coroutine ----
        var longPressTriggered = false
        var axisLock: AxisLock = AxisLock.NONE
        var totalDeltaX: Float = 0f
        var totalDeltaY: Float = 0f
        var slopCleared: Boolean = false
        var seekAccumulatedMs: Long = 0L

        val longPressJob = coroutineScope?.launch {
            kotlinx.coroutines.delay(LONG_PRESS_TIMEOUT_MS)
            if (axisLock == AxisLock.NONE && !slopCleared) {
                longPressTriggered = true
                state.isLongPressActive = true
                callbacks.onLongPressStart()
            }
        }

            // ---- Drain move events ----
            var eventPointers: Int = 1
            try {
                while (true) {
                    val event: PointerEvent = awaitPointerEvent(pass = PointerEventPass.Main)
                    eventPointers = event.changes.count { it.pressed }

                    if (eventPointers == 0) {
                        // All fingers lifted
                        break
                    }

                    if (eventPointers > 1) {
                        // Multi-touch: cancel single-finger logic, let pinch scope handle it
                        longPressJob?.cancel()
                        break
                    }

                    val change: PointerInputChange = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break

                    val dx: Float = change.positionChange().x
                    val dy: Float = change.positionChange().y

                    totalDeltaX += dx
                    totalDeltaY += dy

                    val absTotalX: Float = abs(totalDeltaX)
                    val absTotalY: Float = abs(totalDeltaY)
                    val totalTravel: Float = absTotalX + absTotalY

                    // Cancel long press if finger moved enough
                    if (totalTravel > touchSlopPx * 0.5f && !longPressTriggered) {
                        longPressJob?.cancel()
                    }

                    // Resolve axis lock once slop is cleared
                    if (!slopCleared && totalTravel >= touchSlopPx) {
                        slopCleared = true
                        axisLock = when {
                            absTotalX > absTotalY * AXIS_LOCK_RATIO -> AxisLock.HORIZONTAL
                            absTotalY > absTotalX * AXIS_LOCK_RATIO -> AxisLock.VERTICAL
                            else -> AxisLock.NONE
                        }
                    }

                    if (!slopCleared || longPressTriggered) {
                        change.consume()
                        continue
                    }

                    when (axisLock) {
                        AxisLock.HORIZONTAL -> {
                            // Seek: normalize dx against container width
                            val normalizedDx: Float = dx / containerWidthPx
                            val seekDeltaMs: Long = (normalizedDx * seekWindowMs).toLong()
                            seekAccumulatedMs += seekDeltaMs
                            state.isSeekActive = true
                            state.seekDeltaMs = seekAccumulatedMs
                            state.seekPreviewPositionMs = (currentPositionMs + seekAccumulatedMs)
                                .coerceAtLeast(0L)
                            callbacks.onSeekDelta(seekDeltaMs)
                            change.consume()
                        }

                        AxisLock.VERTICAL -> {
                            if (inSystemInset) {
                                // Inside system inset margin: ignore vertical swipes
                                change.consume()
                                continue
                            }
                            // Normalize dy against container height (inverted: swipe up = increase)
                            val normalizedDy: Float = -dy / containerHeightPx
                            val midX: Float = containerWidthPx / 2f

                            if (downX < midX) {
                                // Left side: brightness control
                                accumulatedBrightness = (accumulatedBrightness + normalizedDy * BRIGHTNESS_SENSITIVITY)
                                    .coerceIn(0.01f, 1.0f)
                                state.isBrightnessActive = true
                                state.brightnessLevel = accumulatedBrightness
                                callbacks.onBrightnessChange(accumulatedBrightness)
                            } else {
                                // Right side: volume control (with boost)
                                accumulatedVolume = (accumulatedVolume + normalizedDy * VOLUME_SENSITIVITY)
                                    .coerceIn(0f, VOLUME_MAX_BOOST)
                                state.isVolumeActive = true
                                state.volumeLevel = accumulatedVolume
                                state.isVolumeBoostActive = accumulatedVolume > 1.0f
                                callbacks.onVolumeChange(accumulatedVolume)
                            }
                            change.consume()
                        }

                        AxisLock.NONE -> {
                            // Axis not yet resolved: consume silently
                            change.consume()
                        }
                    }
                }
            } finally {
                longPressJob?.cancel()
            }

            // ---- Gesture end cleanup ----
            if (longPressTriggered) {
                state.isLongPressActive = false
                callbacks.onLongPressEnd()
            }

            if (state.isSeekActive) {
                state.isSeekActive = false
                state.seekDeltaMs = 0L
            }

            if (state.isBrightnessActive) {
                state.isBrightnessActive = false
            }

            if (state.isVolumeActive) {
                state.isVolumeActive = false
            }

            // ---- Single tap: record for double-tap tracking and toggle HUD ----
            if (!slopCleared && !longPressTriggered && eventPointers == 0) {
                lastTapTimeMs = downTime
                lastTapX = downX
                callbacks.onSingleTap()
            }
        }
    }

// ---------------------------------------------------------------------------
// Double-tap handler
// ---------------------------------------------------------------------------

private fun handleDoubleTap(
    tapX: Float,
    containerWidthPx: Float,
    state: PlayerGestureState,
    callbacks: PlayerGestureCallbacks,
) {
    val thirdWidth: Float = containerWidthPx / 3f
    when {
        tapX < thirdWidth -> {
            state.doubleTapRegion = -1
            state.doubleTapTriggerCount++
            callbacks.onDoubleTapSeekBackward()
        }
        tapX > containerWidthPx - thirdWidth -> {
            state.doubleTapRegion = 1
            state.doubleTapTriggerCount++
            callbacks.onDoubleTapSeekForward()
        }
        else -> {
            state.doubleTapRegion = 2
            state.doubleTapTriggerCount++
            callbacks.onDoubleTapCenter()
        }
    }
}

// ---------------------------------------------------------------------------
// Pinch-to-zoom modifier
// ---------------------------------------------------------------------------

/**
 * Attaches a dedicated two-finger pinch-to-zoom and pan gesture handler.
 * Runs in a separate [pointerInput] scope from [playerGestureDetector] so
 * single-touch and multi-touch paths are fully independent.
 *
 * @param state shared gesture state updated with cumulative zoom/pan values.
 * @param callbacks event sink receiving [PlayerGestureCallbacks.onZoomChange].
 */
fun Modifier.playerPinchZoomDetector(
    state: PlayerGestureState,
    callbacks: PlayerGestureCallbacks,
): Modifier = this.pointerInput(Unit) {
    detectPinchZoom(state, callbacks)
}

private suspend fun PointerInputScope.detectPinchZoom(
    state: PlayerGestureState,
    callbacks: PlayerGestureCallbacks,
) {
    var cumulativeScale: Float = state.zoomScale
    var cumulativePan: Offset = state.zoomPanOffset

    awaitEachGesture {
        // Wait for two simultaneous contacts
        awaitFirstDown(requireUnconsumed = false)

        var eventPointers: Int
        do {
            val event: PointerEvent = awaitPointerEvent(pass = PointerEventPass.Main)
            eventPointers = event.changes.count { it.pressed }
            if (eventPointers < 2) break

            val zoomFactor: Float = event.calculateZoom()
            val panDelta: Offset = event.calculatePan()

            cumulativeScale = (cumulativeScale * zoomFactor).coerceIn(ZOOM_MIN, ZOOM_MAX)
            cumulativePan = cumulativePan + panDelta

            state.zoomScale = cumulativeScale
            state.zoomPanOffset = cumulativePan

            callbacks.onZoomChange(cumulativeScale, cumulativePan)

            event.changes.forEach { it.consume() }
        } while (eventPointers >= 2)
    }
}

// ---------------------------------------------------------------------------
// Utility: consume all remaining events in current gesture
// ---------------------------------------------------------------------------

private suspend fun AwaitPointerEventScope.consumeAllPointerEvents() {
    while (true) {
        val event: PointerEvent = awaitPointerEvent(pass = PointerEventPass.Main)
        event.changes.forEach { it.consume() }
        if (event.changes.none { it.pressed }) break
    }
}

// ---------------------------------------------------------------------------
// Remember helper for state creation
// ---------------------------------------------------------------------------

/**
 * Creates and remembers a [PlayerGestureState] instance stable across recompositions.
 */
@Composable
fun rememberPlayerGestureState(): PlayerGestureState = remember { PlayerGestureState() }
