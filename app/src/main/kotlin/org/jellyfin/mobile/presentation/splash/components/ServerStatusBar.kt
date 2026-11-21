package org.jellyfin.mobile.presentation.splash.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.mobile.presentation.splash.ServerStatus
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives

@Composable
fun ServerStatusBar(
    status: ServerStatus,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "server_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val (serverTitle, latencyText, dotColor, progressColors) = when (status) {
        is ServerStatus.Connected -> {
            Quad(
                "سيرفر الاستراحة",
                "${status.latencyMs}ms متصل",
                JellyfinColors.SuccessEmerald,
                listOf(JellyfinColors.AccentCyan, JellyfinColors.SuccessEmerald)
            )
        }
        is ServerStatus.Checking -> {
            Quad(
                "سيرفر الاستراحة",
                "جاري الفحص...",
                JellyfinColors.AccentCyan,
                listOf(JellyfinColors.AccentCyan.copy(alpha = 0.6f), JellyfinPrimitives.Graphite800)
            )
        }
        is ServerStatus.Offline -> {
            Quad(
                "سيرفر الاستراحة",
                "غير متصل",
                JellyfinColors.RatingGold,
                listOf(JellyfinColors.RatingGold, JellyfinColors.StatusAlert)
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(230.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .alpha(if (status is ServerStatus.Checking) pulseAlpha else 1.0f)
                )

                Text(
                    text = serverTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = JellyfinPrimitives.Gray100,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            }

            Text(
                text = latencyText,
                style = MaterialTheme.typography.labelSmall,
                color = if (status is ServerStatus.Connected) JellyfinColors.AccentCyan else dotColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Matte graphite track with subtle semantic gradient fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(JellyfinPrimitives.Graphite800)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (status is ServerStatus.Offline) 0.35f else 1.0f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(progressColors))
                    .alpha(if (status is ServerStatus.Connected) pulseAlpha else 1.0f)
            )
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
