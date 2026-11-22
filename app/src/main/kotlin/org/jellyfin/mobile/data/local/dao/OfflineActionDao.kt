package org.jellyfin.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.data.local.entity.OfflineActionEntity
import org.jellyfin.mobile.data.local.entity.OfflineActionType

/**
 * Data Access Object for managing queued offline actions.
 * Provides insertion, retrieval, targeted compaction, and cleanup routines.
 */
@Dao
interface OfflineActionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: OfflineActionEntity): Long

    @Query("SELECT * FROM offline_actions ORDER BY createdAt ASC")
    suspend fun getAllPendingActions(): List<OfflineActionEntity>

    @Query("SELECT COUNT(*) FROM offline_actions")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM offline_actions WHERE actionId = :actionId")
    suspend fun deleteAction(actionId: Long)

    @Query("DELETE FROM offline_actions WHERE itemId = :itemId AND actionType = :actionType")
    suspend fun deleteActionsForItemAndType(itemId: String, actionType: OfflineActionType)

    @Query("DELETE FROM offline_actions WHERE itemId = :itemId AND (actionType = 'MARK_FAVORITE' OR actionType = 'REMOVE_FAVORITE')")
    suspend fun compactFavoriteActions(itemId: String)

    @Query("DELETE FROM offline_actions WHERE itemId = :itemId AND (actionType = 'MARK_WATCHED' OR actionType = 'MARK_UNWATCHED')")
    suspend fun compactWatchedActions(itemId: String)

    @Query("DELETE FROM offline_actions WHERE itemId = :itemId AND actionType = 'UPDATE_PROGRESS'")
    suspend fun compactProgressActions(itemId: String)

    @Query("DELETE FROM offline_actions")
    suspend fun clearAll()
}
