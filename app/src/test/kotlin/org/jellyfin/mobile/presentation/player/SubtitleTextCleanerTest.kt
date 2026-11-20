package org.jellyfin.mobile.presentation.player

import org.jellyfin.mobile.presentation.player.util.SubtitleTextCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleTextCleanerTest {

    @Test
    fun `clean returns null on null or blank input`() {
        assertNull(SubtitleTextCleaner.clean(null))
        assertNull(SubtitleTextCleaner.clean(""))
        assertNull(SubtitleTextCleaner.clean("   "))
    }

    @Test
    fun `clean leaves plain SRT Arabic text unchanged`() {
        val input = "مرحبا بكم في استراحة الرصاص"
        val expected = "مرحبا بكم في استراحة الرصاص"
        assertEquals(expected, SubtitleTextCleaner.clean(input))
    }

    @Test
    fun `clean converts ASS escaped newlines to actual linebreaks`() {
        val input = "السطر الأول\\Nالسطر الثاني"
        val expected = "السطر الأول\nالسطر الثاني"
        assertEquals(expected, SubtitleTextCleaner.clean(input))
    }

    @Test
    fun `clean strips ASS formatting tags completely`() {
        val input = "{\\an8\\pos(192,200)\\c&H00FFFF&}ترجمة عربية احترافية"
        val expected = "ترجمة عربية احترافية"
        assertEquals(expected, SubtitleTextCleaner.clean(input))
    }

    @Test
    fun `clean handles mixed ASS tags with newlines`() {
        val input = "{\\b1}العنوان الرئيسي{\\b0}\\N{\\i1}تفاصيل فرعية{\\i0}"
        val expected = "العنوان الرئيسي\nتفاصيل فرعية"
        assertEquals(expected, SubtitleTextCleaner.clean(input))
    }
}
