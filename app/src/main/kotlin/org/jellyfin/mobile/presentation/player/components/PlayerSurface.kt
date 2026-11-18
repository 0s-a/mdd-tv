package org.jellyfin.mobile.presentation.player.components

import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import org.jellyfin.mobile.presentation.player.AspectRatioMode
import org.jellyfin.mobile.ui.theme.JellyfinColors

/**
 * PlayerSurface renders hardware-accelerated video frames via SurfaceView.
 * Strictly adheres to docs/screens/04_ADVANCED_PLAYER.md:
 * - 100% SurfaceView via AndroidView (STRICTLY NO TextureView) for maximum battery efficiency,
 *   zero GPU compositing overhead, and true 10-bit HDR / Dolby Vision passthrough.
 * - Dynamic aspect ratio modes (Fit, Fill, Stretch, Original).
 * - Safe lifecycle attachment and detaching with zero leaks.
 */
@Composable
fun PlayerSurface(
    player: ExoPlayer,
    aspectRatioMode: AspectRatioMode,
    modifier: Modifier = Modifier
) {
    var videoWidth by remember { mutableIntStateOf(player.videoSize.width.coerceAtLeast(1)) }
    var videoHeight by remember { mutableIntStateOf(player.videoSize.height.coerceAtLeast(1)) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    val videoAspectRatio = remember(videoWidth, videoHeight) {
        if (videoHeight > 0) videoWidth.toFloat() / videoHeight.toFloat() else 16f / 9f
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(JellyfinColors.PureBlack)
    ) {
        val screenAspect = if (maxHeight.value > 0) maxWidth.value / maxHeight.value else 16f / 9f
        val fillScale = if (videoAspectRatio > 0f && screenAspect > 0f) {
            maxOf(screenAspect / videoAspectRatio, videoAspectRatio / screenAspect)
        } else {
            1.2f
        }

        val surfaceModifier = when (aspectRatioMode) {
            AspectRatioMode.Fit -> {
                Modifier
                    .fillMaxSize()
                    .aspectRatio(videoAspectRatio, matchHeightConstraintsFirst = false)
            }
            AspectRatioMode.Fill -> {
                Modifier
                    .fillMaxSize()
                    .scale(fillScale)
            }
            AspectRatioMode.Stretch -> {
                Modifier.fillMaxSize()
            }
            AspectRatioMode.Original -> {
                Modifier
                    .fillMaxSize()
                    .aspectRatio(videoAspectRatio, matchHeightConstraintsFirst = true)
            }
        }

        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player.setVideoSurfaceView(this)
                }
            },
            update = { surfaceView ->
                player.setVideoSurfaceView(surfaceView)
            },
            onRelease = { surfaceView ->
                player.clearVideoSurfaceView(surfaceView)
            },
            modifier = surfaceModifier
        )
    }
}
