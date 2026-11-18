package org.jellyfin.mobile.domain

import org.jellyfin.mobile.domain.model.EpisodeSummary
import org.jellyfin.mobile.domain.model.MediaDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaDetailsTest {

    @Test
    fun mediaDetails_durationText_formatsHoursAndMinutes() {
        // 2 hours 15 minutes = (2 * 60 + 15) * 60 seconds * 10,000,000 ticks
        val ticks = 135L * 60L * 10_000_000L
        val details = MediaDetails(
            id = "movie-1",
            title = "Interstellar",
            runTimeTicks = ticks
        )

        assertEquals("2س 15د", details.durationText)
    }

    @Test
    fun mediaDetails_durationText_formatsMinutesOnly() {
        // 45 minutes = 45 * 60 seconds * 10,000,000 ticks
        val ticks = 45L * 60L * 10_000_000L
        val details = MediaDetails(
            id = "episode-1",
            title = "Pilot",
            runTimeTicks = ticks
        )

        assertEquals("45د", details.durationText)
    }

    @Test
    fun mediaDetails_durationText_handlesNull() {
        val details = MediaDetails(
            id = "item-0",
            title = "No Duration",
            runTimeTicks = null
        )

        assertNull(details.durationText)
    }

    @Test
    fun episodeSummary_durationText_formatsCorrectly() {
        val ticks = 55L * 60L * 10_000_000L
        val episode = EpisodeSummary(
            id = "ep-1",
            name = "Winter Is Coming",
            runTimeTicks = ticks
        )

        assertEquals("55د", episode.durationText)
    }
}
