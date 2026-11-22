package org.jellyfin.mobile.presentation.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.jellyfin.mobile.core.player.external.ExternalPlayerIntentBuilder
import org.jellyfin.mobile.core.player.external.ExternalPlayerPayload
import org.jellyfin.mobile.core.player.external.ExternalPlayerResultParser
import org.jellyfin.mobile.presentation.player.components.PlayerActionsModal
import org.jellyfin.mobile.presentation.player.components.PlayerHudOverlay
import org.jellyfin.mobile.presentation.player.components.PlayerSurface
import org.jellyfin.mobile.presentation.player.gestures.GestureOverlayHud
import org.jellyfin.mobile.presentation.player.gestures.PlayerGestureCallbacks
import org.jellyfin.mobile.presentation.player.gestures.PlayerGestureState
import org.jellyfin.mobile.presentation.player.gestures.playerGestureDetector
import org.jellyfin.mobile.presentation.player.subtitles.CinemaSubtitleRenderer
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
    viewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    startPositionMs: Long = 0L
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val hapticFeedback = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()

    var isInPiP by remember { mutableStateOf(activity?.isInPictureInPictureMode ?: false) }

    // Dynamic Video Size tracking for PiP aspect ratio
    var videoWidth by remember { mutableIntStateOf(viewModel.player.videoSize.width) }
    var videoHeight by remember { mutableIntStateOf(viewModel.player.videoSize.height) }

    // External player activity result launcher
    var activeExternalPayload by remember { mutableStateOf<ExternalPlayerPayload?>(null) }
    var externalLaunchTimestamp by remember { mutableLongStateOf(0L) }

    val externalPlayerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val payload = activeExternalPayload
        val elapsedMs = SystemClock.elapsedRealtime() - externalLaunchTimestamp
        if (payload != null) {
            val playbackResult = ExternalPlayerResultParser.parse(
                payload = payload,
                resultCode = result.resultCode,
                data = result.data,
                elapsedMs = elapsedMs
            )
            viewModel.onExternalPlaybackResult(playbackResult)
        }
        activeExternalPayload = null
    }

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
                    val inPip = activity?.isInPictureInPictureMode == true
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
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
                is PlayerEffect.LaunchExternalPlayer -> {
                    activeExternalPayload = effect.payload
                    externalLaunchTimestamp = SystemClock.elapsedRealtime()
                    val chooserIntent = ExternalPlayerIntentBuilder.buildChooserIntent(effect.payload)
                    externalPlayerLauncher.launch(chooserIntent)
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

    val gestureState = remember { PlayerGestureState() }
    LaunchedEffect(uiState.isControlsLocked) {
        gestureState.isTouchLocked = uiState.isControlsLocked
    }
    val isLandscape = activity?.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current.density
    var lastInteractionTime by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    // Auto-dismiss HUD after 3 seconds of inactivity while video playback is active
    LaunchedEffect(
        uiState.isHudVisible,
        uiState.isPlaying,
        uiState.isControlsLocked,
        uiState.isActionsModalVisible,
        lastInteractionTime
    ) {
        if (uiState.isHudVisible && uiState.isPlaying && !uiState.isControlsLocked && !uiState.isActionsModalVisible) {
            delay(3000L)
            viewModel.dispatch(PlayerIntent.ToggleHudVisibility)
        }
    }

    val gestureCallbacks = remember(viewModel, hapticFeedback) {
        object : PlayerGestureCallbacks {
            override fun onSeekDelta(deltaMs: Long) {
                lastInteractionTime = SystemClock.elapsedRealtime()
                viewModel.dispatch(PlayerIntent.FastSeekBy(deltaMs))
            }
            override fun onBrightnessChange(brightness: Float) {
                lastInteractionTime = SystemClock.elapsedRealtime()
                viewModel.dispatch(PlayerIntent.SetBrightness(brightness))
            }
            override fun onVolumeChange(volume: Float) {
                lastInteractionTime = SystemClock.elapsedRealtime()
                viewModel.dispatch(PlayerIntent.SetVolume(volume))
            }
            override fun onLongPressStart() {
                lastInteractionTime = SystemClock.elapsedRealtime()
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.dispatch(PlayerIntent.SetPlaybackSpeed(2.0f))
            }
            override fun onLongPressEnd() {
                lastInteractionTime = SystemClock.elapsedRealtime()
                viewModel.dispatch(PlayerIntent.SetPlaybackSpeed(1.0f))
            }
            override fun onDoubleTapSeekBackward() {
                lastInteractionTime = SystemClock.elapsedRealtime()
                viewModel.dispatch(PlayerIntent.FastSeekBy(-10_000L))
            }
            override fun onDoubleTapSeekForward() {
                lastInteractionTime = SystemClock.elapsedRealtime()
                viewModel.dispatch(PlayerIntent.FastSeekBy(10_000L))
            }
            override fun onDoubleTapCenter() {
                lastInteractionTime = SystemClock.elapsedRealtime()
                viewModel.dispatch(PlayerIntent.TogglePlayPause)
            }
            override fun onZoomChange(scale: Float, panOffset: Offset) {
                lastInteractionTime = SystemClock.elapsedRealtime()
                // Zoom & pan state maintained live in gestureState
            }
            override fun onTouchLockToggle(locked: Boolean) {
                lastInteractionTime = SystemClock.elapsedRealtime()
                viewModel.dispatch(PlayerIntent.ToggleControlsLock)
            }
            override fun onSingleTap() {
                lastInteractionTime = SystemClock.elapsedRealtime()
                viewModel.dispatch(PlayerIntent.ToggleHudVisibility)
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(JellyfinColors.PureBlack)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    lastInteractionTime = SystemClock.elapsedRealtime()
                    when (event.key) {
                        Key.DirectionCenter,
                        Key.Enter,
                        Key.NumPadEnter -> {
                            if (!uiState.isHudVisible) {
                                viewModel.dispatch(PlayerIntent.ToggleHudVisibility)
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionLeft -> {
                            viewModel.dispatch(PlayerIntent.FastSeekBy(-10_000L))
                            true
                        }
                        Key.DirectionRight -> {
                            viewModel.dispatch(PlayerIntent.FastSeekBy(10_000L))
                            true
                        }
                        Key.DirectionUp,
                        Key.DirectionDown -> {
                            if (!uiState.isHudVisible) {
                                viewModel.dispatch(PlayerIntent.ToggleHudVisibility)
                                true
                            } else {
                                false
                            }
                        }
                        Key.MediaPlayPause,
                        Key.HeadsetHook -> {
                            viewModel.dispatch(PlayerIntent.TogglePlayPause)
                            true
                        }
                        Key.MediaPlay -> {
                            viewModel.dispatch(PlayerIntent.Play)
                            true
                        }
                        Key.MediaPause -> {
                            viewModel.dispatch(PlayerIntent.Pause)
                            true
                        }
                        Key.MediaFastForward,
                        Key.MediaStepForward -> {
                            viewModel.dispatch(PlayerIntent.FastSeekBy(10_000L))
                            true
                        }
                        Key.MediaRewind,
                        Key.MediaStepBackward -> {
                            viewModel.dispatch(PlayerIntent.FastSeekBy(-10_000L))
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()

        // 1. Hardware Video Surface (SurfaceView) with Pinch-to-Zoom & Pan
        PlayerSurface(
            player = viewModel.player,
            aspectRatioMode = uiState.aspectRatioMode,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = gestureState.zoomScale,
                    scaleY = gestureState.zoomScale,
                    translationX = gestureState.zoomPanOffset.x,
                    translationY = gestureState.zoomPanOffset.y
                )
        )

        // 2. High-contrast Cinema Subtitle Renderer (Lifted dynamically above HUD)
        CinemaSubtitleRenderer(
            rawCueText = uiState.currentSubtitleText,
            isHudVisible = uiState.isHudVisible && !isInPiP,
            modifier = Modifier.fillMaxSize()
        )

        // Hide gesture & HUD controls completely while inside Picture-in-Picture
        if (!isInPiP) {
            // 3. Zero-Allocation Gesture Layer (MX Player Style Strict Axis Lock 1.5x)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .playerGestureDetector(
                        state = gestureState,
                        callbacks = gestureCallbacks,
                        isLandscape = isLandscape,
                        containerWidthPx = containerWidthPx,
                        containerHeightPx = containerHeightPx,
                        currentPositionMs = uiState.currentPositionMs,
                        currentBrightness = uiState.brightness,
                        currentVolume = uiState.volume,
                        density = density
                    )
            )

            // 4. OLED True-Black Ephemeral Gesture HUD Overlays (Volume, Brightness, Seek, 2x)
            GestureOverlayHud(
                state = gestureState,
                modifier = Modifier.fillMaxSize()
            )

            // 5. Minimalist Cinema Heads-Up Display (Triad + Scrubber)
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
                onToggleOrientation = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    activity?.let { act ->
                        val currentLandscape = act.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                        act.requestedOrientation = if (currentLandscape) {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                    }
                },
                onOpenActionsModal = { viewModel.dispatch(PlayerIntent.ToggleActionsModal) },
                onGetThumbnail = { viewModel.getThumbnail(it) },
                modifier = Modifier.fillMaxSize()
            )

            // 6. Modular Unified Cinema Actions Modal (Subtitles, Audio, Speed, Aspect Ratio, EQ, PiP)
            PlayerActionsModal(
                uiState = uiState,
                isVisible = uiState.isActionsModalVisible,
                isPlaying = uiState.isPlaying,
                audioBands = uiState.audioBands,
                isBitstreamPassthrough = uiState.isBitstreamPassthrough,
                onDismiss = { viewModel.dispatch(PlayerIntent.SetActionsModalVisibility(false)) },
                onIntent = { viewModel.dispatch(it) },
                onSetEqualizerBand = { index, gain -> viewModel.dispatch(PlayerIntent.SetEqualizerBand(index, gain)) },
                onApplyVocalBoost = { viewModel.dispatch(PlayerIntent.ApplyVocalBoost) },
                onSetBitstreamPassthrough = { viewModel.dispatch(PlayerIntent.SetBitstreamPassthrough(it)) },
                onSetAudioDelay = { viewModel.dispatch(PlayerIntent.SetAudioDelay(it)) },
                onLaunchExternalPlayer = { viewModel.dispatch(PlayerIntent.LaunchExternalPlayer) },
                onEnterPiP = { viewModel.dispatch(PlayerIntent.EnterPictureInPicture) },
                onSetAspectRatio = { viewModel.dispatch(PlayerIntent.SetAspectRatio(it)) },
                onSetPlaybackSpeed = { viewModel.dispatch(PlayerIntent.SetPlaybackSpeed(it)) },
                onPlayNextEpisode = if (!uiState.nextEpisodeId.isNullOrBlank()) {
                    { viewModel.dispatch(PlayerIntent.PlayNextEpisode) }
                } else null
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
