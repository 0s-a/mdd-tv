package org.jellyfin.mobile.presentation.player.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap

/**
 * ThumbnailPreviewBox
 *
 * A floating 16:9 thumbnail preview card that tracks the user's touch X position
 * horizontally above the scrubber timeline. Renders the bitmap frame returned by
 * [TrickplayThumbnailManager.getThumbnail] together with a formatted timestamp label.
 *
 * Layout specification:
 *   - Card width   : 160 dp
 *   - Card height  : 90 dp (16:9 aspect enforced by [aspectRatio])
 *   - Corner radius: 8 dp
 *   - Border       : 1 dp, Color(0xFF2A2A2A) (subtle dark)
 *   - Drop shadow  : 4 dp elevation
 *   - Background   : Color(0xFF0D0D0D) shown while thumbnail is loading
 *
 * @param positionMs     Target playback position the preview represents, in milliseconds.
 * @param thumbnail      Decoded [Bitmap] from TrickplayThumbnailManager, or null while loading.
 * @param touchX         Horizontal touch offset in pixels within the parent container.
 * @param containerWidth Width in pixels of the parent container (used for clamping).
 * @param bottomOffsetDp Vertical offset in dp from the bottom of the parent; positions the card
 *                       above the scrubber track.
 * @param modifier       Additional modifiers applied to the root Box.
 */
@Composable
fun ThumbnailPreviewBox(
    positionMs: Long,
    thumbnail: Bitmap?,
    touchX: Float,
    containerWidth: Float,
    bottomOffsetDp: Dp = 56.dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    val cardWidthDp = CARD_WIDTH_DP.dp
    val cardWidthPx = with(density) { cardWidthDp.toPx() }
    val halfCardPx = cardWidthPx / 2f

    // Clamp the card horizontally so it never overflows the parent container edges.
    val clampedX = touchX.coerceIn(halfCardPx, containerWidth - halfCardPx) - halfCardPx
    val xOffsetPx = clampedX.toInt()

    val bottomOffsetPx = with(density) { bottomOffsetDp.toPx() }.toInt()

    val formattedTimestamp = remember(positionMs) { formatTimestamp(positionMs) }
    val imageBitmap: ImageBitmap? = remember(thumbnail) { thumbnail?.asImageBitmap() }

    Box(
        modifier = modifier
            .offset { IntOffset(x = xOffsetPx, y = -bottomOffsetPx) }
            .width(cardWidthDp)
            .shadow(elevation = 4.dp, shape = CARD_SHAPE)
            .clip(CARD_SHAPE)
            .border(width = 1.dp, color = BORDER_COLOR, shape = CARD_SHAPE)
            .background(color = BACKGROUND_COLOR),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Thumbnail image area - enforces 16:9 aspect ratio.
            Box(
                modifier = Modifier
                    .width(cardWidthDp)
                    .aspectRatio(ASPECT_RATIO_16_9),
                contentAlignment = Alignment.Center,
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    // Placeholder color fill while the bitmap is being loaded from cache or network.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(PLACEHOLDER_COLOR),
                    )
                }
            }

            // Timestamp label below the image.
            Text(
                text = formattedTimestamp,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                ),
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }
    }
}

// -------------------------------------------------------------------------------------------
// Timestamp formatting
// -------------------------------------------------------------------------------------------

/**
 * Formats [positionMs] as HH:MM:SS when the duration includes hours, or MM:SS otherwise.
 * Uses a monospace font to prevent label width from jumping during scrubbing.
 *
 * Examples:
 *   4482000  ms -> "01:14:42"
 *   182000   ms -> "03:02"
 */
private fun formatTimestamp(positionMs: Long): String {
    val totalSeconds = positionMs / 1_000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

// -------------------------------------------------------------------------------------------
// Constants
// -------------------------------------------------------------------------------------------

/** Specification: card width in dp. Height is derived from 16:9 aspect ratio = 90 dp. */
private const val CARD_WIDTH_DP: Int = 160

/** 16:9 width-to-height ratio. */
private const val ASPECT_RATIO_16_9: Float = 16f / 9f

/** Corner radius per specification. */
private val CARD_SHAPE = RoundedCornerShape(size = 8.dp)

/** Subtle dark border so the card reads against both bright and dark video frames. */
private val BORDER_COLOR = Color(0xFF2A2A2A)

/** True-black background visible behind the thumbnail while loading; also suits OLED screens. */
private val BACKGROUND_COLOR = Color(0xFF0D0D0D)

/** Placeholder fill while thumbnail bitmap is being fetched asynchronously. */
private val PLACEHOLDER_COLOR = Color(0xFF1A1A1A)
