package org.jellyfin.mobile.core.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TrickplayThumbnailManager
 *
 * Manages video thumbnail extraction for trickplay timeline scrubbing.
 *
 * Strategy (in priority order, lowest server cost first):
 *   1. Download pre-generated BIF (Base Index Frame) storyboard from the Jellyfin server.
 *      BIF files are static binary assets - zero server CPU transcoding involved.
 *   2. If no BIF is available, extract I-Frame thumbnails locally on the device using
 *      MediaMetadataRetriever with OPTION_CLOSEST_SYNC for hardware-accelerated snapping.
 *
 * All decoded bitmaps are stored in a 24 MB in-memory LruCache keyed by position milliseconds,
 * providing sub-10 ms retrieval for positions that have already been fetched.
 *
 * CONSTRAINT: This class never requests dynamic server-side transcoding for thumbnail frames.
 */
class TrickplayThumbnailManager(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    // 24 MB in-memory LRU cache; key = position in milliseconds.
    private val cache: LruCache<Long, Bitmap> = object : LruCache<Long, Bitmap>(CACHE_SIZE_BYTES) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }

    // Parsed BIF index: maps thumbnail position (ms) -> byte offset within BIF payload bytes.
    private val bifIndex: MutableList<BifEntry> = mutableListOf()

    // Raw BIF payload bytes stored after a successful download.
    private var bifPayload: ByteArray? = null

    // MediaMetadataRetriever used for local I-Frame fallback; lazily initialized per video URI.
    private var retriever: MediaMetadataRetriever? = null
    private var localVideoUri: String? = null

    // -------------------------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------------------------

    /**
     * Attempts to load a BIF storyboard from the given URL.
     * Must be called once per playback session before any [getThumbnail] calls so that BIF-based
     * lookups can be served from the cache without blocking individual scrub operations.
     *
     * Returns true if a valid BIF was successfully parsed; false otherwise.
     * On failure the caller should supply a local video URI via [setLocalVideoUri] so the
     * MediaMetadataRetriever fallback path activates.
     *
     * This function does NOT perform any dynamic transcoding request on the server.
     * BIF files are pre-generated static assets.
     */
    suspend fun loadBif(bifUrl: String): Boolean = withContext(ioDispatcher) {
        try {
            val bytes = downloadBytes(bifUrl) ?: return@withContext false
            parseBif(bytes)
            bifPayload = bytes
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Provides the local video URI for the MediaMetadataRetriever fallback path.
     * Call this only when BIF is unavailable.
     */
    fun setLocalVideoUri(uri: String) {
        if (uri != localVideoUri) {
            releaseRetriever()
            localVideoUri = uri
        }
    }

    /**
     * Returns the best available thumbnail bitmap for [positionMs].
     *
     * Resolution order:
     *   1. LRU cache hit (returns in <10 ms).
     *   2. BIF storyboard lookup (CPU-only decode, no network).
     *   3. Local MediaMetadataRetriever I-Frame extraction.
     *
     * Returns null if no thumbnail source is available yet.
     */
    /**
     * Returns the best available thumbnail bitmap synchronously from in-memory cache or BIF bytes.
     * Safe to call from UI/Compose remember blocks.
     */
    fun getThumbnailSync(positionMs: Long): Bitmap? {
        val snappedPosition = snapToBifInterval(positionMs)
        cache.get(snappedPosition)?.let { return it }
        val bitmap = decodeBifFrame(snappedPosition)
        if (bitmap != null) {
            cache.put(snappedPosition, bitmap)
        }
        return bitmap
    }

    suspend fun getThumbnail(positionMs: Long): Bitmap? {
        // Round to nearest BIF interval to maximize cache reuse.
        val snappedPosition = snapToBifInterval(positionMs)

        cache.get(snappedPosition)?.let { return it }

        return withContext(ioDispatcher) {
            val bitmap = decodeBifFrame(snappedPosition) ?: extractLocalFrame(snappedPosition)
            if (bitmap != null) {
                cache.put(snappedPosition, bitmap)
            }
            bitmap
        }
    }

    /**
     * Clears the in-memory cache and releases the MediaMetadataRetriever.
     * Call from the player lifecycle onStop / onDestroy.
     */
    fun release() {
        cache.evictAll()
        bifIndex.clear()
        bifPayload = null
        releaseRetriever()
    }

    // -------------------------------------------------------------------------------------------
    // BIF parsing
    // -------------------------------------------------------------------------------------------

    /**
     * Parses a BIF v0 or v1 binary storyboard file.
     *
     * BIF format summary:
     *   - Magic bytes 8  : 0x89 0x42 0x49 0x46 0x0D 0x0A 0x1A 0x0A
     *   - Version  4     : uint32 LE
     *   - ImageCount 4   : uint32 LE
     *   - TimestampMultiplier 4 : uint32 LE (0 means default 1000 ms per frame)
     *   - Reserved 44    : zero bytes
     *   - Index entries  : ImageCount * 8 bytes each
     *                        timestamp (uint32 LE) + absoluteOffset (uint32 LE)
     *   - Sentinel entry : 0xFFFFFFFF + totalDataSize
     */
    private fun parseBif(bytes: ByteArray) {
        bifIndex.clear()

        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Skip magic (8 bytes).
        buf.position(8)

        val version = buf.int
        val imageCount = buf.int
        val multiplier = buf.int.let { if (it == 0) DEFAULT_MULTIPLIER_MS else it.toLong() }

        // Skip reserved 44 bytes.
        buf.position(buf.position() + 44)

        for (i in 0 until imageCount) {
            val timestampTicks = buf.int.toLong() and 0xFFFFFFFFL
            val byteOffset = buf.int.toLong() and 0xFFFFFFFFL
            val positionMs = timestampTicks * multiplier
            bifIndex.add(BifEntry(positionMs, byteOffset.toInt()))
        }

        // Sentinel entry carries the offset just past the last image.
        val sentinelOffset = if (buf.remaining() >= 8) {
            buf.int // 0xFFFFFFFF
            (buf.int.toLong() and 0xFFFFFFFFL).toInt()
        } else {
            bytes.size
        }

        // Store end-offset in each entry by looking at the next entry's start-offset.
        for (i in bifIndex.indices) {
            val endOffset = if (i + 1 < bifIndex.size) bifIndex[i + 1].offset else sentinelOffset
            bifIndex[i] = bifIndex[i].copy(length = endOffset - bifIndex[i].offset)
        }
    }

    private fun decodeBifFrame(positionMs: Long): Bitmap? {
        val payload = bifPayload ?: return null
        if (bifIndex.isEmpty()) return null

        val entry = findNearestBifEntry(positionMs) ?: return null
        if (entry.offset < 0 || entry.length <= 0) return null
        if (entry.offset + entry.length > payload.size) return null

        return BitmapFactory.decodeByteArray(payload, entry.offset, entry.length, BIF_DECODE_OPTIONS)
    }

    private fun findNearestBifEntry(positionMs: Long): BifEntry? {
        if (bifIndex.isEmpty()) return null
        var lo = 0
        var hi = bifIndex.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (bifIndex[mid].positionMs <= positionMs) lo = mid else hi = mid - 1
        }
        return bifIndex[lo]
    }

    private fun snapToBifInterval(positionMs: Long): Long {
        if (bifIndex.isEmpty()) return positionMs
        // BIF interval is determined by the first two entries.
        if (bifIndex.size < 2) return bifIndex[0].positionMs
        val interval = bifIndex[1].positionMs - bifIndex[0].positionMs
        if (interval <= 0L) return positionMs
        return (positionMs / interval) * interval
    }

    // -------------------------------------------------------------------------------------------
    // Local I-Frame fallback via MediaMetadataRetriever
    // -------------------------------------------------------------------------------------------

    private fun extractLocalFrame(positionMs: Long): Bitmap? {
        val uri = localVideoUri ?: return null
        return try {
            val r = getOrCreateRetriever(uri) ?: return null
            // OPTION_CLOSEST_SYNC snaps to the nearest I-Frame without decoding B/P frames,
            // leveraging hardware MediaCodec acceleration where available.
            r.getFrameAtTime(
                positionMs * 1000L, // convert ms to microseconds
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getOrCreateRetriever(uri: String): MediaMetadataRetriever? {
        retriever?.let { return it }
        return try {
            MediaMetadataRetriever().also {
                it.setDataSource(context, android.net.Uri.parse(uri))
                retriever = it
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun releaseRetriever() {
        try {
            retriever?.release()
        } catch (_: Exception) {
        }
        retriever = null
        localVideoUri = null
    }

    // -------------------------------------------------------------------------------------------
    // Network helper
    // -------------------------------------------------------------------------------------------

    private fun downloadBytes(urlString: String): ByteArray? {
        var connection: HttpURLConnection? = null
        var stream: InputStream? = null
        return try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                connect()
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            stream = connection.inputStream
            stream.readBytes()
        } catch (e: Exception) {
            null
        } finally {
            stream?.close()
            connection?.disconnect()
        }
    }

    // -------------------------------------------------------------------------------------------
    // Data classes and constants
    // -------------------------------------------------------------------------------------------

    private data class BifEntry(
        val positionMs: Long,
        val offset: Int,
        val length: Int = 0,
    )

    companion object {
        /** 24 MB LRU cache ceiling. */
        private const val CACHE_SIZE_BYTES: Int = 24 * 1024 * 1024

        /** Default BIF timestamp multiplier when the file reports 0 (1 second per tick). */
        private const val DEFAULT_MULTIPLIER_MS: Long = 1_000L

        private const val CONNECT_TIMEOUT_MS: Int = 5_000
        private const val READ_TIMEOUT_MS: Int = 15_000

        private val BIF_DECODE_OPTIONS = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565 // Halves memory vs ARGB_8888 for thumbnails.
        }
    }
}
