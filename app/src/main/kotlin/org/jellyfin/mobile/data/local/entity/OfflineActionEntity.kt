package org.jellyfin.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an action queued while offline or during unstable network conditions.
 * Enables zero-latency local optimistic updates and resilient background synchronization.
 */
@Entity(
    tableName = "offline_actions",
    indices = [
        Index(value = ["itemId", "actionType"]),
        Index(value = ["createdAt"])
    ]
)
data class OfflineActionEntity(
    @PrimaryKey(autoGenerate = true)
    val actionId: Long = 0,
    val itemId: String,
    val actionType: OfflineActionType,
    val payload: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class OfflineActionType {
    MARK_FAVORITE,
    REMOVE_FAVORITE,
    MARK_WATCHED,
    MARK_UNWATCHED,
    UPDATE_PROGRESS
}
