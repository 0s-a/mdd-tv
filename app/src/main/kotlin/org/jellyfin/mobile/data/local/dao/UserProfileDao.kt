package org.jellyfin.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.data.local.entity.UserProfileEntity

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String = "default_user"): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun observeProfile(id: String = "default_user"): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("SELECT COUNT(*) > 0 FROM user_profile WHERE id = :id")
    suspend fun hasProfile(id: String = "default_user"): Boolean

    @Query("UPDATE user_profile SET displayName = :displayName, avatarKey = :avatarKey, lastUpdatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProfile(
        id: String = "default_user",
        displayName: String,
        avatarKey: String,
        updatedAt: Long = System.currentTimeMillis()
    )
}
