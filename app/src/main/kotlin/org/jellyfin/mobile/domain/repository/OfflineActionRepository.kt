package org.jellyfin.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.data.local.entity.OfflineActionEntity
import org.jellyfin.mobile.data.local.entity.OfflineActionType

/**
 * Repository interface governing offline action queuing, compaction, and synchronization.
 */
interface OfflineActionRepository {

    suspend fun enqueueAction(
        itemId: String,
        actionType: OfflineActionType,
        payload: String = ""
    ): Long

    suspend fun getAllPendingActions(): List<OfflineActionEntity>

    fun observePendingCount(): Flow<Int>

    suspend fun deleteAction(actionId: Long)

    suspend fun removeAction(itemId: String, actionType: OfflineActionType)

    suspend fun syncPendingActions(): Result<Int>
}
