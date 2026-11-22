package org.jellyfin.mobile

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import org.jellyfin.mobile.core.sync.OfflineSyncCoordinator
import org.jellyfin.mobile.di.appModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class JellyfinApp : Application(), ImageLoaderFactory {

    private val offlineSyncCoordinator: OfflineSyncCoordinator by inject()
    private val imageLoader: ImageLoader by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@JellyfinApp)
            modules(appModule)
        }

        offlineSyncCoordinator.start()
    }

    override fun newImageLoader(): ImageLoader = imageLoader
}
