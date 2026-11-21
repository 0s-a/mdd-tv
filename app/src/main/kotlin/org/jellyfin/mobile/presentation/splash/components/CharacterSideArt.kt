package org.jellyfin.mobile.presentation.splash.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import org.jellyfin.mobile.presentation.splash.model.SplashPoster

private enum class SideFadeDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT
}

@Composable
fun CharacterSideArt(
    posters: List<SplashPoster>,
    modifier: Modifier = Modifier
) {
    if (posters.isEmpty()) return

    var leftIndex by remember { mutableIntStateOf(0) }
    var rightIndex by remember { mutableIntStateOf(minOf(5, posters.size - 1)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4500L)
            leftIndex = (leftIndex + 1) % posters.size
            rightIndex = (rightIndex + 1) % posters.size
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Left Side Character
        AnimatedContent(
            targetState = leftIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 900)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 900))
            },
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .align(Alignment.CenterStart),
            label = "left_character_fade"
        ) { idx ->
            CharacterSideImage(
                imageUrl = posters[idx % posters.size].imageUrl,
                fadeDirection = SideFadeDirection.LEFT_TO_RIGHT
            )
        }

        // Right Side Character
        AnimatedContent(
            targetState = rightIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 900)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 900))
            },
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .align(Alignment.CenterEnd),
            label = "right_character_fade"
        ) { idx ->
            CharacterSideImage(
                imageUrl = posters[idx % posters.size].imageUrl,
                fadeDirection = SideFadeDirection.RIGHT_TO_LEFT
            )
        }
    }
}

@Composable
private fun CharacterSideImage(
    imageUrl: String,
    fadeDirection: SideFadeDirection
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.50f)
        )

        // Gradient fade toward the center into OLED True Black
        val maskBrush = when (fadeDirection) {
            SideFadeDirection.LEFT_TO_RIGHT -> Brush.horizontalGradient(
                0.0f to Color.Transparent,
                0.85f to Color.Black
            )
            SideFadeDirection.RIGHT_TO_LEFT -> Brush.horizontalGradient(
                0.0f to Color.Black,
                0.85f to Color.Transparent
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(maskBrush)
        )
    }
}
