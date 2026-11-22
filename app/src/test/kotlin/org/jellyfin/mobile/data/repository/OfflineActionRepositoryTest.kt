package org.jellyfin.mobile.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jellyfin.mobile.core.network.SessionManager
import org.jellyfin.mobile.data.local.dao.OfflineActionDao
import org.jellyfin.mobile.data.local.entity.OfflineActionEntity
import org.jellyfin.mobile.data.local.entity.OfflineActionType
import org.jellyfin.mobile.data.remote.api.JellyfinApiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineActionRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeOfflineActionDao
    private lateinit var sessionManager: SessionManager
    private val markedFavorites = mutableListOf<String>()
    private val markedPlayed = mutableListOf<String>()
    private var shouldThrowError = false

    private lateinit var apiService: JellyfinApiService
    private lateinit var repository: OfflineActionRepositoryImpl

    @Before
    fun setup() {
        fakeDao = FakeOfflineActionDao()
        sessionManager = SessionManager()
        sessionManager.saveSession("token-123", "user-456")
        markedFavorites.clear()
        markedPlayed.clear()
        shouldThrowError = false

        apiService = Proxy.newProxyInstance(
            JellyfinApiService::class.java.classLoader,
            arrayOf(JellyfinApiService::class.java)
        ) { _, method, args ->
            if (shouldThrowError) {
                throw java.io.IOException("Simulated network outage")
            }
            when (method.name) {
                "markFavorite" -> {
                    markedFavorites.add(args[1] as String)
                    null
                }
                "markPlayed" -> {
                    markedPlayed.add(args[1] as String)
                    null
                }
                else -> null
            }
        } as JellyfinApiService

        repository = OfflineActionRepositoryImpl(
            offlineActionDao = fakeDao,
            apiService = apiService,
            sessionManager = sessionManager,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun enqueueAction_insertsActionAndCompactsConflicting() = runTest(testDispatcher) {
        repository.enqueueAction("item-1", OfflineActionType.MARK_FAVORITE)
        assertEquals(1, fakeDao.actions.size)
        assertEquals(OfflineActionType.MARK_FAVORITE, fakeDao.actions[0].actionType)

        // Enqueue contradictory action for same item -> compaction should leave only REMOVE_FAVORITE
        repository.enqueueAction("item-1", OfflineActionType.REMOVE_FAVORITE)
        assertEquals(1, fakeDao.actions.size)
        assertEquals(OfflineActionType.REMOVE_FAVORITE, fakeDao.actions[0].actionType)
    }

    @Test
    fun syncPendingActions_callsApiServiceAndDeletesActions() = runTest(testDispatcher) {
        repository.enqueueAction("item-1", OfflineActionType.MARK_FAVORITE)
        repository.enqueueAction("item-2", OfflineActionType.MARK_WATCHED)

        assertEquals(2, fakeDao.actions.size)

        val result = repository.syncPendingActions()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        assertEquals(listOf("item-1"), markedFavorites)
        assertEquals(listOf("item-2"), markedPlayed)
        assertEquals(0, fakeDao.actions.size)
    }

    @Test
    fun syncPendingActions_abortsOnNetworkErrorAndPreservesRemaining() = runTest(testDispatcher) {
        repository.enqueueAction("item-1", OfflineActionType.MARK_FAVORITE)
        repository.enqueueAction("item-2", OfflineActionType.MARK_WATCHED)

        shouldThrowError = true

        val result = repository.syncPendingActions()
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())

        // Both items should be preserved in the queue
        assertEquals(2, fakeDao.actions.size)
    }

    @Test
    fun observePendingCount_reflectsQueuedItems() = runTest(testDispatcher) {
        repository.enqueueAction("item-1", OfflineActionType.MARK_FAVORITE)
        val count = repository.observePendingCount().first()
        assertEquals(1, count)
    }

    private class FakeOfflineActionDao : OfflineActionDao {
        val actions = mutableListOf<OfflineActionEntity>()
        private var nextId = 1L
        private val countFlow = MutableStateFlow(0)

        override suspend fun insertAction(action: OfflineActionEntity): Long {
            val id = nextId++
            val saved = action.copy(actionId = id)
            actions.add(saved)
            countFlow.value = actions.size
            return id
        }

        override suspend fun getAllPendingActions(): List<OfflineActionEntity> = actions.toList()

        override fun observePendingCount(): Flow<Int> = countFlow

        override suspend fun deleteAction(actionId: Long) {
            actions.removeAll { it.actionId == actionId }
            countFlow.value = actions.size
        }

        override suspend fun deleteActionsForItemAndType(itemId: String, actionType: OfflineActionType) {
            actions.removeAll { it.itemId == itemId && it.actionType == actionType }
            countFlow.value = actions.size
        }

        override suspend fun compactFavoriteActions(itemId: String) {
            actions.removeAll {
                it.itemId == itemId && (it.actionType == OfflineActionType.MARK_FAVORITE || it.actionType == OfflineActionType.REMOVE_FAVORITE)
            }
            countFlow.value = actions.size
        }

        override suspend fun compactWatchedActions(itemId: String) {
            actions.removeAll {
                it.itemId == itemId && (it.actionType == OfflineActionType.MARK_WATCHED || it.actionType == OfflineActionType.MARK_UNWATCHED)
            }
            countFlow.value = actions.size
        }

        override suspend fun compactProgressActions(itemId: String) {
            actions.removeAll { it.itemId == itemId && it.actionType == OfflineActionType.UPDATE_PROGRESS }
            countFlow.value = actions.size
        }

        override suspend fun clearAll() {
            actions.clear()
            countFlow.value = 0
        }
    }
}
