package org.jellyfin.mobile.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.R
import org.jellyfin.mobile.presentation.core.components.AmbientGlow
import org.jellyfin.mobile.presentation.splash.components.CharacterSideArt
import org.jellyfin.mobile.presentation.splash.components.PosterMontageLayer
import org.jellyfin.mobile.presentation.splash.components.ServerStatusBar
import org.jellyfin.mobile.presentation.splash.model.SplashPosterCatalog
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import org.jellyfin.mobile.ui.theme.JellyfinPrimitives

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
            .background(JellyfinColors.PureBlack)
    ) {
        when (uiState.montagePhase) {
            MontagePhase.RUNNING -> {
                PosterMontageLayer(
                    posters = SplashPosterCatalog,
                    onMontageComplete = {
                        viewModel.handleIntent(SplashIntent.MontageFinished)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            MontagePhase.IMPLODING -> {
                val scale by animateFloatAsState(
                    targetValue = 0.08f,
                    animationSpec = tween(durationMillis = 400, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
                    label = "implode_scale"
                )
                val alpha by animateFloatAsState(
                    targetValue = 0.0f,
                    animationSpec = tween(durationMillis = 350),
                    label = "implode_alpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .alpha(alpha)
                        .background(JellyfinColors.PureBlack)
                )
            }

            MontagePhase.SETTLED -> {
                SettledLandingScreen(
                    uiState = uiState,
                    onRetry = { viewModel.handleIntent(SplashIntent.Retry) },
                    onContinueOffline = { viewModel.handleIntent(SplashIntent.ContinueOffline) }
                )
            }
        }
    }
}

@Composable
private fun SettledLandingScreen(
    uiState: SplashUiState,
    onRetry: () -> Unit,
    onContinueOffline: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Refined Ambient Glow (Single Tone, Matte Cinema Palette, 14% Opacity)
        AmbientGlow(
            color = JellyfinColors.AccentCyan.copy(alpha = 0.14f),
            modifier = Modifier.align(Alignment.Center)
        )

        // 2. Character Side-Art Layers (Left and Right)
        CharacterSideArt(
            posters = SplashPosterCatalog,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Anamorphic Laser Flare Sweep
        val laserProgress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            laserProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))
            )
        }
        if (laserProgress.value in 0.02f..0.98f) {
            val laserAlpha = (1f - kotlin.math.abs(laserProgress.value - 0.5f) * 2f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.Center)
                    .alpha(laserAlpha)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                JellyfinColors.AccentCyan.copy(alpha = 0.8f),
                                JellyfinPrimitives.WhitePure,
                                JellyfinColors.AccentCyan.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // 4. Main Structured Content (Top, Center, Bottom)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Real App Icon alone in the top corner (without frame, larger)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF080B10)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Center Section: Primary Network Title + Subtitle + Server Status Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                TypewriterHeadline(
                    text = "استراحة الرصاص",
                    delayPerCharMs = 60L
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "سينما منزلية فائقة السرعة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JellyfinPrimitives.Gray400,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.5.dp)
                        .background(JellyfinColors.AccentCyan.copy(alpha = 0.35f), RoundedCornerShape(1.dp))
                )

                Spacer(modifier = Modifier.height(20.dp))

                ServerStatusBar(status = uiState.serverStatus)
            }

            // Bottom Section: Developer Info + Offline Actions when disconnected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                AnimatedVisibility(
                    visible = uiState.serverStatus is ServerStatus.Offline,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JellyfinColors.PrimaryIndigo,
                                contentColor = JellyfinPrimitives.BlackOled
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

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onContinueOffline,
                            shape = RoundedCornerShape(JellyfinDimens.RadiusMedium),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = JellyfinColors.AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "المتابعة دون اتصال",
                                color = JellyfinColors.AccentCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Simple Developer Text without Glass Cards
                Text(
                    text = "المطور: حسام الرصاص",
                    color = JellyfinPrimitives.Gray400,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "إصدار الاستراحة V2.0",
                    color = JellyfinPrimitives.Gray500,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TypewriterHeadline(
    text: String,
    delayPerCharMs: Long = 55L,
    modifier: Modifier = Modifier
) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        displayedText = ""
        text.forEach { char ->
            displayedText += char
            delay(delayPerCharMs)
        }
    }

    Text(
        text = displayedText,
        style = MaterialTheme.typography.headlineLarge.copy(
            brush = Brush.horizontalGradient(
                listOf(
                    JellyfinPrimitives.WhitePure,
                    JellyfinPrimitives.Ivory50,
                    JellyfinPrimitives.WhitePure
                )
            ),
            fontSize = 34.sp
        ),
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}
