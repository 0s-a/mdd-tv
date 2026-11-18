package org.jellyfin.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing the local-first user profile.
 * Exactly adheres to docs/v2/ANTI_HALLUCINATION_MASTER_PLAN.md.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "default_user",
    val displayName: String,
    val avatarKey: String, // "avatar_1" to "avatar_8"
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
