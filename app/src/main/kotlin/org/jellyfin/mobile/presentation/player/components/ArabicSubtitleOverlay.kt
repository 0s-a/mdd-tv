package org.jellyfin.mobile.presentation.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.mobile.ui.theme.JellyfinDimens

/**
 * ArabicSubtitleOverlay provides cinema-grade Arabic subtitle rendering:
 * - High-contrast styling with sharp black shadow and soft translucent backing.
 * - Dynamic height lift: smoothly elevates above the bottom HUD when visible.
 * - Direct visual legibility complying with WCAG 2.1 AAA.
 */
@Composable
fun ArabicSubtitleOverlay(
    subtitleText: String?,
    isHudVisible: Boolean,
    subtitleOffsetMs: Long,
    modifier: Modifier = Modifier
) {
    val bottomPadding by animateDpAsState(
        targetValue = if (isHudVisible) 140.dp else 40.dp,
        animationSpec = tween(250),
        label = "subtitle_lift"
    )

    AnimatedVisibility(
        visible = !subtitleText.isNullOrBlank(),
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150)),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding, start = 32.dp, end = 32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xB3000000))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = subtitleText.orEmpty(),
                    style = TextStyle(
                        color = Color(0xFFFFFFFF),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp,
                        shadow = Shadow(
                            color = Color(0xFF000000),
                            offset = Offset(0f, 2f),
                            blurRadius = 6f
                        )
                    )
                )
            }
        }
    }
}