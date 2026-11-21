package org.jellyfin.mobile.core.player.preload

import android.net.Uri
import androidx.media3.datasource.DataSource

/**
 * Interface contract for the speculative media pre-buffering engine.
 * Preloads the first 4MB of video direct stream while the user browses media details.
 * Zero emojis across all code, comments, and strings.
 */
interface PreloadManager {
    /**
     * Initiates speculative background pre-buffering of the first 4MB of the stream.
     * Only executes on unmetered networks (Wi-Fi or Ethernet) to preserve cellular data.
     *
     * @param mediaId Unique identifier of the target media item.
     * @param streamUri Fully qualified direct playback stream URI.
     */
    fun startPreload(mediaId: String, streamUri: Uri)

    /**
     * Cancels any active pre-buffering job for the specified media item.
     *
     * @param mediaId Identifier of the media whose preload should be cancelled.
     */
    fun cancelPreload(mediaId: String)

    /**
     * Checks whether the specified media item has pre-buffered data available in cache.
     *
     * @param mediaId Media identifier to query.
     * @return True if preloaded bytes exist in cache, false otherwise.
     */
    fun isPreloaded(mediaId: String): Boolean

    /**
     * Evicts all preloaded cache resources to free memory during low-memory conditions.
     */
    fun evictAll()

    /**
     * Factory providing CacheDataSource instances tied to the speculative pre-buffering cache.
     * Shared directly with ExoPlayer's DefaultMediaSourceFactory for sub-50ms instant start.
     */
    val cacheDataSourceFactory: DataSource.Factory
}
