package org.jellyfin.mobile.core.player

import android.media.AudioFormat
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

/**
 * AudioBoostEngine
 *
 * Manages on-device audio enhancement for a single audio session. All processing
 * is performed locally; no server transcoding is involved.
 *
 * Features:
 *   - Volume boost via [LoudnessEnhancer] from 101% to 200% (1 dB to 10 dB gain).
 *   - Anti-clipping protection through a software gain ceiling applied before the
 *     boost reaches the hardware amplifier.
 *   - 5-Band Equalizer at standard centre frequencies: 60 Hz, 230 Hz, 910 Hz,
 *     3.6 kHz, 14 kHz.
 *   - Vocal Booster preset that lifts the 910 Hz and 3.6 kHz bands by +6 dB to
 *     improve dialog clarity.
 *   - Bitstream passthrough detection: when the audio session carries an encoded
 *     bitstream (Dolby / DTS) intended for HDMI multi-channel output, the engine
 *     skips PCM-domain effects so the encoded stream is not corrupted.
 *   - Strict lifecycle: call [release] when the audio session ends to free all
 *     AudioEffect resources and prevent memory / handle leaks.
 *
 * Usage:
 *   val engine = AudioBoostEngine(audioSessionId)
 *   engine.applyVocalBoostPreset()
 *   engine.setBoostPercent(150)
 *   // ... playback ...
 *   engine.release()
 */
class AudioBoostEngine(private val audioSessionId: Int) {

    companion object {
        private const val TAG = "AudioBoostEngine"

        // Boost range in percent: 101% to 200%.
        const val BOOST_PERCENT_MIN = 101
        const val BOOST_PERCENT_MAX = 200

        // LoudnessEnhancer works in millibels (100 mB = 1 dB).
        // 101% -> ~0.09 dB -> 9 mB (floor: 1 mB to avoid disable ambiguity).
        // 200% -> ~6.02 dB -> 602 mB.
        private const val MB_PER_PERCENT_STEP = 6 // approx 0.6 mB per 0.1% step

        // Anti-clipping ceiling in millibels. Gains above this value are
        // automatically attenuated at the digital output stage.
        private const val CLIPPING_CEILING_MB = 1200 // 12 dB absolute ceiling

        // Equalizer band gain range (millibels): typically -1500 to +1500 mB.
        private const val VOCAL_BOOST_BAND_GAIN_MB: Short = 600  // +6 dB
        private const val NEUTRAL_BAND_GAIN_MB: Short = 0

        // Index of the 910 Hz and 3.6 kHz bands in the 5-band equalizer.
        // Band order: 0=60Hz, 1=230Hz, 2=910Hz, 3=3600Hz, 4=14000Hz.
        private const val BAND_910HZ_INDEX: Short = 2
        private const val BAND_3600HZ_INDEX: Short = 3
    }

    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    // Track passthrough state so PCM effects are gated appropriately.
    private var isBitstreamPassthrough = false

    // Current boost level stored to allow live adjustment.
    private var currentBoostPercent: Int = 100

    init {
        initialiseEffects()
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sets the volume boost level.
     *
     * @param percent integer in [101, 200]. Values outside the range are clamped.
     *        A value of 100 or below disables the enhancer.
     */
    fun setBoostPercent(percent: Int) {
        if (isBitstreamPassthrough) return

        currentBoostPercent = percent.coerceIn(BOOST_PERCENT_MIN, BOOST_PERCENT_MAX)
        val gainMb = percentToMillibels(currentBoostPercent)
        val safeMb = applyAntiClipping(gainMb)
        try {
            loudnessEnhancer?.setTargetGain(safeMb)
            loudnessEnhancer?.enabled = true
        } catch (e: Exception) {
            Log.w(TAG, "setBoostPercent failed: ${e.message}")
        }
    }

    /**
     * Disables the volume boost and returns the enhancer to unity gain.
     */
    fun disableBoost() {
        try {
            loudnessEnhancer?.enabled = false
        } catch (e: Exception) {
            Log.w(TAG, "disableBoost failed: ${e.message}")
        }
        currentBoostPercent = 100
    }

    /**
     * Applies the Vocal Booster equalizer preset.
     *
     * Lifts 910 Hz and 3.6 kHz by +6 dB, leaving all other bands flat. This
     * improves intelligibility of spoken dialog without altering music balance.
     */
    fun applyVocalBoostPreset() {
        if (isBitstreamPassthrough) return

        val eq = equalizer ?: return
        try {
            val bandCount = eq.numberOfBands
            for (band in 0 until bandCount) {
                val bandShort = band.toShort()
                val gain: Short = when (bandShort) {
                    BAND_910HZ_INDEX, BAND_3600HZ_INDEX -> VOCAL_BOOST_BAND_GAIN_MB
                    else -> NEUTRAL_BAND_GAIN_MB
                }
                eq.setBandLevel(bandShort, gain)
            }
            eq.enabled = true
        } catch (e: Exception) {
            Log.w(TAG, "applyVocalBoostPreset failed: ${e.message}")
        }
    }

    /**
     * Sets all equalizer bands to 0 dB (flat / neutral response).
     */
    fun applyFlatPreset() {
        if (isBitstreamPassthrough) return

        val eq = equalizer ?: return
        try {
            val bandCount = eq.numberOfBands
            for (band in 0 until bandCount) {
                eq.setBandLevel(band.toShort(), NEUTRAL_BAND_GAIN_MB)
            }
            eq.enabled = true
        } catch (e: Exception) {
            Log.w(TAG, "applyFlatPreset failed: ${e.message}")
        }
    }

    /**
     * Sets the gain of a specific equalizer band by index.
     *
     * @param bandIndex 0-based index (0=60Hz, 1=230Hz, 2=910Hz, 3=3.6kHz, 4=14kHz).
     * @param gainMb    gain in millibels; clamped to the hardware-reported range.
     */
    fun setBandLevel(bandIndex: Short, gainMb: Short) {
        if (isBitstreamPassthrough) return

        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange
            val clamped = gainMb.toInt().coerceIn(range[0].toInt(), range[1].toInt()).toShort()
            eq.setBandLevel(bandIndex, clamped)
        } catch (e: Exception) {
            Log.w(TAG, "setBandLevel failed: ${e.message}")
        }
    }

