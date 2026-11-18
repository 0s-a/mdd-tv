package org.jellyfin.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.data.local.entity.LocalFavoriteEntity

@Dao
interface LocalFavoriteDao {

    @Query("SELECT * FROM local_favorites ORDER BY addedAt DESC")
    fun observeAllFavorites(): Flow<List<LocalFavoriteEntity>>

    @Query("SELECT * FROM local_favorites ORDER BY addedAt DESC")
    suspend fun getAllFavorites(): List<LocalFavoriteEntity>

    @Query("SELECT itemId FROM local_favorites")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT itemId FROM local_favorites")
    suspend fun getFavoriteIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM local_favorites WHERE itemId = :itemId)")
    suspend fun isFavorite(itemId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM local_favorites WHERE itemId = :itemId)")
    fun observeIsFavorite(itemId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: LocalFavoriteEntity)

    @Query("DELETE FROM local_favorites WHERE itemId = :itemId")
    suspend fun deleteFavorite(itemId: String)
}
