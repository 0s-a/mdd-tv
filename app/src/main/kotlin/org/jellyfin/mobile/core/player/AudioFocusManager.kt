package org.jellyfin.mobile.core.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * AudioFocusManager
 *
 * Centralises all audio-focus and audio-routing interactions for a media player
 * component. It is designed to be paired with a single player instance and must
 * be released when that player is destroyed.
 *
 * Responsibilities:
 *   1. Requesting and abandoning audio focus (AUDIOFOCUS_GAIN) using the
 *      modern [AudioFocusRequest] API on API >= 26, with a legacy fallback for
 *      older devices.
 *   2. Handling transient focus loss (e.g. incoming phone call):
 *        - AUDIOFOCUS_LOSS_TRANSIENT: pause playback; resume when focus returns.
 *        - AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: lower volume; restore when done.
 *        - AUDIOFOCUS_LOSS (permanent): stop and abandon focus.
 *   3. Listening for [AudioManager.ACTION_AUDIO_BECOMING_NOISY] and pausing
 *      immediately when headphones are disconnected, preventing audio from
 *      being redirected to the loudspeaker unexpectedly.
 *
 * Usage:
 *   val manager = AudioFocusManager(context, listener)
 *   manager.requestAudioFocus()          // before starting playback
 *   // ... playback ...
 *   manager.release()                    // in onDestroy / player teardown
 */
class AudioFocusManager(
    private val context: Context,
    private val listener: Listener
) {

    /**
     * Callback interface for the owning player component.
     */
    interface Listener {
        /** Resume or start playback. */
        fun onAudioFocusGained()

        /** Pause playback immediately. */
        fun onAudioFocusPaused()

        /**
         * Reduce playback volume to duck under a concurrent audio source.
         * Restore to normal volume when [onAudioFocusGained] is called.
         */
        fun onAudioFocusDucked()

        /**
         * Stop and release the player; audio focus has been permanently lost
         * (e.g. another app started media playback).
         */
        fun onAudioFocusLostPermanently()

        /** Pause playback because headphones were disconnected. */
        fun onBecomingNoisy()
    }

    companion object {
        private const val TAG = "AudioFocusManager"
        private const val DUCK_VOLUME_FACTOR = 0.2f
    }

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Tracks whether this manager currently holds audio focus.
    private var hasFocus = false

    // Tracks whether playback was paused due to a transient focus loss so that
    // we can resume it automatically when focus is regained.
    private var pausedForTransientLoss = false

    // -------------------------------------------------------------------------
    // AudioFocusRequest (API 26+)
    // -------------------------------------------------------------------------

    private val audioFocusRequest: AudioFocusRequest? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(buildAudioAttributes())
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false) // we handle ducking ourselves
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
        } else {
            null
        }
    }

    // -------------------------------------------------------------------------
    // Audio focus change listener
    // -------------------------------------------------------------------------

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                if (pausedForTransientLoss) {
                    pausedForTransientLoss = false
                    listener.onAudioFocusGained()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasFocus = false
                pausedForTransientLoss = false
                listener.onAudioFocusLostPermanently()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasFocus = false
                pausedForTransientLoss = true
                listener.onAudioFocusPaused()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // The system prefers that we duck rather than pause. We honour
                // this only if we have not already been paused for another reason.
                listener.onAudioFocusDucked()
            }

            else -> Log.d(TAG, "Unknown audio focus change: $focusChange")
        }
    }

    // -------------------------------------------------------------------------
    // Becoming-noisy broadcast receiver
    // -------------------------------------------------------------------------

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                Log.d(TAG, "Headphones disconnected; pausing playback to prevent noise.")
                listener.onBecomingNoisy()
            }
        }
    }

    private var noisyReceiverRegistered = false

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Requests [AudioManager.AUDIOFOCUS_GAIN] from the system.
     *
     * @return true if focus was granted immediately; false if the request was
     *         delayed or denied. The [Listener.onAudioFocusGained] callback will
     *         fire if delayed focus is eventually granted.
     */
    fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = audioFocusRequest ?: return false
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (hasFocus) {
            registerNoisyReceiver()
        }
        return hasFocus
    }

    /**
     * Abandons audio focus and unregisters the becoming-noisy receiver.
     * Call this when the player pauses or stops at the user's request.
     */
    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
        hasFocus = false
        pausedForTransientLoss = false
        unregisterNoisyReceiver()
    }

    /**
     * Fully releases this manager. Must be called when the player is destroyed.
     * After calling release(), this instance must not be used again.
     */
    fun release() {
        abandonAudioFocus()
        // noisyReceiver is already unregistered by abandonAudioFocus; guard anyway.
        unregisterNoisyReceiver()
    }

    /**
     * Returns true if this manager currently holds audio focus.
     */
    fun hasFocus(): Boolean = hasFocus

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun registerNoisyReceiver() {
        if (noisyReceiverRegistered) return
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        ContextCompat.registerReceiver(
            context,
            noisyReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        noisyReceiverRegistered = true
    }

    private fun unregisterNoisyReceiver() {
        if (!noisyReceiverRegistered) return
        try {
            context.unregisterReceiver(noisyReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered; safe to ignore.
        }
        noisyReceiverRegistered = false
    }

    private fun buildAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
    }
}
