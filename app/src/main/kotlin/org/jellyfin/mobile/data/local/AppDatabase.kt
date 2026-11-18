package org.jellyfin.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.jellyfin.mobile.data.local.dao.DownloadedMediaDao
import org.jellyfin.mobile.data.local.entity.DownloadedMediaEntity

/**
 * Main Room Database for MDD TV offline storage and local caching.
 */
@Database(
    entities = [DownloadedMediaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadedMediaDao(): DownloadedMediaDao

    companion object {
        const val DATABASE_NAME = "jellyfin_mdd_tv.db"
    }
}
