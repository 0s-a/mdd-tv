package org.jellyfin.mobile.core.download

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadIndex
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import java.io.File
import java.util.concurrent.Executors

/**
 * DownloadManagerProvider provides thread-safe singletons for Media3 Download management,
 * offline caching, and cache-backed data sources.
 * Strictly adheres to docs/screens/06_DOWNLOADS_OFFLINE.md Sections 3 & 4:
 * - Standalone SQLite database provider for download metadata tracking.
 * - SimpleCache located in external files dir (app-specific storage, no storage permissions required).
 * - CacheDataSource.Factory for seamless offline playback with zero network overhead.
 */
@OptIn(UnstableApi::class)
object DownloadManagerProvider {

    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "jellyfin_downloads"
    const val DOWNLOAD_NOTIFICATION_ID = 1001

    @Volatile
    private var databaseProvider: StandaloneDatabaseProvider? = null

    @Volatile
    private var downloadCache: SimpleCache? = null

    @Volatile
    private var downloadManager: DownloadManager? = null

    @Volatile
    private var downloadNotificationHelper: DownloadNotificationHelper? = null

    fun getDatabaseProvider(context: Context): StandaloneDatabaseProvider {
        return databaseProvider ?: synchronized(this) {
            databaseProvider ?: StandaloneDatabaseProvider(context.applicationContext).also {
                databaseProvider = it
            }
        }
    }

    fun getDownloadCache(context: Context): SimpleCache {
        return downloadCache ?: synchronized(this) {
            downloadCache ?: run {
                val baseDir = context.applicationContext.getExternalFilesDir(null)
                    ?: context.applicationContext.filesDir
                val downloadDirectory = File(baseDir, "downloads")
                if (!downloadDirectory.exists()) {
                    downloadDirectory.mkdirs()
                }
                SimpleCache(
                    downloadDirectory,
                    NoOpCacheEvictor(),
                    getDatabaseProvider(context)
                ).also { downloadCache = it }
            }
        }
    }

    fun getHttpDataSourceFactory(context: Context): DataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent("mdd-tv-android/1.0.0")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
    }

    fun getDownloadManager(context: Context): DownloadManager {
        return downloadManager ?: synchronized(this) {
            downloadManager ?: run {
                val executor = Executors.newFixedThreadPool(4)
                DownloadManager(
                    context.applicationContext,
                    getDatabaseProvider(context),
                    getDownloadCache(context),
                    getHttpDataSourceFactory(context),
                    executor
                ).apply {
                    maxParallelDownloads = 3
                    minRetryCount = 5
                }.also { downloadManager = it }
            }
        }
    }

    fun getDownloadNotificationHelper(context: Context): DownloadNotificationHelper {
        return downloadNotificationHelper ?: synchronized(this) {
            downloadNotificationHelper ?: DownloadNotificationHelper(
                context.applicationContext,
                DOWNLOAD_NOTIFICATION_CHANNEL_ID
            ).also { downloadNotificationHelper = it }
        }
    }

    /**
     * Builds a DataSource.Factory that resolves content from local SimpleCache first.
     * When offline, all downloaded media segments are read directly from disk with zero network requests.
     */
    fun buildCacheDataSourceFactory(context: Context): DataSource.Factory {
        val upstreamFactory = DefaultDataSource.Factory(
            context.applicationContext,
            getHttpDataSourceFactory(context)
        )
        return CacheDataSource.Factory()
            .setCache(getDownloadCache(context))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null) // read-only during playback
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun buildPlaybackDataSourceFactory(context: Context): DataSource.Factory =
        buildCacheDataSourceFactory(context)

    fun getDownloadIndex(context: Context): DownloadIndex {
        return getDownloadManager(context).downloadIndex
    }

    fun isMediaDownloaded(context: Context, mediaId: String): Boolean {
        return try {
            val download = getDownloadIndex(context).getDownload(mediaId)
            download != null && download.state == Download.STATE_COMPLETED
        } catch (e: Exception) {
            false
        }
    }

    fun isMediaDownloadedById(context: Context, mediaId: String): Boolean =
        isMediaDownloaded(context, mediaId)

    fun isUriCached(context: Context, uri: Uri): Boolean {
        return try {
            val cache = getDownloadCache(context)
            val key = uri.toString()
            cache.isCached(key, 0, 1024 * 1024) || cache.isCached(key, 0, C.LENGTH_UNSET.toLong())
        } catch (e: Exception) {
            false
        }
    }

    fun sendAddDownload(context: Context, request: DownloadRequest) {
        DownloadService.sendAddDownload(
            context.applicationContext,
            JellyfinDownloadService::class.java,
            request,
            true
        )
    }

    fun sendRemoveDownload(context: Context, id: String) {
        DownloadService.sendRemoveDownload(
            context.applicationContext,
            JellyfinDownloadService::class.java,
            id,
            false
        )
    }

    fun sendPauseDownloads(context: Context) {
        DownloadService.sendPauseDownloads(
            context.applicationContext,
            JellyfinDownloadService::class.java,
            false
        )
    }

    fun sendResumeDownloads(context: Context) {
        DownloadService.sendResumeDownloads(
            context.applicationContext,
            JellyfinDownloadService::class.java,
            false
        )
    }
}
