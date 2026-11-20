package org.jellyfin.mobile.core.util

/**
 * StreamNameParser intelligently resolves and localizes media stream titles (Audio & Subtitles)
 * from Jellyfin API, resolving unlabeled 'Language: und' streams and providing descriptive Arabic labels.
 */
object StreamNameParser {

    /**
     * Resolves human-readable Arabic label for an audio stream.
     */
    fun resolveAudioStreamName(
        languageCode: String?,
        title: String?,
        codec: String?,
        channels: Int?,
        channelLayout: String?,
        streamIndex: Int = 0,
        isDefault: Boolean = false
    ): String {
        val resolvedLang = resolveLanguageName(languageCode, title)
        val channelsText = when {
            channels == 8 || channelLayout?.contains("7.1") == true -> "7.1 محيطي"
            channels == 6 || channelLayout?.contains("5.1") == true -> "5.1 محيطي"
            channels == 2 -> "ستيريو"
            channels == 1 -> "أحادي"
            channels != null && channels > 0 -> "$channels.0"
            else -> ""
        }
        val codecText = codec?.uppercase() ?: ""

        val baseLabel = when {
            resolvedLang.isNotBlank() && resolvedLang != "غير محدد" -> resolvedLang
            !title.isNullOrBlank() -> title
            else -> "مسار صوتي ${streamIndex + 1}"
        }

        val technicalDetails = listOf(channelsText, codecText).filter { it.isNotBlank() }.joinToString(" • ")
        val defaultSuffix = if (isDefault) " (افتراضي)" else ""

        return if (technicalDetails.isNotBlank()) {
            "$baseLabel ($technicalDetails)$defaultSuffix"
        } else {
            "$baseLabel$defaultSuffix"
        }
    }

    /**
     * Resolves human-readable Arabic label for a subtitle stream.
     */
    fun resolveSubtitleStreamName(
        languageCode: String?,
        title: String?,
        codec: String?,
        isForced: Boolean = false,
        isDefault: Boolean = false,
        streamIndex: Int = 0
    ): String {
        val resolvedLang = resolveLanguageName(languageCode, title)
        val codecText = when (codec?.lowercase()) {
            "subrip", "srt" -> "SRT"
            "ass" -> "ASS"
            "pgssub", "pgs" -> "بلوراي PGS"
            "vtt", "webvtt" -> "VTT"
            else -> codec?.uppercase() ?: ""
        }

        val baseLabel = when {
            resolvedLang.isNotBlank() && resolvedLang != "غير محدد" -> resolvedLang
            !title.isNullOrBlank() -> title
            else -> "ترجمة ${streamIndex + 1}"
        }

        val suffix = buildString {
            if (codecText.isNotBlank()) append(" [$codecText]")
            if (isForced) append(" (إجباري)")
            if (isDefault) append(" (افتراضي)")
        }

        return "$baseLabel$suffix"
    }

    /**
     * Resolves ISO code or raw title to localized Arabic language name.
     */
    fun resolveLanguageName(code: String?, title: String?): String {
        val normalizedCode = code?.trim()?.lowercase() ?: ""
        val normalizedTitle = title?.trim()?.lowercase() ?: ""

        // 1. Direct ISO 639 matching
        when (normalizedCode) {
            "ara", "ar", "arabic" -> return "العربية"
            "eng", "en", "english" -> return "الإنجليزية"
            "fra", "fre", "fr", "french" -> return "الفرنسية"
            "spa", "es", "spanish" -> return "الإسبانية"
            "deu", "ger", "de", "german" -> return "الألمانية"
            "ita", "it", "italian" -> return "الإيطالية"
            "tur", "tr", "turkish" -> return "التركية"
            "jpn", "ja", "japanese" -> return "اليابانية"
            "kor", "ko", "korean" -> return "الكورية"
            "rus", "ru", "russian" -> return "الروسية"
            "hin", "hi", "hindi" -> return "الهندية"
        }

        // 2. Keyword inference from stream title when code is 'und' or blank
        if (normalizedCode == "und" || normalizedCode.isBlank()) {
            when {
                normalizedTitle.contains("arabic") || normalizedTitle.contains("ara") ||
                        normalizedTitle.contains("عربي") || normalizedTitle.contains("عربية") ||
                        normalizedTitle.contains("دبلجة") -> return "العربية"

                normalizedTitle.contains("english") || normalizedTitle.contains("eng") ||
                        normalizedTitle.contains("انجليزي") -> return "الإنجليزية"

                normalizedTitle.contains("french") || normalizedTitle.contains("français") -> return "الفرنسية"
                normalizedTitle.contains("turkish") || normalizedTitle.contains("تركي") -> return "التركية"
                normalizedTitle.contains("japanese") || normalizedTitle.contains("ياباني") -> return "اليابانية"
            }
        }

        return if (normalizedCode.isNotBlank() && normalizedCode != "und") normalizedCode else "غير محدد"
    }
}
