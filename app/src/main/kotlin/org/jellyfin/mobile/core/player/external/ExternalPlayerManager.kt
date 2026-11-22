package org.jellyfin.mobile.core.player.external

import android.content.Context
import android.content.Intent
import org.jellyfin.mobile.domain.repository.LocalProfileRepository

/**
 * High-level coordinator for external video player lifecycle, intent creation,
 * result parsing, and local Room DB persistence adhering to ADR 0001.
 */
interface ExternalPlayerManager {
    fun buildIntent(payload: ExternalPlayerPayload): Intent
    fun buildChooserIntent(payload: ExternalPlayerPayload, chooserTitle: String = "تشغيل بواسطة"): Intent
    fun parseResult(payload: ExternalPlayerPayload, resultCode: Int, data: Intent?, elapsedMs: Long): ExternalPlaybackResult
    suspend fun persistResult(result: ExternalPlaybackResult, title: String, mediaType: String = "Movie")
}

class ExternalPlayerManagerImpl(
    private val localProfileRepository: LocalProfileRepository
) : ExternalPlayerManager {

    override fun buildIntent(payload: ExternalPlayerPayload): Intent {
        return ExternalPlayerIntentBuilder.buildIntent(payload)
    }

    override fun buildChooserIntent(payload: ExternalPlayerPayload, chooserTitle: String): Intent {
        return ExternalPlayerIntentBuilder.buildChooserIntent(payload, chooserTitle)
    }

    override fun parseResult(
        payload: ExternalPlayerPayload,
        resultCode: Int,
        data: Intent?,
        elapsedMs: Long
    ): ExternalPlaybackResult {
        return ExternalPlayerResultParser.parse(payload, resultCode, data, elapsedMs)
    }

    override suspend fun persistResult(
        result: ExternalPlaybackResult,
        title: String,
        mediaType: String
    ) {
        if (result.positionMs <= 0L && !result.isCompleted) return

        // 1. Save or update local watch progress (ticks = ms * 10_000)
        localProfileRepository.saveWatchProgress(
            itemId = result.mediaId,
            title = title,
            positionTicks = result.positionMs * 10_000L,
            durationTicks = result.durationMs * 10_000L,
            isPlayed = result.isCompleted
        )

        // 2. If finished/completed (>90% or completion signal), mark watched and archive
        if (result.isCompleted) {
            localProfileRepository.markWatched(result.mediaId)
            localProfileRepository.addToArchive(
                itemId = result.mediaId,
                title = title,
                mediaType = mediaType
            )
        }
    }
}
