package org.jellyfin.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.data.local.entity.LocalWatchProgressEntity

@Dao
interface LocalWatchProgressDao {

    @Query("SELECT * FROM local_watch_progress WHERE isPlayed = 0 ORDER BY updatedAt DESC")
    fun observeContinueWatching(): Flow<List<LocalWatchProgressEntity>>

    @Query("SELECT * FROM local_watch_progress WHERE isPlayed = 0 ORDER BY updatedAt DESC")
    suspend fun getContinueWatching(): List<LocalWatchProgressEntity>

    @Query("SELECT * FROM local_watch_progress WHERE itemId = :itemId LIMIT 1")
    suspend fun getProgress(itemId: String): LocalWatchProgressEntity?

    @Query("SELECT * FROM local_watch_progress WHERE itemId = :itemId LIMIT 1")
    fun observeProgress(itemId: String): Flow<LocalWatchProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: LocalWatchProgressEntity)

    @Query("DELETE FROM local_watch_progress WHERE itemId = :itemId")
    suspend fun deleteProgress(itemId: String)

    @Query("UPDATE local_watch_progress SET isPlayed = 1 WHERE itemId = :itemId")
    suspend fun markPlayed(itemId: String)
}
