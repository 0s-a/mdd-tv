package org.jellyfin.mobile.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.AppConfig
import org.jellyfin.mobile.presentation.core.components.AmbientGlow
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.LocalOledTrueBlack

/**
 * SplashScreen represents the custom cinematic entry point for "استراحة الرصاص".
 * Features:
 * 1. Top Title: "استراحة الرصاص" with live server connectivity status pill.
 * 2. Center: "مدد TV" brand logo box with ambient glow.
 * 3. Bottom: Developer info: "المطور: حسام الرصاص" with fallback offline actions when disconnected.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOled = LocalOledTrueBlack.current
    val backgroundColor = if (isOled) JellyfinColors.PureBlack else JellyfinColors.Background

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                SplashEffect.NavigateToHome -> onNavigateToHome()
                SplashEffect.NavigateToOnboarding -> onNavigateToOnboarding()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. Ambient Glow behind the logo
        AmbientGlow(
            color = JellyfinColors.PrimaryIndigo.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.Center)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = JellyfinDimens.SpaceLarge, vertical = JellyfinDimens.SpaceMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ──────────────────────────────────────────
            // TOP SECTION: "استراحة الرصاص" + حالة الاتصال بالسيرفر
            // ──────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = JellyfinDimens.SpaceLarge)
            ) {
                Text(
                    text = "استراحة الرصاص",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        brush = Brush.horizontalGradient(
                            listOf(
                                JellyfinColors.TextPrimary,
                                JellyfinColors.TechSky,
                                JellyfinColors.TextPrimary
                            )
                        )
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                // Server Status Pill
                ServerStatusPill(status = uiState.serverStatus)
            }

            // ──────────────────────────────────────────
            // CENTER SECTION: شعار مدد TV
            // ──────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(112.dp)
                        .glassmorphism(
                            backgroundColor = JellyfinColors.SurfaceGlass,
                            borderColor = JellyfinColors.TechSky.copy(alpha = 0.6f),
                            cornerRadius = 28.dp
                        )
                        .border(1.5.dp, JellyfinColors.BorderGlass, RoundedCornerShape(28.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = AppConfig.APP_NAME,
                        tint = JellyfinColors.TechSky,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))

                Text(
                    text = AppConfig.APP_NAME,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = JellyfinColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "سينما منزلية فائقة السرعة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JellyfinColors.TextSecondary
                )
            }

            // ──────────────────────────────────────────
            // BOTTOM SECTION: معلومات المطور حسام الرصاص + أزرار الطوارئ
            // ──────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = JellyfinDimens.SpaceMedium)
            ) {
                // If offline: Show Retry & Offline local profile options
                AnimatedVisibility(
                    visible = uiState.serverStatus is ServerStatus.Offline,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = JellyfinDimens.SpaceMedium)
                    ) {
                        Button(
                            onClick = { viewModel.handleIntent(SplashIntent.Retry) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JellyfinColors.PrimaryIndigo,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "إعادة المحاولة", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceSmall))

                        OutlinedButton(
                            onClick = { viewModel.handleIntent(SplashIntent.ContinueOffline) },
                            shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = JellyfinColors.TechSky,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "المتابعة دون اتصال",
                                color = JellyfinColors.TechSky,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(JellyfinDimens.SpaceMedium))
                    }
                }

                // Developer Credits Pill
                DeveloperCreditsPill()
            }
        }
    }
}

/**
 * ServerStatusPill displays live server connection state:
 * - Checking: pulsing light with status text
 * - Connected: emerald green indicator with latency
 * - Offline: warning indicator with offline state
 */
@Composable
private fun ServerStatusPill(
    status: ServerStatus,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val (bgGlassColor, borderColor, statusText, dotColor) = when (status) {
        is ServerStatus.Checking -> {
            Tuple4(
                JellyfinColors.SurfaceGlass,
                JellyfinColors.BorderGlass,
                "جاري فحص الاتصال بالسيرفر...",
                JellyfinColors.TechSky
            )
        }
        is ServerStatus.Connected -> {
            Tuple4(
                JellyfinColors.SuccessEmerald.copy(alpha = 0.15f),
                JellyfinColors.SuccessEmerald.copy(alpha = 0.5f),
                "متصل بالسيرفر (${status.latencyMs}ms)",
                JellyfinColors.SuccessEmerald
            )
        }
        is ServerStatus.Offline -> {
            Tuple4(
                JellyfinColors.RatingGold.copy(alpha = 0.15f),
                JellyfinColors.RatingGold.copy(alpha = 0.5f),
                "غير متصل بالسيرفر",
                JellyfinColors.RatingGold
            )
        }
    }

    Box(
        modifier = modifier
            .glassmorphism(
                backgroundColor = bgGlassColor,
                borderColor = borderColor,
                cornerRadius = JellyfinDimens.RadiusPill
            )
            .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = JellyfinDimens.SpaceExtraSmall)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (status is ServerStatus.Checking) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .alpha(pulseAlpha)
                )
            } else if (status is ServerStatus.Offline) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = dotColor,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = JellyfinColors.TextPrimary
            )
        }
    }
}

/**
 * DeveloperCreditsPill displays developer info:
 * "المطور: حسام الرصاص" + "إصدار الاستراحة V2.0"
 */
@Composable
private fun DeveloperCreditsPill(
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceGlass,
                borderColor = JellyfinColors.BorderGlass,
                cornerRadius = JellyfinDimens.RadiusPill
            )
            .padding(horizontal = JellyfinDimens.SpaceMedium, vertical = JellyfinDimens.SpaceSmall)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(JellyfinDimens.SpaceSmall)
        ) {
            Icon(
                imageVector = Icons.Rounded.Code,
                contentDescription = null,
                tint = JellyfinColors.TechSky,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = "المطور: حسام الرصاص",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = JellyfinColors.TextPrimary
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(JellyfinColors.PrimaryIndigo.copy(alpha = 0.3f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "إصدار الاستراحة V2.0",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Medium,
                    color = JellyfinColors.TechSky
                )
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
