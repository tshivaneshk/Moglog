package com.skech.moglog.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DramaDao {
    @Query("SELECT * FROM dramas")
    fun getAllDramas(): Flow<List<DramaEntity>>

    @Query("SELECT * FROM dramas WHERE id = :id LIMIT 1")
    suspend fun getDramaById(id: Int): DramaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrama(drama: DramaEntity)

    @Query("UPDATE dramas SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteDrama(drama: DramaEntity)

    @Query("DELETE FROM dramas WHERE isDeleted = 1 AND deletedAt > 0 AND deletedAt < :cutoffTimestamp")
    suspend fun purgeDeletedBefore(cutoffTimestamp: Long)

    @Query("DELETE FROM dramas WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()

    @Query("DELETE FROM dramas")
    suspend fun clearAllDramas()
}
