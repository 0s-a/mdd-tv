package org.jellyfin.mobile.presentation.core.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.mobile.ui.theme.JellyfinColors

/**
 * Remembers and extracts the dominant vibrant color from an image URL using AndroidX Palette.
 * Smoothly animates color changes using [animateColorAsState] over 600ms (120Hz smooth).
 * Falls back to [defaultColor] (JellyfinColors.PrimaryIndigo) if unavailable.
 */
@Composable
fun rememberDominantColor(
    imageUrl: String?,
    defaultColor: Color = JellyfinColors.PrimaryIndigo
): State<Color> {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(defaultColor) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            dominantColor = defaultColor
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(128) // Lightweight thumbnail size for rapid palette extraction
                    .allowHardware(false) // Must be non-hardware bitmap for Palette
                    .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        ?: result.drawable.toBitmap()
                    val palette = Palette.from(bitmap).generate()
                    val rgb = palette.dominantSwatch?.rgb
                        ?: palette.vibrantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb

                    if (rgb != null) {
                        withContext(Dispatchers.Main) {
                            dominantColor = Color(rgb)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dominantColor = defaultColor
                }
            }
        }
    }

    return animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 600),
        label = "AmbientGlowColorAnimation"
    )
}

/**
 * AmbientGlow renders a cinematic radial gradient scrim at the top of the screen.
 * Strictly adheres to docs/DESIGN_SYSTEM.md and 01_HOME_DASHBOARD.md.
 *
 * @param color The dominant ambient color to glow (animated).
 * @param modifier External modifier.
 * @param alpha The opacity of the glow at its center (default 25%).
 * @param height Height of the ambient scrim canvas.
 */
@Composable
fun AmbientGlow(
    color: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 0.25f,
    height: Dp = 380.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val centerOffset = Offset(x = size.width / 2f, y = 0f)
        val glowRadius = size.height * 1.1f

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = alpha),
                    color.copy(alpha = alpha * 0.4f),
                    Color.Transparent
                ),
                center = centerOffset,
                radius = glowRadius
            )
        )
    }
}
