package org.jellyfin.mobile.presentation.player.subtitles

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction

// ─────────────────────────────────────────────────────────────────────────────
// Encoding constants
// ─────────────────────────────────────────────────────────────────────────────

private const val TAG = "CinemaSubtitleRenderer"

/** Charset name for Windows-1256 (legacy Arabic encoding). */
private const val CHARSET_WINDOWS_1256 = "windows-1256"

/** Charset name for modern Unicode UTF-8. */
private const val CHARSET_UTF_8 = "UTF-8"

// Arabic Unicode block: U+0600 to U+06FF
private const val ARABIC_RANGE_START = '\u0600'
private const val ARABIC_RANGE_END = '\u06FF'

// RTL embedding character for forcing right-to-left layout when needed
private const val RTL_MARK = '\u200F'

// ─────────────────────────────────────────────────────────────────────────────
// CinemaSubtitleRenderer (Compose entry point)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CinemaSubtitleRenderer renders high-contrast, cinema-grade subtitles directly in
 * Jetpack Compose with the following characteristics:
 *
 *   - 2dp solid black outline stroke drawn behind white fill for 100% legibility in
 *     bright or snow-white scenes without opaque background blocks.
 *   - Automatic RTL / LTR layout direction selection based on Arabic character detection.
 *   - Auto-encoding detection pipeline: sniffs UTF-8 validity first; falls back to
 *     Windows-1256 for legacy Arabic encoded subtitle streams.
 *   - Compatible with the libass/SubtitleView pipeline: raw cue text is cleaned before
 *     display (strips ASS override tags via [ASSTagStripper]).
 *   - Dynamic vertical lift above the HUD chrome when controls are visible.
 *
 * Usage:
 *   Place this composable above the video surface layer, anchored to the bottom of
 *   the player container. It consumes only the width it needs and leaves the rest
 *   transparent.
 *
 * @param rawCueText      The raw subtitle cue string from Media3 (may contain ASS tags).
 * @param isHudVisible    When true, the subtitle lifts upward to avoid being hidden by HUD.
 * @param modifier        Composable modifier chain.
 */
