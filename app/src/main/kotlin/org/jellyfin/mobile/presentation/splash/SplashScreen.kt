package org.jellyfin.mobile.presentation.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import org.jellyfin.mobile.R
import org.jellyfin.mobile.presentation.core.components.AmbientGlow
import org.jellyfin.mobile.presentation.splash.components.CinematicBackdropLayer
import org.jellyfin.mobile.presentation.splash.components.PosterMontageLayer
import org.jellyfin.mobile.presentation.splash.components.ServerStatusBar
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
        val isMontageRunning = uiState.montagePhase == MontagePhase.RUNNING

        AnimatedContent(
            targetState = isMontageRunning,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                    )
                ) togetherWith
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = 350,
                            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                        )
                    )
            },
            label = "splash_screen_phase_transition",
            modifier = Modifier.fillMaxSize()
        ) { isRunning ->
            if (isRunning) {
                PosterMontageLayer(
                    posters = uiState.posters,
                    onMontageComplete = {
                        viewModel.handleIntent(SplashIntent.MontageFinished)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
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
        // 1. Refined Ambient Glow (Single Tone, Matte Cinema Palette)
        AmbientGlow(
            color = JellyfinColors.AccentCyan.copy(alpha = 0.12f),
            modifier = Modifier.align(Alignment.Center)
        )

        // 2. Cinematic Ambient Artwork Backdrop
        CinematicBackdropLayer(
            posters = uiState.posters,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Structured Content: Unified Centered Brand Core + Bottom Info
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Unified Brand Core: Capsule Icon + Headline + Subtitle + Status Bar
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Brand Capsule Icon in Obsidian Container
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    JellyfinPrimitives.Graphite800,
                                    JellyfinPrimitives.Obsidian950
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                CinematicHeadline(text = "استراحة الرصاص")

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "سينما منزلية فائقة السرعة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JellyfinPrimitives.Gray400,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(JellyfinColors.AccentCyan.copy(alpha = 0.35f))
                )

                Spacer(modifier = Modifier.height(22.dp))

                ServerStatusBar(status = uiState.serverStatus)
            }

            // Bottom Section: Developer Info + Offline Actions when disconnected
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
private fun CinematicHeadline(
    text: String,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(18f) }

    LaunchedEffect(text) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        )
    }

    LaunchedEffect(text) {
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 800,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        )
    }

    Text(
        text = text,
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
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            this.translationY = offsetY.value
        }
    )
}
