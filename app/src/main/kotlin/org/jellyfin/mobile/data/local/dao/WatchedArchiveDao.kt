package org.jellyfin.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.data.local.entity.WatchedArchiveEntity

@Dao
interface WatchedArchiveDao {

    @Query("SELECT * FROM watched_archive ORDER BY completedAt DESC")
    fun observeAllArchive(): Flow<List<WatchedArchiveEntity>>

    @Query("SELECT * FROM watched_archive ORDER BY completedAt DESC")
    suspend fun getAllArchive(): List<WatchedArchiveEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM watched_archive WHERE itemId = :itemId)")
    suspend fun isWatched(itemId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM watched_archive WHERE itemId = :itemId)")
    fun observeIsWatched(itemId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateArchive(item: WatchedArchiveEntity)

    @Query("DELETE FROM watched_archive WHERE itemId = :itemId")
    suspend fun deleteFromArchive(itemId: String)
}
