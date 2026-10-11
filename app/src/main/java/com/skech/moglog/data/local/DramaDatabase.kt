package com.skech.moglog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DramaEntity::class, ShortcutEntity::class], version = 6, exportSchema = false)
abstract class DramaDatabase : RoomDatabase() {
    abstract val dao: DramaDao
    abstract val shortcutDao: ShortcutDao
}
