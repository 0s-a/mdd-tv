package org.jellyfin.mobile.presentation.player

import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerContractTest {

    @Test
    fun formatPlayerTime_formatsSecondsProperly() {
        assertEquals("00:00", formatPlayerTime(0L))
        assertEquals("00:45", formatPlayerTime(45_000L))
        assertEquals("05:30", formatPlayerTime(330_000L))
        assertEquals("59:59", formatPlayerTime(3_599_000L))
    }

    @Test
    fun formatPlayerTime_formatsHoursProperly() {
        assertEquals("01:00:00", formatPlayerTime(3_600_000L))
        assertEquals("02:45:15", formatPlayerTime(9_915_000L))
    }

    @Test
    fun formatPlayerTime_handlesNegativeValuesSafely() {
        assertEquals("00:00", formatPlayerTime(-5000L))
    }

    @Test
    fun playerUiState_calculatesProgressPercentagesAccurately() {
        val state = PlayerUiState(
            currentPositionMs = 30_000L,
            bufferedPositionMs = 60_000L,
            totalDurationMs = 120_000L
        )

        assertEquals(0.25f, state.progressPercentage, 0.001f)
        assertEquals(0.50f, state.bufferedPercentage, 0.001f)
        assertEquals("00:30", state.formattedCurrentTime)
        assertEquals("02:00", state.formattedTotalTime)
        assertEquals("-01:30", state.formattedRemainingTime)
    }

    @Test
    fun playerUiState_zeroDuration_handlesPercentagesSafely() {
        val state = PlayerUiState(
            currentPositionMs = 0L,
            bufferedPositionMs = 0L,
            totalDurationMs = 0L
        )

        assertEquals(0.0f, state.progressPercentage, 0.001f)
        assertEquals(0.0f, state.bufferedPercentage, 0.001f)
        assertEquals("00:00", state.formattedCurrentTime)
        assertEquals("00:00", state.formattedTotalTime)
        assertEquals("-00:00", state.formattedRemainingTime)
    }

    @Test
    fun playerUiState_defaults_adhereToV2Specification() {
        val state = PlayerUiState()

        assertTrue(state.isBuffering)
        assertFalse(state.isPlaying)
        assertFalse(state.isControlsLocked)
        assertTrue(state.isHudVisible)
        assertEquals(1.0f, state.volume, 0.001f)
        assertEquals(0.5f, state.brightness, 0.001f)
        assertEquals(1.0f, state.playbackSpeed, 0.001f)
        assertEquals(AspectRatioMode.Fit, state.aspectRatioMode)
        assertNull(state.selectedSubtitle)
        assertNull(state.selectedAudioTrack)
        assertNull(state.currentSubtitleText)
        assertEquals(0L, state.subtitleOffsetMs)
        assertNull(state.nextEpisodeId)
    }

    @Test
    fun playerUiState_volumeBoostCalculation() {
        val boostedState = PlayerUiState(volume = 1.8f)
        assertTrue(boostedState.volume > 1.0f)
        assertEquals(1.8f, boostedState.volume, 0.001f)
    }

    @Test
    fun playerIntents_instantiationAndDataHold() {
        val initIntent = PlayerIntent.InitializePlayer("media_123", 45_000L)
        assertEquals("media_123", initIntent.mediaId)
        assertEquals(45_000L, initIntent.startPositionMs)

        val seekIntent = PlayerIntent.SeekTo(90_000L)
        assertEquals(90_000L, seekIntent.positionMs)

        val fastSeekIntent = PlayerIntent.FastSeekBy(10_000L)
        assertEquals(10_000L, fastSeekIntent.deltaMs)

        val offsetIntent = PlayerIntent.AdjustSubtitleOffset(100L)
        assertEquals(100L, offsetIntent.offsetMs)

        val speedIntent = PlayerIntent.SetPlaybackSpeed(1.5f)
        assertEquals(1.5f, speedIntent.speed, 0.001f)
    }

    @Test
    fun subtitleOption_immutabilityAndEquality() {
        val sub1 = SubtitleOption(
            index = 0,
            name = "العربية",
            language = "ar",
            isDefault = true,
            trackGroupIndex = 0,
            trackIndex = 0
        )
        val sub2 = SubtitleOption(
            index = 0,
            name = "العربية",
            language = "ar",
            isDefault = true,
            trackGroupIndex = 0,
            trackIndex = 0
        )
        assertEquals(sub1, sub2)
        assertEquals("ar", sub1.language)
        assertTrue(sub1.isDefault)
    }

    @Test
    fun audioOption_immutabilityAndEquality() {
        val audio1 = AudioOption(
            index = 0,
            name = "Arabic AAC",
            language = "ar",
            codec = "audio/mp4a-latm",
            trackGroupIndex = 1,
            trackIndex = 0
        )
        val audio2 = AudioOption(
            index = 0,
            name = "Arabic AAC",
            language = "ar",
            codec = "audio/mp4a-latm",
            trackGroupIndex = 1,
            trackIndex = 0
        )
        assertEquals(audio1, audio2)
        assertEquals("audio/mp4a-latm", audio1.codec)
    }

    @Test
    fun playerEffects_distinctTypes() {
        val toast = PlayerEffect.ShowToast("مرحباً")
        assertEquals("مرحباً", toast.message)

        val haptics = listOf(
            PlayerEffect.TriggerHapticTick,
            PlayerEffect.TriggerHapticHeavy,
            PlayerEffect.TriggerHapticDouble
        )
        assertEquals(3, haptics.size)
        assertTrue(haptics.contains(PlayerEffect.TriggerHapticHeavy))
    }
}