package org.jellyfin.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.jellyfin.mobile.data.local.entity.SplashPosterEntity

@Dao
interface SplashPosterDao {

    @Query("SELECT * FROM splash_posters ORDER BY orderIndex ASC")
    fun observePosters(): Flow<List<SplashPosterEntity>>

    @Query("SELECT * FROM splash_posters ORDER BY orderIndex ASC")
    suspend fun getPosters(): List<SplashPosterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosters(posters: List<SplashPosterEntity>)

    @Query("DELETE FROM splash_posters")
    suspend fun clearPosters()

    @Transaction
    suspend fun replacePosters(posters: List<SplashPosterEntity>) {
        clearPosters()
        insertPosters(posters)
    }
}
