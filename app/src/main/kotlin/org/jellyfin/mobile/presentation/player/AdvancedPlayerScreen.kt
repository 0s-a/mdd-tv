package org.jellyfin.mobile.presentation.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.jellyfin.mobile.presentation.player.components.ArabicSubtitleOverlay
import org.jellyfin.mobile.presentation.player.components.PlayerGestureLayer
import org.jellyfin.mobile.presentation.player.components.PlayerHudOverlay
import org.jellyfin.mobile.presentation.player.components.PlayerSurface
import org.jellyfin.mobile.ui.theme.JellyfinColors

/**
 * AdvancedPlayerScreen represents Screen 04 - Fullscreen Immersive Media3 Video Player.
 * Strictly adheres to docs/screens/04_ADVANCED_PLAYER.md:
 * - Immersive system bar hiding (WindowInsetsControllerCompat with BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE).
 * - Full SurfaceView hardware decoding and zero battery waste.
 * - Dynamic system brightness overriding and screen wake-lock.
 * - Native Picture-in-Picture (PiP) support with auto-enter on Android 12+.
 * - Arabic subtitle overlay with high contrast and dynamic lift.
 */
@Composable
fun AdvancedPlayerScreen(
    mediaId: String,
    startPositionMs: Long = 0L,
    viewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val hapticFeedback = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()

    var isInPiP by remember { mutableStateOf(activity?.isInPictureInPictureMode ?: false) }

    // Dynamic Video Size tracking for PiP aspect ratio
    var videoWidth by remember { mutableStateOf(viewModel.player.videoSize.width) }
    var videoHeight by remember { mutableStateOf(viewModel.player.videoSize.height) }

    DisposableEffect(viewModel.player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }
        }
        viewModel.player.addListener(listener)
        onDispose {
            viewModel.player.removeListener(listener)
        }
    }

    val dynamicPipRational = remember(videoWidth, videoHeight) {
        if (videoWidth > 0 && videoHeight > 0) {
            val aspect = (videoWidth.toFloat() / videoHeight.toFloat()).coerceIn(0.42f, 2.38f)
            Rational((aspect * 1000).toInt(), 1000)
        } else {
            Rational(16, 9)
        }
    }

    // Initialize player with media ID and starting position
    LaunchedEffect(mediaId) {
        viewModel.dispatch(PlayerIntent.InitializePlayer(mediaId, startPositionMs))
    }

    // Monitor PiP lifecycle changes on ComponentActivity
    DisposableEffect(activity) {
        val componentActivity = activity as? ComponentActivity
        val pipListener = Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isInPiP = info.isInPictureInPictureMode
        }
        componentActivity?.addOnPictureInPictureModeChangedListener(pipListener)

        onDispose {
            componentActivity?.removeOnPictureInPictureModeChangedListener(pipListener)
        }
    }

    // Pause playback when leaving screen/backgrounding without PiP
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        activity?.isInPictureInPictureMode == true
                    } else {
                        false
                    }
                    if (!inPip) {
                        viewModel.dispatch(PlayerIntent.Pause)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Configure PiP auto-enter on Android 12+ (API 31+) with dynamic aspect ratio
    LaunchedEffect(activity, dynamicPipRational) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null) {
            val pipParams = PictureInPictureParams.Builder()
                .setAutoEnterEnabled(true)
                .setAspectRatio(dynamicPipRational)
                .build()
            activity.setPictureInPictureParams(pipParams)
        }
    }

    // Fullscreen Immersive Mode & WakeLock Lifecycle
    DisposableEffect(activity) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.let { win ->
                val attrs = win.attributes
                attrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = attrs
            }
        }
    }

    // Update system window brightness
    LaunchedEffect(uiState.brightness) {
        activity?.window?.let { win ->
            val attrs = win.attributes
            attrs.screenBrightness = uiState.brightness
            win.attributes = attrs
        }
    }

    // Handle one-time side effects
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PlayerEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                PlayerEffect.TriggerHapticTick -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                PlayerEffect.TriggerHapticHeavy -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                PlayerEffect.TriggerHapticDouble -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                PlayerEffect.ClosePlayerScreen -> {
                    onNavigateBack()
                }
                PlayerEffect.EnterPictureInPicture -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(dynamicPipRational)
                            .build()
                        activity.enterPictureInPictureMode(params)
                    }
                }
            }
        }
    }

    // Handle back press
    BackHandler {
        if (uiState.isControlsLocked) {
            viewModel.dispatch(PlayerIntent.ToggleControlsLock)
        } else {
            viewModel.dispatch(PlayerIntent.ReleasePlayer)
            onNavigateBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JellyfinColors.PureBlack)
    ) {
        // 1. Hardware Video Surface (SurfaceView)
        PlayerSurface(
            player = viewModel.player,
            aspectRatioMode = uiState.aspectRatioMode,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Arabic Subtitle Overlay (Lifted above HUD)
        ArabicSubtitleOverlay(
            subtitleText = uiState.currentSubtitleText,
            isHudVisible = uiState.isHudVisible && !isInPiP,
            subtitleOffsetMs = uiState.subtitleOffsetMs,
            modifier = Modifier.fillMaxSize()
        )

        // Hide gesture & HUD controls completely while inside Picture-in-Picture
        if (!isInPiP) {
            // 3. Touch Gesture Engine Layer
            PlayerGestureLayer(
                isControlsLocked = uiState.isControlsLocked,
                isHudVisible = uiState.isHudVisible,
                isPlaying = uiState.isPlaying,
                currentVolume = uiState.volume,
                currentBrightness = uiState.brightness,
                onVolumeChange = { viewModel.dispatch(PlayerIntent.SetVolume(it)) },
                onBrightnessChange = { viewModel.dispatch(PlayerIntent.SetBrightness(it)) },
                onFastSeekBy = { viewModel.dispatch(PlayerIntent.FastSeekBy(it)) },
                onToggleHud = { viewModel.dispatch(PlayerIntent.ToggleHudVisibility) },
                modifier = Modifier.fillMaxSize()
            )

            // 4. Floating Frosted Glass HUD Overlay
            PlayerHudOverlay(
                uiState = uiState.copy(isInPictureInPicture = isInPiP),
                onNavigateBack = {
                    viewModel.dispatch(PlayerIntent.ReleasePlayer)
                    onNavigateBack()
                },
                onTogglePlayPause = { viewModel.dispatch(PlayerIntent.TogglePlayPause) },
                onSeekTo = { viewModel.dispatch(PlayerIntent.SeekTo(it)) },
                onFastSeekBy = { viewModel.dispatch(PlayerIntent.FastSeekBy(it)) },
                onToggleLock = { viewModel.dispatch(PlayerIntent.ToggleControlsLock) },
                onSelectSubtitle = { viewModel.dispatch(PlayerIntent.SelectSubtitleTrack(it)) },
                onAdjustSubtitleOffset = { viewModel.dispatch(PlayerIntent.AdjustSubtitleOffset(it)) },
                onSelectAudio = { viewModel.dispatch(PlayerIntent.SelectAudioTrack(it)) },
                onSetPlaybackSpeed = { viewModel.dispatch(PlayerIntent.SetPlaybackSpeed(it)) },
                onSetAspectRatio = { viewModel.dispatch(PlayerIntent.SetAspectRatio(it)) },
                onEnterPiP = { viewModel.dispatch(PlayerIntent.EnterPictureInPicture) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Safely resolves the hosting Activity from a Composable Context.
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
