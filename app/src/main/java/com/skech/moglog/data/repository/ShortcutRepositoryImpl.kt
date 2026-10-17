package com.skech.moglog.data.repository

import com.google.gson.Gson
import com.skech.moglog.data.local.ApiKeyManager
import com.skech.moglog.data.local.ShortcutDao
import com.skech.moglog.data.local.toDomainModel
import com.skech.moglog.data.local.toEntity
import com.skech.moglog.domain.model.Shortcut
import com.skech.moglog.domain.repository.ShortcutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShortcutRepositoryImpl(
    private val dao: ShortcutDao,
    private val apiKeyManager: ApiKeyManager
) : ShortcutRepository {

    override fun getAllShortcuts(): Flow<List<Shortcut>> {
        return dao.getAllShortcuts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getShortcutForDrama(dramaId: Int): Shortcut? {
        val fromDb = dao.getShortcutForDrama(dramaId)?.toDomainModel()
        if (fromDb != null) return fromDb

        // One-time migration from legacy SharedPreferences if present
        val legacyJson = apiKeyManager.getShortcut(dramaId)
        if (!legacyJson.isNullOrBlank()) {
            return try {
                val parsed = Gson().fromJson(legacyJson, Shortcut::class.java).copy(dramaId = dramaId)
                dao.insertShortcut(parsed.toEntity())
                apiKeyManager.setShortcut(dramaId, null)
                parsed
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    override suspend fun saveShortcutForDrama(dramaId: Int, shortcut: Shortcut?) {
        dao.deleteShortcutsForDrama(dramaId)
        apiKeyManager.setShortcut(dramaId, null)
        if (shortcut != null) {
            dao.insertShortcut(shortcut.copy(dramaId = dramaId).toEntity())
        }
    }

    override suspend fun saveShortcut(shortcut: Shortcut) {
        dao.insertShortcut(shortcut.toEntity())
    }

    override suspend fun deleteShortcut(shortcut: Shortcut) {
        dao.deleteShortcut(shortcut.toEntity())
    }
}
