package org.jellyfin.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.jellyfin.mobile.data.local.dao.LocalFavoriteDao
import org.jellyfin.mobile.data.local.dao.LocalWatchProgressDao
import org.jellyfin.mobile.data.local.dao.SplashPosterDao
import org.jellyfin.mobile.data.local.dao.UserProfileDao
import org.jellyfin.mobile.data.local.dao.WatchedArchiveDao
import org.jellyfin.mobile.data.local.entity.LocalFavoriteEntity
import org.jellyfin.mobile.data.local.entity.LocalWatchProgressEntity
import org.jellyfin.mobile.data.local.entity.SplashPosterEntity
import org.jellyfin.mobile.data.local.entity.UserProfileEntity
import org.jellyfin.mobile.data.local.entity.WatchedArchiveEntity

/**
 * Main Room Database for MDD TV local-first storage and profile isolation.
 * Version 4: Added splash_posters cache for dynamic cinema splash screen.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        LocalFavoriteEntity::class,
        WatchedArchiveEntity::class,
        LocalWatchProgressEntity::class,
        SplashPosterEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun localFavoriteDao(): LocalFavoriteDao
    abstract fun watchedArchiveDao(): WatchedArchiveDao
    abstract fun localWatchProgressDao(): LocalWatchProgressDao
    abstract fun splashPosterDao(): SplashPosterDao

    companion object {
        const val DATABASE_NAME = "jellyfin_mdd_tv.db"
    }
}
