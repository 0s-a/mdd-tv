package org.jellyfin.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.jellyfin.mobile.data.local.dao.DownloadedMediaDao
import org.jellyfin.mobile.data.local.dao.LocalFavoriteDao
import org.jellyfin.mobile.data.local.dao.LocalWatchProgressDao
import org.jellyfin.mobile.data.local.dao.UserProfileDao
import org.jellyfin.mobile.data.local.dao.WatchedArchiveDao
import org.jellyfin.mobile.data.local.entity.DownloadedMediaEntity
import org.jellyfin.mobile.data.local.entity.LocalFavoriteEntity
import org.jellyfin.mobile.data.local.entity.LocalWatchProgressEntity
import org.jellyfin.mobile.data.local.entity.UserProfileEntity
import org.jellyfin.mobile.data.local.entity.WatchedArchiveEntity

/**
 * Main Room Database for MDD TV offline storage and local caching.
 * Upgraded to Version 2 to support Local-First Profiles, Favorites, Archive, and Watch Progress.
 */
@Database(
    entities = [
        DownloadedMediaEntity::class,
        UserProfileEntity::class,
        LocalFavoriteEntity::class,
        WatchedArchiveEntity::class,
        LocalWatchProgressEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadedMediaDao(): DownloadedMediaDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun localFavoriteDao(): LocalFavoriteDao
    abstract fun watchedArchiveDao(): WatchedArchiveDao
    abstract fun localWatchProgressDao(): LocalWatchProgressDao

    companion object {
        const val DATABASE_NAME = "jellyfin_mdd_tv.db"
    }
}
