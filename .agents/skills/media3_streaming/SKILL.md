---
name: media3-streaming
description: >-
  Provides architecture patterns and code guidelines for AndroidX Media3 (ExoPlayer) playback,
  MediaSession, AudioFocus, stream resolution with Jellyfin Server, and MVI player state management.
  Use when implementing or fixing video/audio playback, player HUD overlays, playback lifecycle, buffering, or streaming errors in mdd_tv.
---

# AndroidX Media3 & Jellyfin Streaming Architecture Guide

## Overview

This skill establishes the production standards for video playback, hardware surface rendering, audio focus lifecycle, and Jellyfin API stream integration within `mdd_tv`.

---

## 1. Golden Rules for Media3 Implementation

1. **Strictly `androidx.media3`:** Never import legacy `com.google.android.exoplayer2.*` packages. All imports must reside under `androidx.media3.*`.
2. **Hardware Direct Play Priority:** Prioritize direct hardware decoding on the native Android surface. Only request transcoded streams when formats or audio codecs are unsupported by the client device.
3. **Player Lifecycle Binding:** ExoPlayer instances must be released and surfaces decoupled when leaving the playback screen or when transitioning to background without active audio.
4. **MVI Player State Machine:** The UI layer must only observe state and emit user intents (Play, Pause, Seek, ToggleSubtitles, ToggleHud). The player controller handles state transitions deterministically.

---

## 2. Playback Lifecycle in Jetpack Compose

To safely embed Media3 `PlayerView` into Jetpack Compose without surface leaks:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayerSurface(
    player: Player?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                useController = false // Custom Compose HUD controls are used instead
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            }
        },
        update = { playerView ->
            if (playerView.player != player) {
                playerView.player = player
            }
        },
        modifier = modifier
    )

    DisposableEffect(player) {
        onDispose {
            // Player detachment handled here or in ViewModel lifecycle
        }
    }
}
```

---

## 3. Audio Focus & System Integration

Always route playback requests through the project's `AudioFocusManager` before triggering `player.play()`:

```kotlin
// In PlayerViewModel or PlaybackController:
fun handlePlayIntent() {
    val focusGranted = audioFocusManager.requestAudioFocus {
        // Callback on focus loss (e.g., call incoming or external audio requested)
        player.pause()
    }

    if (focusGranted) {
        player.play()
    }
}
```

---

## 4. Jellyfin Stream URL Construction & Headers

When constructing media sources for ExoPlayer from Jellyfin:

* **Direct Stream Endpoint:**
  `${serverUrl}/Videos/${itemId}/stream?static=true&api_key=${token}`
* **Transcode Fallback Endpoint:**
  `${serverUrl}/Videos/${itemId}/master.m3u8?api_key=${token}&VideoCodec=h264,hevc&AudioCodec=aac,ac3`
* **Authentication Header:**
  Ensure the `HttpDataSource.Factory` injects `X-Emby-Token: ${token}` or passes `api_key` in query parameters.
* **Resume Position Reporting:**
  Send periodic playback progress reports to `/Sessions/Playing/Progress` at regular intervals (e.g., every 10 seconds) and `/Sessions/Playing/Stopped` on exit.

---

## 5. MVI Playback State Contract

Maintain player state cleanly separated from UI rendering:

```kotlin
data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isHudVisible: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val activeAudioTrack: String? = null,
    val activeSubtitleTrack: String? = null,
    val errorMessage: String? = null
)

sealed interface PlayerUiEvent {
    object TogglePlayPause : PlayerUiEvent
    data class SeekTo(val positionMs: Long) : PlayerUiEvent
    data class SeekRelative(val offsetSeconds: Int) : PlayerUiEvent
    object ToggleHud : PlayerUiEvent
    data class SelectSubtitle(val trackIndex: Int) : PlayerUiEvent
    object DismissError : PlayerUiEvent
}
```
