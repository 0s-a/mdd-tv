package org.jellyfin.mobile.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jellyfin.mobile.ui.theme.JellyfinColors

/**
 * Adaptive blur modifier providing 100% backward compatibility across all Android versions.
 *
 * On Android 12+ (API 31+): Utilizes hardware-accelerated RenderEffect Gaussian blur via [Modifier.blur].
 * On Android 7.0 - 11 (API 24 - 30): Seamlessly falls back to a sleek, zero-overhead carbon smoked tint
 * overlay to avoid software-rasterization frame drops and maintain a 60/120fps fluid experience.
 */
fun Modifier.adaptiveBlur(
    radius: Dp = 50.dp,
    fallbackTint: Color = JellyfinColors.PureBlack.copy(alpha = 0.75f)
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius)
    } else {
        this.background(fallbackTint)
    }
}
