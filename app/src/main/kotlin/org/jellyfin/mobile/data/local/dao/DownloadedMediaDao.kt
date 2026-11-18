package org.jellyfin.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.data.local.entity.DownloadedMediaEntity

/**
 * Data Access Object for local downloaded media transactions.
 */
@Dao
interface DownloadedMediaDao {

    @Query("SELECT * FROM downloaded_media ORDER BY downloadedAt DESC")
    fun getDownloadedItems(): Flow<List<DownloadedMediaEntity>>

    @Query("SELECT * FROM downloaded_media WHERE id = :id LIMIT 1")
    suspend fun getDownloadedItemById(id: String): DownloadedMediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: DownloadedMediaEntity)

    @Query("DELETE FROM downloaded_media WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM downloaded_media")
    suspend fun clearAll()

    @Query("SELECT SUM(totalSizeBytes) FROM downloaded_media")
    suspend fun getTotalStorageBytes(): Long?

    @Query("SELECT SUM(totalSizeBytes) FROM downloaded_media")
    fun observeTotalStorageBytes(): Flow<Long?>
}
