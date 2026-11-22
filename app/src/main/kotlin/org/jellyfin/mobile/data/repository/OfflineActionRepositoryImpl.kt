package org.jellyfin.mobile.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.local.dao.OfflineActionDao
import org.jellyfin.mobile.data.local.entity.OfflineActionEntity
import org.jellyfin.mobile.data.local.entity.OfflineActionType
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.jellyfin.mobile.domain.repository.OfflineActionRepository

/**
 * Robust implementation of OfflineActionRepository with intelligent event compaction
 * and resilient queue flushing against Jellyfin server API endpoints.
 */
class OfflineActionRepositoryImpl(
    private val offlineActionDao: OfflineActionDao,
    private val apiService: JellyfinApiService,
    private val sessionManager: SessionManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : OfflineActionRepository {

    override suspend fun enqueueAction(
        itemId: String,
        actionType: OfflineActionType,
        payload: String
    ): Long = withContext(ioDispatcher) {
        // Event Compaction: remove prior contradicting/stale actions for the same item
        when (actionType) {
            OfflineActionType.MARK_FAVORITE, OfflineActionType.REMOVE_FAVORITE -> {
                offlineActionDao.compactFavoriteActions(itemId)
            }
            OfflineActionType.MARK_WATCHED, OfflineActionType.MARK_UNWATCHED -> {
                offlineActionDao.compactWatchedActions(itemId)
            }
            OfflineActionType.UPDATE_PROGRESS -> {
                offlineActionDao.compactProgressActions(itemId)
            }
        }

        val entity = OfflineActionEntity(
            itemId = itemId,
            actionType = actionType,
            payload = payload,
            createdAt = System.currentTimeMillis()
        )
        offlineActionDao.insertAction(entity)
    }

    override suspend fun getAllPendingActions(): List<OfflineActionEntity> = withContext(ioDispatcher) {
        offlineActionDao.getAllPendingActions()
    }

    override fun observePendingCount(): Flow<Int> {
        return offlineActionDao.observePendingCount()
    }

    override suspend fun deleteAction(actionId: Long) = withContext(ioDispatcher) {
        offlineActionDao.deleteAction(actionId)
    }

    override suspend fun removeAction(itemId: String, actionType: OfflineActionType) = withContext(ioDispatcher) {
        offlineActionDao.deleteActionsForItemAndType(itemId, actionType)
    }

    override suspend fun syncPendingActions(): Result<Int> = withContext(ioDispatcher) {
        runCatching {
            val userId = sessionManager.getUserId()
            if (userId.isNullOrBlank()) {
                return@runCatching 0
            }

            val pendingActions = offlineActionDao.getAllPendingActions()
            if (pendingActions.isEmpty()) {
                return@runCatching 0
            }

            var syncedCount = 0

            for (action in pendingActions) {
                try {
                    when (action.actionType) {
                        OfflineActionType.MARK_FAVORITE -> {
                            apiService.markFavorite(userId = userId, itemId = action.itemId)
                        }
                        OfflineActionType.REMOVE_FAVORITE -> {
                            apiService.removeFavorite(userId = userId, itemId = action.itemId)
                        }
                        OfflineActionType.MARK_WATCHED -> {
                            apiService.markPlayed(userId = userId, itemId = action.itemId)
                        }
                        OfflineActionType.MARK_UNWATCHED -> {
                            apiService.markUnplayed(userId = userId, itemId = action.itemId)
                        }
                        OfflineActionType.UPDATE_PROGRESS -> {
                            // Progress is preserved in local Room DB per ADR 0001
                        }
                    }
                    offlineActionDao.deleteAction(action.actionId)
                    syncedCount++
                } catch (e: Exception) {
                    // Abort sync loop upon network failure to preserve remaining actions in FIFO order
                    break
                }
            }

            syncedCount
        }
    }
}
