package org.jellyfin.mobile.core.player.external

import android.content.Intent
import android.net.Uri

/**
 * Factory for constructing Android Intents compatible with major media players
 * (MX Player, Just Player, VLC, MPV, Nova Player) per Android Intent Contract specifications.
 */
object ExternalPlayerIntentBuilder {

    fun buildIntent(payload: ExternalPlayerPayload): Intent {
        val streamUri = Uri.parse(payload.streamUrl)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(streamUri, "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Titles
            putExtra("title", payload.title)
            putExtra(Intent.EXTRA_TITLE, payload.title)

            // Resume Position & Result Contract
            putExtra("position", payload.resumePositionMs.toInt())
            putExtra("position_ms", payload.resumePositionMs)
            putExtra("return_result", true)

            // Duration
            if (payload.durationMs > 0L) {
                putExtra("duration", payload.durationMs.toInt())
                putExtra("duration_ms", payload.durationMs)
            }

            // Subtitles (MX Player format)
            if (payload.subtitles.isNotEmpty()) {
                val subUris = payload.subtitles.map { Uri.parse(it.url) }.toTypedArray()
                val subNames = payload.subtitles.map { it.name }.toTypedArray()
                putExtra("subs", subUris)
                putExtra("subs.name", subNames)

                val defaultSub = payload.subtitles.firstOrNull { it.isDefault } ?: payload.subtitles.first()
                putExtra("subs.enable", Uri.parse(defaultSub.url))
            }

            // Custom HTTP headers
            putExtra("headers", arrayOf("User-Agent", "MDD-TV"))
        }

        return intent
    }

    fun buildChooserIntent(payload: ExternalPlayerPayload, chooserTitle: String = "تشغيل بواسطة"): Intent {
        val targetIntent = buildIntent(payload)
        return Intent.createChooser(targetIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
