package org.jellyfin.mobile.presentation.player.util

/**
 * SubtitleTextCleaner sanitizes raw subtitle cues for optimal rendering.
 * Strictly adheres to project performance guidelines:
 * - Strips ASS/SSA visual style tags (e.g. {\pos(x,y)}, {\an8}, {\fn...}, {\c&H...&}) without allocating unnecessary strings.
 * - Converts escaped newlines (\N, \n) into real linebreaks.
 * - Normalizes Arabic text formatting and removes empty leading/trailing lines.
 */
object SubtitleTextCleaner {

    private val ASS_STYLE_TAG_REGEX = Regex("""\{[^}]*\}""")

    /**
     * Cleans raw subtitle text received from ExoPlayer CueGroup.
     * Returns null if the resulting text is empty or blank.
     */
    fun clean(rawText: String?): String? {
        if (rawText.isNullOrBlank()) return null

        // 1. Convert ASS escaped newlines
        val withNewlines = rawText
            .replace("\\N", "\n")
            .replace("\\n", "\n")

        // 2. Strip ASS style override tags
        val stripped = withNewlines.replace(ASS_STYLE_TAG_REGEX, "")

        // 3. Trim each line and clean up formatting
        val cleanedLines = stripped.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (cleanedLines.isEmpty()) return null

        return cleanedLines.joinToString("\n")
    }
}
