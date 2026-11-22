package org.jellyfin.mobile.core.player.external

import android.content.Intent

/**
 * Parses playback results returned by external video players via ActivityResultLauncher.
 * Supports direct position extraction (MX Player, Just Player) and adaptive time estimation fallback (VLC).
 */
object ExternalPlayerResultParser {

    private const val MIN_ELAPSED_FOR_ESTIMATION_MS = 5_000L
    private const val COMPLETION_THRESHOLD_RATIO = 0.90

    fun parse(
        payload: ExternalPlayerPayload,
        resultCode: Int,
        data: Intent?,
        elapsedMs: Long
    ): ExternalPlaybackResult {
        val mediaId = payload.mediaId
        val fallbackDuration = payload.durationMs
        val startPosition = payload.resumePositionMs

        // 1. Direct result extraction from returned intent extras
        if (data != null) {
            val returnedPosition: Long? = when {
                data.hasExtra("position") -> {
                    val intPos = data.getIntExtra("position", -1)
                    if (intPos >= 0) intPos.toLong() else data.getLongExtra("position", -1L).takeIf { it >= 0 }
                }
                data.hasExtra("position_ms") -> {
                    data.getLongExtra("position_ms", -1L).takeIf { it >= 0 }
                }
                else -> null
            }

            val returnedDuration: Long = when {
                data.hasExtra("duration") -> {
                    val intDur = data.getIntExtra("duration", -1)
                    if (intDur > 0) intDur.toLong() else data.getLongExtra("duration", -1L).takeIf { it > 0 } ?: fallbackDuration
                }
                data.hasExtra("duration_ms") -> {
                    data.getLongExtra("duration_ms", -1L).takeIf { it > 0 } ?: fallbackDuration
                }
                else -> fallbackDuration
            }

            val endBy = data.getStringExtra("end_by")

            if (returnedPosition != null) {
                val isCompleted = endBy.equals("playback_completion", ignoreCase = true) ||
                        (returnedDuration > 0L && (returnedPosition.toDouble() / returnedDuration) >= COMPLETION_THRESHOLD_RATIO)

                return ExternalPlaybackResult(
                    mediaId = mediaId,
                    positionMs = returnedPosition,
                    durationMs = returnedDuration,
                    isCompleted = isCompleted
                )
            }
        }

        // 2. Adaptive time estimation fallback (e.g., VLC, MPV)
        if (elapsedMs >= MIN_ELAPSED_FOR_ESTIMATION_MS) {
            val maxBound = if (fallbackDuration > 0L) fallbackDuration else Long.MAX_VALUE
            val estimatedPosition = (startPosition + elapsedMs).coerceAtMost(maxBound)
            val isCompleted = fallbackDuration > 0L && (estimatedPosition.toDouble() / fallbackDuration) >= COMPLETION_THRESHOLD_RATIO

            return ExternalPlaybackResult(
                mediaId = mediaId,
                positionMs = estimatedPosition,
                durationMs = fallbackDuration,
                isCompleted = isCompleted
            )
        }

        // 3. User closed or aborted immediately
        return ExternalPlaybackResult(
            mediaId = mediaId,
            positionMs = startPosition,
            durationMs = fallbackDuration,
            isCompleted = false
        )
    }
}
