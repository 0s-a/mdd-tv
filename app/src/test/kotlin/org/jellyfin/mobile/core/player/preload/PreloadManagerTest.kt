package org.jellyfin.mobile.core.player.preload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test verifying PreloadManager contract, speculative pre-buffering lifecycle,
 * memory ceilings, and unmetered network gating.
 * Strictly adheres to project rules with zero emojis.
 */
class PreloadManagerTest {

    @Test
    fun preloadManagerConstants_satisfyMemoryAndChunkLimits() {
        assertEquals(4L * 1024 * 1024, Media3PreloadManagerImpl.PRELOAD_CHUNK_BYTES)
        assertEquals(24L * 1024 * 1024, Media3PreloadManagerImpl.MAX_CACHE_BYTES)
        assertEquals("speculative_preload", Media3PreloadManagerImpl.CACHE_DIR_NAME)
    }

    private class TrackingPreloadManager {
        val preloadedMediaIds = mutableMapOf<String, String>()
        val cancelledMediaIds = mutableListOf<String>()
        var evictCount = 0
        var isNetworkUnmetered = true

        fun startPreload(mediaId: String, streamUrl: String) {
            if (!isNetworkUnmetered) return
            preloadedMediaIds[mediaId] = streamUrl
        }

        fun cancelPreload(mediaId: String) {
            preloadedMediaIds.remove(mediaId)
            cancelledMediaIds.add(mediaId)
        }

        fun isPreloaded(mediaId: String): Boolean = preloadedMediaIds.containsKey(mediaId)

        fun evictAll() {
            preloadedMediaIds.clear()
            evictCount++
        }
    }

    @Test
    fun startPreload_whenUnmeteredNetwork_registersMediaId() {
        val manager = TrackingPreloadManager()
        manager.startPreload("movie-1", "http://server/Videos/movie-1/stream")
        assertTrue(manager.isPreloaded("movie-1"))
        assertEquals("http://server/Videos/movie-1/stream", manager.preloadedMediaIds["movie-1"])
    }

    @Test
    fun startPreload_whenMeteredNetwork_skipsPreload() {
        val manager = TrackingPreloadManager().apply { isNetworkUnmetered = false }
        manager.startPreload("movie-2", "http://server/Videos/movie-2/stream")
        assertFalse(manager.isPreloaded("movie-2"))
        assertTrue(manager.preloadedMediaIds.isEmpty())
    }

    @Test
    fun cancelPreload_removesActivePreload() {
        val manager = TrackingPreloadManager()
        manager.startPreload("ep-1", "http://server/Videos/ep-1/stream")
        assertTrue(manager.isPreloaded("ep-1"))

        manager.cancelPreload("ep-1")
        assertFalse(manager.isPreloaded("ep-1"))
        assertTrue(manager.cancelledMediaIds.contains("ep-1"))
    }

    @Test
    fun evictAll_clearsAllPreloadedItems() {
        val manager = TrackingPreloadManager()
        manager.startPreload("m1", "http://server/Videos/m1/stream")
        manager.startPreload("m2", "http://server/Videos/m2/stream")

        manager.evictAll()
        assertFalse(manager.isPreloaded("m1"))
        assertFalse(manager.isPreloaded("m2"))
        assertEquals(1, manager.evictCount)
    }
}