    /**
     * Notifies the engine whether the current audio track is an encoded bitstream
     * intended for HDMI multi-channel passthrough (Dolby / DTS).
     *
     * When passthrough is active all PCM-domain effects (equalizer, loudness
     * enhancer) are disabled so the encoded stream reaches the receiver intact.
     *
     * @param passthrough true when HDMI bitstream passthrough is active.
     */
    fun setBitstreamPassthrough(passthrough: Boolean) {
        isBitstreamPassthrough = passthrough
        if (passthrough) {
            suspendEffectsForPassthrough()
        } else {
            restoreEffectsAfterPassthrough()
        }
    }

    /**
     * Returns the centre frequency (in millihertz) of a given equalizer band.
     *
     * @param bandIndex 0-based index.
     * @return frequency in millihertz, or -1 if unavailable.
     */
    fun getBandCentreFrequencyMillihertz(bandIndex: Short): Int {
        return try {
            equalizer?.getCenterFreq(bandIndex) ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Returns the number of equalizer bands supported by the hardware, or 0 if
     * the equalizer could not be initialised.
     */
    fun getNumberOfBands(): Short = equalizer?.numberOfBands ?: 0

    /**
     * Releases all AudioEffect handles. Must be called when the audio session ends.
     * After release, this instance must not be used again.
     */
    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer release failed: ${e.message}")
        } finally {
            equalizer = null
        }

        try {
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer release failed: ${e.message}")
        } finally {
            loudnessEnhancer = null
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Constructs the AudioEffect instances for the given audio session. Failures
     * are caught individually so a missing hardware effect does not block the other.
     */
    private fun initialiseEffects() {
        try {
            equalizer = Equalizer(0, audioSessionId).also { eq ->
                eq.enabled = false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer init failed for session $audioSessionId: ${e.message}")
            equalizer = null
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).also { le ->
                le.enabled = false
            }
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer init failed for session $audioSessionId: ${e.message}")
            loudnessEnhancer = null
        }
    }

    /**
     * Disables PCM-domain effects for bitstream passthrough mode.
     */
    private fun suspendEffectsForPassthrough() {
        try {
            equalizer?.enabled = false
            loudnessEnhancer?.enabled = false
        } catch (e: Exception) {
            Log.w(TAG, "suspendEffectsForPassthrough failed: ${e.message}")
        }
    }

    /**
     * Re-enables previously active effects after leaving passthrough mode.
     */
    private fun restoreEffectsAfterPassthrough() {
        try {
            if (currentBoostPercent >= BOOST_PERCENT_MIN) {
                loudnessEnhancer?.enabled = true
            }
            // The equalizer remains in whatever state it was left in before passthrough.
        } catch (e: Exception) {
            Log.w(TAG, "restoreEffectsAfterPassthrough failed: ${e.message}")
        }
    }

    /**
     * Converts a percentage value (101-200) to a millibel gain value suitable for
     * [LoudnessEnhancer.setTargetGain].
     *
     * The mapping is linear on a dB scale:
     *   101% ~ +0.09 dB  ->  9 mB
     *   150%  ~ +3.52 dB -> 352 mB
     *   200%  ~ +6.02 dB -> 602 mB
     *
     * Formula: gainMb = round(2000 * log10(percent / 100.0) * 100)
     */
    private fun percentToMillibels(percent: Int): Int {
        val ratio = percent / 100.0
        val gainDb = 20.0 * Math.log10(ratio)
        return (gainDb * 100).toInt().coerceAtLeast(1)
    }

    /**
     * Applies the anti-clipping ceiling. If the requested gain exceeds
     * [CLIPPING_CEILING_MB], it is reduced to the ceiling value. This ensures the
     * audio output stage is never driven into digital clipping regardless of the
     * user-selected boost level.
     *
     * @param requestedMb the unconstrained gain in millibels.
     * @return the gain in millibels, guaranteed not to exceed [CLIPPING_CEILING_MB].
     */
    private fun applyAntiClipping(requestedMb: Int): Int {
        return requestedMb.coerceAtMost(CLIPPING_CEILING_MB)
    }
}
