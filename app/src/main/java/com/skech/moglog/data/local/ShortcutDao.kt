package com.skech.moglog.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY id DESC")
    fun getAllShortcuts(): Flow<List<ShortcutEntity>>

    @Query("SELECT * FROM shortcuts WHERE dramaId = :dramaId LIMIT 1")
    suspend fun getShortcutForDrama(dramaId: Int): ShortcutEntity?

    @Query("DELETE FROM shortcuts WHERE dramaId = :dramaId")
    suspend fun deleteShortcutsForDrama(dramaId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: ShortcutEntity)

    @Delete
    suspend fun deleteShortcut(shortcut: ShortcutEntity)
}
