package org.jellyfin.mobile

import android.app.Application
import org.jellyfin.mobile.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class JellyfinApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@JellyfinApp)
            modules(appModule)
        }
    }
}
