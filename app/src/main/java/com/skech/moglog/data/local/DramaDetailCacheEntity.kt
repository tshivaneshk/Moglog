package com.skech.moglog.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "drama_details_cache")
data class DramaDetailCacheEntity(
    @PrimaryKey val dramaId: Int,
    val payloadJson: String,
    val cachedAt: Long
)

@Dao
interface DramaDetailCacheDao {
    @Query("SELECT * FROM drama_details_cache WHERE dramaId = :dramaId LIMIT 1")
    suspend fun getCache(dramaId: Int): DramaDetailCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCache(entity: DramaDetailCacheEntity)

    @Query("DELETE FROM drama_details_cache")
    suspend fun clearAll()
}
