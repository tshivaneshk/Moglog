package com.skech.moglog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skech.moglog.domain.model.Shortcut

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val data: String,
    val type: String,
    val iconUrl: String? = null,
    val dramaId: Int = 0
)

fun ShortcutEntity.toDomainModel(): Shortcut {
    return Shortcut(id, title, data, type, iconUrl, dramaId)
}

fun Shortcut.toEntity(): ShortcutEntity {
    return ShortcutEntity(id, title, data, type, iconUrl, dramaId)
}
