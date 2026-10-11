package com.skech.moglog.domain.repository

import com.skech.moglog.domain.model.Shortcut
import kotlinx.coroutines.flow.Flow

interface ShortcutRepository {
    fun getAllShortcuts(): Flow<List<Shortcut>>
    suspend fun saveShortcut(shortcut: Shortcut)
    suspend fun deleteShortcut(shortcut: Shortcut)
}
