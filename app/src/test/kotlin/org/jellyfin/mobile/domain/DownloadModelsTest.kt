package org.jellyfin.mobile.domain

import org.jellyfin.mobile.domain.model.DownloadTaskProgress
import org.jellyfin.mobile.domain.model.DownloadedMediaItem
import org.jellyfin.mobile.domain.model.StorageBreakdown
import org.jellyfin.mobile.domain.model.StorageGaugeInfo
import org.jellyfin.mobile.domain.model.toStorageGaugeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadModelsTest {

    @Test
    fun storageGaugeInfo_computesGbCorrectly() {
        val oneGb = 1024L * 1024L * 1024L
        val storage = StorageGaugeInfo(
            appUsedBytes = 2 * oneGb,
            systemAndOtherUsedBytes = 4 * oneGb,
            freeBytes = 10 * oneGb
        )

        assertEquals(16 * oneGb, storage.totalBytes)
        assertEquals(2.0, storage.appUsedGb, 0.01)
        assertEquals(4.0, storage.otherUsedGb, 0.01)
        assertEquals(10.0, storage.freeGb, 0.01)
        assertEquals(16.0, storage.totalGb, 0.01)
    }

    @Test
    fun storageBreakdown_toStorageGaugeInfo_mapsCorrectly() {
        val oneGb = 1024L * 1024L * 1024L
        val breakdown = StorageBreakdown(
            totalDeviceBytes = 32 * oneGb,
            freeDeviceBytes = 20 * oneGb,
            jellyfinMediaBytes = 4 * oneGb,
            otherAppsBytes = 8 * oneGb
        )

        val gaugeInfo = breakdown.toStorageGaugeInfo()
        assertEquals(4 * oneGb, gaugeInfo.appUsedBytes)
        assertEquals(8 * oneGb, gaugeInfo.systemAndOtherUsedBytes)
        assertEquals(20 * oneGb, gaugeInfo.freeBytes)
        assertEquals(32 * oneGb, gaugeInfo.totalBytes)
    }

    @Test
    fun downloadTaskProgress_calculatesProgressAndSpeed() {
        val task = DownloadTaskProgress(
            mediaId = "media-1",
            title = "Test Movie",
            qualityInfo = "1080p",
            totalBytes = 2000L,
            downloadedBytes = 1000L,
            speedBytesPerSec = 10 * 1024 * 1024L,
            remainingSeconds = 60
        )

        assertEquals(0.5f, task.progressPercentage, 0.001f)
        assertEquals(10.0, task.speedMbPerSec, 0.01)
    }

    @Test
    fun downloadedMediaItem_fileSizeGb_computesProperly() {
        val oneGb = 1024L * 1024L * 1024L
        val item = DownloadedMediaItem(
            id = "item-1",
            title = "Inception",
            mediaType = "Movie",
            totalSizeBytes = 3 * oneGb
        )

        assertEquals(3.0, item.fileSizeGb, 0.01)
        assertTrue(item.mediaType == "Movie")
    }
}
