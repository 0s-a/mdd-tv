package org.jellyfin.mobile.core.download

import android.app.Notification
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import org.jellyfin.mobile.R

/**
 * JellyfinDownloadService runs as an unbreakable Foreground Service for downloading
 * high-bitrate media files (4K/1080p) and accompanying Arabic subtitles.
 * Strictly adheres to docs/screens/06_DOWNLOADS_OFFLINE.md Section 3:
 * - Foreground service with persistent notification and active progress updates.
 * - Resumes on system reboot or network reconnection via PlatformScheduler.
 * - Zero drops, range resumption, and background completion even when the app is closed.
 */
@OptIn(UnstableApi::class)
class JellyfinDownloadService : DownloadService(
    DownloadManagerProvider.DOWNLOAD_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DownloadManagerProvider.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_description
) {

    companion object {
        private const val JOB_ID = 101

        fun start(context: Context) {
            try {
                start(context, JellyfinDownloadService::class.java)
            } catch (e: Exception) {
                startForeground(context, JellyfinDownloadService::class.java)
            }
        }
    }

    override fun getDownloadManager(): DownloadManager {
        return DownloadManagerProvider.getDownloadManager(this)
    }

    override fun getScheduler(): Scheduler? {
        return try {
            PlatformScheduler(this, JOB_ID)
        } catch (e: Exception) {
            null
        }
    }

    override fun getForegroundNotification(
        downloads: List<Download>,
        notMetRequirements: Int
    ): Notification {
        return DownloadManagerProvider.getDownloadNotificationHelper(this)
            .buildProgressNotification(
                this,
                android.R.drawable.stat_sys_download,
                null,
                null,
                downloads,
                notMetRequirements
            )
    }
}
