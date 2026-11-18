package org.jellyfin.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.jellyfin.mobile.core.download.DownloadManagerProvider
import org.jellyfin.mobile.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class JellyfinApp : Application() {
    override fun onCreate() {
        super.onCreate()

        createNotificationChannels()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@JellyfinApp)
            modules(appModule)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DownloadManagerProvider.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
                "تنزيلات مدد TV",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تقدم التنزيل للمشاهدة بدون إنترنت"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
