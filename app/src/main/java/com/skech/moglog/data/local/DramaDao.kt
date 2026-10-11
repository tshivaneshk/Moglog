package com.skech.moglog.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DramaDao {
    @Query("SELECT * FROM dramas")
    fun getAllDramas(): Flow<List<DramaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrama(drama: DramaEntity)

    @Query("UPDATE dramas SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Delete
    suspend fun deleteDrama(drama: DramaEntity)

    @Query("DELETE FROM dramas")
    suspend fun clearAllDramas()
}
