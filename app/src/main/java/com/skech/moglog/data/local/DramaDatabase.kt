package com.skech.moglog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DramaEntity::class,
        ShortcutEntity::class,
        DramaDetailCacheEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class DramaDatabase : RoomDatabase() {
    abstract val dao: DramaDao
    abstract val shortcutDao: ShortcutDao
    abstract val detailCacheDao: DramaDetailCacheDao
}