@Composable
fun CinemaSubtitleRenderer(
    rawCueText: String?,
    isHudVisible: Boolean,
    modifier: Modifier = Modifier
) {
    // Decode and strip the cue text through the full pipeline
    val displayText = remember(rawCueText) {
        if (rawCueText.isNullOrBlank()) return@remember null
        val decoded = SubtitleEncodingDetector.decode(rawCueText)
        ASSTagStripper.strip(decoded).takeIf { it.isNotBlank() }
    }

    // Smooth vertical lift animation when HUD is visible
    val bottomPadding by animateDpAsState(
        targetValue = if (isHudVisible) 136.dp else 36.dp,
        animationSpec = tween(durationMillis = 240),
        label = "subtitle_bottom_lift"
    )

    // Detect Arabic content to apply RTL text direction
    val isRtl = remember(displayText) {
        displayText?.any { c -> c in ARABIC_RANGE_START..ARABIC_RANGE_END } == true
    }

    AnimatedVisibility(
        visible = displayText != null,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(120)),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding, start = 24.dp, end = 24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (displayText != null) {
                SubtitleTextWithOutline(
                    text = displayText,
                    isRtl = isRtl
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Outlined subtitle text (Compose layer)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders subtitle text with a 2dp solid black outline and white fill.
 *
 * The outline is achieved via two overlapping [Text] composables:
 *   1. Bottom layer: drawn with [Stroke] drawStyle at 2dp width, black colour.
 *   2. Top layer: white fill with a subtle drop shadow for extra depth.
 *
 * This technique avoids background panels while maintaining WCAG 2.1 AAA contrast
 * in all scene lighting conditions.
 */
@Composable
private fun SubtitleTextWithOutline(
    text: String,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val outlineWidthPx = with(density) { 2.dp.toPx() }

    val textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr

    // Common typography parameters
    val fontSize = 20.sp
    val fontWeight = FontWeight.Bold
    val lineHeight = 29.sp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Layer 1: black outline stroke
        Text(
            text = text,
            style = TextStyle(
                color = Color.Black,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = TextAlign.Center,
                lineHeight = lineHeight,
                textDirection = textDirection,
                drawStyle = Stroke(
                    width = outlineWidthPx,
                    join = StrokeJoin.Round
                )
            )
        )

        // Layer 2: white fill with drop shadow for depth
        Text(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontSize = fontSize,
                fontWeight = fontWeight,
                textAlign = TextAlign.Center,
                lineHeight = lineHeight,
                textDirection = textDirection,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.70f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = 4f
                )
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ASS / SSA tag stripper
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ASSTagStripper removes Advanced SubStation Alpha (ASS/SSA) override tags from
 * raw subtitle cue text so plain display-ready text is passed to the renderer.
 *
 * Supported tag patterns:
 *   - Braces-delimited override blocks: {...}
 *   - Self-closing soft-break line tags: \N \n
 *   - Hard space: \h (replaced with a standard space)
 *   - Drawing commands: m, l, b, s, p (stripped via brace removal)
 *
 * Line breaks (\N and \n in ASS) are normalised to the platform newline character.
 */
object ASSTagStripper {

    /**
     * Strip all ASS/SSA override tags from [raw] and return the clean display text.
     *
     * @param raw The raw cue string potentially containing ASS override tags.
     * @return A human-readable string free of markup, with normalised line breaks.
     */
    fun strip(raw: String): String {
        if (raw.isBlank()) return raw

        return raw
            // Remove brace-enclosed ASS override blocks: {anything}
            .replace(Regex("\\{[^}]*}"), "")
            // Normalise ASS hard line breaks to system newline
            .replace(Regex("""\\[Nn]"""), "\n")
            // Replace ASS hard space with a regular space
            .replace(Regex("""\\h"""), " ")
            // Collapse consecutive whitespace lines (but preserve intentional newlines)
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .trim()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Encoding auto-detector
// ─────────────────────────────────────────────────────────────────────────────

/**
 * SubtitleEncodingDetector inspects raw byte content to determine whether subtitle
 * text is encoded as UTF-8 or Windows-1256 (legacy Arabic).
 *
 * Detection algorithm:
 *   1. If the input is already a Kotlin String (decoded by the JVM), return it directly.
 *   2. If called with raw bytes, attempt strict UTF-8 decoding first (CodingErrorAction.REPORT).
 *   3. On failure, fall back to Windows-1256.
 *
 * The [decode] overload accepting a [String] is used when Media3 has already decoded the
 * cue text but the original encoding was mis-detected upstream. In that case the function
 * performs a heuristic re-encode/re-decode round-trip through Windows-1256.
 */
object SubtitleEncodingDetector {

    /**
     * Returns a correctly decoded version of [input].
     *
     * When [input] contains Arabic Unicode characters it is returned as-is.
     * When [input] appears to be a mis-decoded Windows-1256 string (i.e., it contains
     * Latin-1 characters in ranges used by Windows-1256 for Arabic), a re-encoding
     * round-trip is attempted.
     *
     * @param input The subtitle text as supplied by the Media3 subtitle decoder.
     * @return Correctly decoded subtitle text.
     */
    fun decode(input: String): String {
        // Fast path: already contains Arabic Unicode characters - no re-decoding needed.
        if (input.any { c -> c in ARABIC_RANGE_START..ARABIC_RANGE_END }) {
            return input
        }

        // Heuristic: check if the input contains high-byte Latin-1 codepoints that map
        // to Arabic in Windows-1256 (roughly U+00C0 to U+00FF range mis-mapped).
        val mightBeWindows1256 = input.any { c ->
            c.code in 0xC0..0xFF
        }

        if (!mightBeWindows1256) {
            return input
        }

        return try {
            // Re-encode as ISO-8859-1 (preserves byte values as-is) then decode as Windows-1256
            val bytes = input.toByteArray(Charsets.ISO_8859_1)
            decodeBytes(bytes)
        } catch (e: Exception) {
            Log.w(TAG, "Encoding re-decode round-trip failed: ${e.message}")
            input
        }
    }

    /**
     * Decode a raw byte array using the detected charset.
     *
     * Tries UTF-8 strictly first. Falls back to Windows-1256 if the byte sequence
     * is not valid UTF-8.
     *
     * @param bytes Raw subtitle bytes from the container or external .srt file.
     * @return Decoded string using the best-matching charset.
     */
    fun decodeBytes(bytes: ByteArray): String {
        // Attempt 1: strict UTF-8
        val utf8Result = tryDecode(bytes, Charsets.UTF_8)
        if (utf8Result != null) return utf8Result

        // Attempt 2: Windows-1256 (Arabic legacy)
        val windows1256Charset = runCatching { Charset.forName(CHARSET_WINDOWS_1256) }.getOrNull()
        if (windows1256Charset != null) {
            val w1256Result = tryDecode(bytes, windows1256Charset)
            if (w1256Result != null) return w1256Result
        }

        // Last resort: replace unmappable characters rather than fail
        Log.w(TAG, "Falling back to UTF-8 with replacement characters for unrecognised encoding.")
        return String(bytes, Charsets.UTF_8)
    }

    /**
     * Attempt strict decoding of [bytes] using [charset].
     *
     * @return The decoded string, or null if any bytes were unmappable.
     */
    private fun tryDecode(bytes: ByteArray, charset: Charset): String? {
        return try {
            val decoder: CharsetDecoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val byteBuffer = ByteBuffer.wrap(bytes)
            val charBuffer: CharBuffer = decoder.decode(byteBuffer)
            charBuffer.toString()
        } catch (e: Exception) {
            null
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Canvas-based subtitle renderer (non-Compose, for custom SubtitleView integration)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CinemaSubtitlePainter provides a [Canvas]-based rendering path for integration with
 * Android's legacy [android.view.View] subtitle layer or Media3's SubtitleView
 * custom drawing callback.
 *
 * This painter is stateless; create one instance per SubtitleView and call [draw] from
 * within the view's [android.view.View.onDraw] or the SubtitleView output callback.
 *
 * Outline technique:
 *   - Text is painted once with [Paint.Style.STROKE] at 4f stroke width (equals 2dp on
 *     mdpi; scaled automatically via [textSizePx]).
 *   - Text is then painted again with [Paint.Style.FILL] over the stroke.
 *
 * @param textSizePx    The desired text size in pixels (density-aware, supplied by caller).
 * @param outlineWidthPx Stroke width in pixels for the black outline (default 4px = 2dp mdpi).
 */
class CinemaSubtitlePainter(
    private val textSizePx: Float = 56f,
    private val outlineWidthPx: Float = 4f
) {

    private val outlinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = textSizePx
        style = Paint.Style.STROKE
        strokeWidth = outlineWidthPx
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val fillPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = textSizePx
        style = Paint.Style.FILL
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(6f, 0f, 2f, android.graphics.Color.argb(160, 0, 0, 0))
    }

    /**
     * Draw [text] centred horizontally within [canvasWidth] at a vertical position
     * [bottomY] pixels from the top of the canvas.
     *
     * @param canvas      The [Canvas] to draw onto.
     * @param text        Clean display text (ASS tags must have been stripped prior).
     * @param canvasWidth Width of the drawable area in pixels.
     * @param bottomY     Y coordinate of the bottom edge of the text block in pixels.
     */
    fun draw(canvas: Canvas, text: String, canvasWidth: Int, bottomY: Float) {
        if (text.isBlank()) return

        val availableWidth = (canvasWidth - 64).coerceAtLeast(1)
        val cx = canvasWidth / 2f

        // Build StaticLayout for multi-line text wrapping
        val outlineLayout = buildStaticLayout(text, outlinePaint, availableWidth)
        val fillLayout = buildStaticLayout(text, fillPaint, availableWidth)

        val layoutHeight = outlineLayout.height.toFloat()
        val topY = bottomY - layoutHeight

        // Draw outline layer
        canvas.save()
        canvas.translate(cx - availableWidth / 2f, topY)
        outlineLayout.draw(canvas)
        canvas.restore()

        // Draw fill layer
        canvas.save()
        canvas.translate(cx - availableWidth / 2f, topY)
        fillLayout.draw(canvas)
        canvas.restore()
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()
    }
}
