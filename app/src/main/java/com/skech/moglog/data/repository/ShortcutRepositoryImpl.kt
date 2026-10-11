package com.skech.moglog.data.repository

import com.skech.moglog.data.local.ShortcutDao
import com.skech.moglog.data.local.toDomainModel
import com.skech.moglog.data.local.toEntity
import com.skech.moglog.domain.model.Shortcut
import com.skech.moglog.domain.repository.ShortcutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShortcutRepositoryImpl(
    private val dao: ShortcutDao
) : ShortcutRepository {

    override fun getAllShortcuts(): Flow<List<Shortcut>> {
        return dao.getAllShortcuts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun saveShortcut(shortcut: Shortcut) {
        dao.insertShortcut(shortcut.toEntity())
    }

    override suspend fun deleteShortcut(shortcut: Shortcut) {
        dao.deleteShortcut(shortcut.toEntity())
    }
}
