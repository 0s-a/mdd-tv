package org.jellyfin.mobile.presentation.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.mobile.ui.components.glassmorphism
import org.jellyfin.mobile.ui.theme.JellyfinColors
import org.jellyfin.mobile.ui.theme.JellyfinDimens
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * PlayerGestureLayer handles physical touch interactions:
 * - Left vertical drag: system brightness (0% to 100%).
 * - Right vertical drag: audio volume with software boost up to 200%.
 * - Double tap left: seek -10s with ripple.
 * - Double tap right: seek +10s with ripple.
 * - Single tap: toggle HUD visibility with 3-second auto-dismiss.
 */
@Composable
fun PlayerGestureLayer(
    isControlsLocked: Boolean,
    isHudVisible: Boolean,
    isPlaying: Boolean,
    currentVolume: Float,
    currentBrightness: Float,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onFastSeekBy: (Long) -> Unit,
    onToggleHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val viewConfig = LocalViewConfiguration.current
    val touchSlop = viewConfig.touchSlop

    var isDraggingBrightness by remember { mutableStateOf(false) }
    var isDraggingVolume by remember { mutableStateOf(false) }

    var showRewindRipple by remember { mutableStateOf(false) }
    var showForwardRipple by remember { mutableStateOf(false) }

    val rewindScale = remember { Animatable(0.7f) }
    val forwardScale = remember { Animatable(0.7f) }

    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    var lastTapX by remember { mutableFloatStateOf(0f) }
    var userInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 3-second HUD auto-dismiss timer
    LaunchedEffect(isHudVisible, isPlaying, isControlsLocked, userInteractionTime) {
        if (isHudVisible && isPlaying && !isControlsLocked) {
            delay(3000L)
            onToggleHud()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isControlsLocked, currentBrightness, currentVolume) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val startY = down.position.y
                    val isLeftHalf = startX < (size.width / 2f)
                    var isDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            if (isDrag) {
                                isDrag = false
                                isDraggingBrightness = false
                                isDraggingVolume = false
                            } else {
                                // Tap evaluation
                                userInteractionTime = System.currentTimeMillis()
                                val now = System.currentTimeMillis()
                                val isDoubleTap = (now - lastTapTimeMs) < 320L && abs(startX - lastTapX) < 160f

                                if (isDoubleTap && !isControlsLocked) {
                                    lastTapTimeMs = 0L
                                    if (startX < (size.width / 2f)) {
                                        // Double tap left: Rewind -10s
                                        onFastSeekBy(-10_000L)
                                        coroutineScope.launch {
                                            showRewindRipple = true
                                            rewindScale.snapTo(0.6f)
                                            rewindScale.animateTo(
                                                targetValue = 1.2f,
                                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                                            )
                                            showRewindRipple = false
                                        }
                                    } else {
                                        // Double tap right: Forward +10s
                                        onFastSeekBy(10_000L)
                                        coroutineScope.launch {
                                            showForwardRipple = true
                                            forwardScale.snapTo(0.6f)
                                            forwardScale.animateTo(
                                                targetValue = 1.2f,
                                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                                            )
                                            showForwardRipple = false
                                        }
                                    }
                                } else {
                                    lastTapTimeMs = now
                                    lastTapX = startX
                                    coroutineScope.launch {
                                        delay(320L)
                                        if (lastTapTimeMs == now) {
                                            onToggleHud()
                                        }
                                    }
                                }
                            }
                            break
                        }

                        val dragY = change.position.y - startY
                        if (!isDrag && abs(dragY) > touchSlop && !isControlsLocked) {
                            isDrag = true
                            if (isLeftHalf) {
                                isDraggingBrightness = true
                            } else {
                                isDraggingVolume = true
                            }
                        }

                        if (isDrag) {
                            change.consume()
                            userInteractionTime = System.currentTimeMillis()
                            val deltaY = change.position.y - change.previousPosition.y
                            // 1.5x sensitivity for smooth responsive feel
                            val deltaNormalized = -deltaY / size.height.toFloat() * 1.5f

                            if (isLeftHalf) {
                                val newBrightness = (currentBrightness + deltaNormalized).coerceIn(0.01f, 1.0f)
                                onBrightnessChange(newBrightness)
                            } else {
                                val newVolume = (currentVolume + (deltaNormalized * 2.0f)).coerceIn(0.0f, 2.0f)
                                onVolumeChange(newVolume)
                            }
                        }
                    }
                }
            }
    ) {
        // Brightness indicator overlay (Left Half)
        AnimatedVisibility(
            visible = isDraggingBrightness,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
        ) {
            GestureFeedbackCapsule(
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = "سطوع الشاشة",
                        tint = JellyfinColors.TechSky,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = "السطوع",
                percentage = (currentBrightness * 100f).roundToInt(),
                progress = currentBrightness,
                isBoost = false
            )
        }

        // Volume indicator overlay (Right Half)
        AnimatedVisibility(
            visible = isDraggingVolume,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
        ) {
            val volumePercent = (currentVolume * 100f).roundToInt()
            val isBoost = currentVolume > 1.0f
            val volumeIcon = when {
                currentVolume <= 0.01f -> Icons.AutoMirrored.Rounded.VolumeMute
                currentVolume <= 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                else -> Icons.AutoMirrored.Rounded.VolumeUp
            }

            GestureFeedbackCapsule(
                icon = {
                    Icon(
                        imageVector = volumeIcon,
                        contentDescription = "مستوى الصوت",
                        tint = if (isBoost) Color(0xFFF97316) else JellyfinColors.PrimaryIndigo,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = if (isBoost) "مضخم الصوت" else "الصوت",
                percentage = volumePercent,
                progress = (currentVolume / 2.0f).coerceIn(0f, 1f),
                isBoost = isBoost
            )
        }

        // Double-Tap Rewind Ripple (-10s)
        if (showRewindRipple) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxSize(0.5f)
                    .align(Alignment.CenterStart)
                    .scale(rewindScale.value)
                    .alpha((1.2f - rewindScale.value).coerceIn(0f, 1f))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(JellyfinColors.SurfaceGlass)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastRewind,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-10 ثانية",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Double-Tap Forward Ripple (+10s)
        if (showForwardRipple) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxSize(0.5f)
                    .align(Alignment.CenterEnd)
                    .scale(forwardScale.value)
                    .alpha((1.2f - forwardScale.value).coerceIn(0f, 1f))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(JellyfinColors.SurfaceGlass)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+10 ثانية",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureFeedbackCapsule(
    icon: @Composable () -> Unit,
    title: String,
    percentage: Int,
    progress: Float,
    isBoost: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(80.dp)
            .glassmorphism(
                backgroundColor = JellyfinColors.SurfaceGlass,
                borderColor = if (isBoost) Color(0x66F97316) else JellyfinColors.BorderGlass,
                cornerRadius = JellyfinDimens.RadiusLarge
            )
            .padding(vertical = JellyfinDimens.SpaceMedium, horizontal = JellyfinDimens.SpaceSmall),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isBoost) Color(0xFFF97316) else JellyfinColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = JellyfinColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isBoost) Color(0xFFF97316) else JellyfinColors.TechSky,
                trackColor = Color(0x33FFFFFF)
            )
        }
    }
}
