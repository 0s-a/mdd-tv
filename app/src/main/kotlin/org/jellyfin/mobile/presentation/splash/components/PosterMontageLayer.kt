package org.jellyfin.mobile.presentation.splash.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import org.jellyfin.mobile.presentation.splash.model.SplashPoster

@Composable
fun PosterMontageLayer(
    posters: List<SplashPoster>,
    onMontageComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val totalCuts = minOf(16, posters.size)

    LaunchedEffect(Unit) {
        for (i in 0 until totalCuts) {
            val stepDuration = maxOf(45L, 130L - (i * 6L))
            currentIndex = i
            delay(stepDuration)
        }
        delay(80L)
        onMontageComplete()
    }

    AnimatedContent(
        targetState = currentIndex,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 40)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 40))
        },
        modifier = modifier.fillMaxSize(),
        label = "poster_cut_transition"
    ) { index ->
        val poster = posters[index % posters.size]
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(poster.imageUrl)
                    .crossfade(false)
                    .memoryCacheKey(poster.imageUrl)
                    .build(),
                contentDescription = poster.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Cinematic Vignette Overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.65f),
                            0.35f to Color.Transparent,
                            0.70f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )

            // Poster Dynamic Info Overlay at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.70f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            Color(0xFF38BDF8).copy(alpha = 0.40f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = poster.badge,
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = poster.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
