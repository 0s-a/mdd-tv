package org.jellyfin.mobile.presentation.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Represents a single chapter marker extracted from the media container (MKV/MP4).
 *
 * @param positionMs Chapter start position in milliseconds.
 * @param title      Human-readable chapter title from the container metadata.
 */
@Stable
data class ChapterMarker(
    val positionMs: Long,
    val title: String,
)

/**
 * CinemaScrubber
 *
 * A precision video progress scrubber rendered entirely in Jetpack Compose.
 *
 * Features:
 *   - Thin white chapter divider tick marks drawn at each ChapterMarker boundary.
 *   - Dynamic chapter title displayed above the timeline whenever the user is actively scrubbing.
 *   - Non-blocking seek: position updates are reported through [onSeek] without stalling
 *     the ExoPlayer render thread. Actual player seek is deferred until [onSeekFinished].
 *
 * @param positionMs       Current playback position in milliseconds.
 * @param durationMs       Total media duration in milliseconds (must be > 0).
 * @param chapters         List of chapter markers sorted by positionMs ascending.
 * @param onSeek           Called continuously during drag with the target position in ms.
 * @param onSeekFinished   Called once when the user lifts their finger with the final target ms.
 * @param modifier         Compose modifier applied to the outer container.
 * @param trackHeight      Thickness of the progress bar track.
 * @param thumbRadius      Radius of the circular drag handle.
 */
@Composable
fun CinemaScrubber(
    positionMs: Long,
    durationMs: Long,
    chapters: List<ChapterMarker>,
    onSeek: (Long) -> Unit,
    onSeekFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 3.dp,
    thumbRadius: Dp = 6.dp,
) {
    if (durationMs <= 0L) return

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    // The displayed fraction: live scrub position while dragging, else actual playback fraction.
    val displayFraction by remember(positionMs, durationMs) {
        derivedStateOf {
            if (isScrubbing) scrubFraction
            else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
    }

    // Active chapter derived from the current display position.
    val activeChapter by remember(chapters) {
        derivedStateOf {
            if (chapters.isEmpty()) null
            else {
                val targetMs = (displayFraction * durationMs).toLong()
                chapters.lastOrNull { it.positionMs <= targetMs }
            }
        }
    }

    Box(modifier = modifier) {
        // Chapter name shown above the scrubber while user is dragging.
        AnimatedVisibility(
            visible = isScrubbing && activeChapter != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(bottom = 4.dp),
        ) {
            Text(
                text = activeChapter?.title.orEmpty(),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(thumbRadius * 2 + trackHeight + 8.dp),
        ) {
            val trackWidthPx = constraints.maxWidth.toFloat()

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(thumbRadius * 2 + trackHeight + 8.dp)
                    .pointerInput(durationMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isScrubbing = true
                                scrubFraction = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                                onSeek((scrubFraction * durationMs).toLong())
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                scrubFraction = (scrubFraction + dragAmount / trackWidthPx).coerceIn(0f, 1f)
                                onSeek((scrubFraction * durationMs).toLong())
                            },
                            onDragEnd = {
                                val finalMs = (scrubFraction * durationMs).toLong()
                                onSeekFinished(finalMs)
                                isScrubbing = false
                            },
                            onDragCancel = {
                                isScrubbing = false
                            },
                        )
                    },
            ) {
                val centerY = size.height / 2f
                val trackLeft = 0f
                val trackRight = size.width
                val trackHeightPx = trackHeight.toPx()
                val thumbRadiusPx = thumbRadius.toPx()

                // Background track.
                drawLine(
                    color = TRACK_BACKGROUND_COLOR,
                    start = Offset(trackLeft, centerY),
                    end = Offset(trackRight, centerY),
                    strokeWidth = trackHeightPx,
                    cap = StrokeCap.Round,
                )

                // Elapsed (filled) portion of the track.
                val progressX = trackLeft + (trackRight - trackLeft) * displayFraction
                drawLine(
                    color = TRACK_PROGRESS_COLOR,
                    start = Offset(trackLeft, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = trackHeightPx,
                    cap = StrokeCap.Round,
                )

                // Chapter tick marks.
                for (chapter in chapters) {
                    if (chapter.positionMs <= 0L || chapter.positionMs >= durationMs) continue
                    val tickX = trackLeft + (trackRight - trackLeft) * (chapter.positionMs.toFloat() / durationMs.toFloat())
                    drawLine(
                        color = CHAPTER_TICK_COLOR,
                        start = Offset(tickX, centerY - CHAPTER_TICK_HALF_HEIGHT_PX),
                        end = Offset(tickX, centerY + CHAPTER_TICK_HALF_HEIGHT_PX),
                        strokeWidth = CHAPTER_TICK_WIDTH_PX,
                        cap = StrokeCap.Square,
                    )
                }

                // Drag thumb.
                drawCircle(
                    color = THUMB_COLOR,
                    radius = thumbRadiusPx,
                    center = Offset(progressX, centerY),
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------
// Private constants
// -------------------------------------------------------------------------------------------

private val TRACK_BACKGROUND_COLOR = Color(0x4DFFFFFF)   // 30% white
private val TRACK_PROGRESS_COLOR = Color.White
private val THUMB_COLOR = Color.White
private val CHAPTER_TICK_COLOR = Color(0xB3FFFFFF)        // 70% white - subtle but visible

/** Tick mark dimensions in raw pixels (density-independent values scaled at draw time). */
private const val CHAPTER_TICK_WIDTH_PX = 1.5f
private const val CHAPTER_TICK_HALF_HEIGHT_PX = 5f
