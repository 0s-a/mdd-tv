package org.jellyfin.mobile.core.util

/**
 * BiDi (Bidirectional) Text Formatter to prevent alignment distortion and word inversion
 * in Arabic/English mixed strings across Android UI components.
 *
 * Employs standard Unicode Directional Formatting:
 * - LRM (\u200E): Left-to-Right Mark for English titles, codecs, and technical parameters.
 * - RLM (\u200F): Right-to-Left Mark for Arabic headings, season/episode prefixes.
 */
object BidiFormatter {
    private const val LRM = "\u200E"
    private const val RLM = "\u200F"

    /**
     * Isolates a string as Left-to-Right content (e.g. English title, Codec, Resolution).
     */
    fun wrapLtr(text: String): String {
        if (text.isBlank()) return text
        return "$LRM$text$LRM"
    }

    /**
     * Isolates a string as Right-to-Left content (Arabic prose).
     */
    fun wrapRtl(text: String): String {
        if (text.isBlank()) return text
        return "$RLM$text$RLM"
    }

    /**
     * Formats an episode title with mixed English and Arabic characters cleanly:
     * e.g. "Breaking Bad - الموسم 1 - الحلقة 4: Pilot"
     */
    fun formatEpisodeDisplay(
        seriesName: String?,
        seasonNumber: Int,
        episodeNumber: Int,
        episodeTitle: String?
    ): String {
        val sName = seriesName?.let { wrapLtr(it) } ?: ""
        val seasonLabel = "${wrapRtl("الموسم")} $seasonNumber"
        val episodeLabel = "${wrapRtl("الحلقة")} $episodeNumber"
        val epTitle = episodeTitle?.takeIf { it.isNotBlank() }?.let { wrapLtr(it) }

        return buildString {
            if (sName.isNotBlank()) {
                append(sName)
                append(" • ")
            }
            append(seasonLabel)
            append(" • ")
            append(episodeLabel)
            if (!epTitle.isNullOrBlank()) {
                append(": ")
                append(epTitle)
            }
        }
    }

    /**
     * Formats titles with release year without punctuation flipping.
     */
    fun formatTitleWithYear(title: String, year: Int?): String {
        if (year == null) return title
        return "${wrapLtr(title)} ($year)"
    }
}
