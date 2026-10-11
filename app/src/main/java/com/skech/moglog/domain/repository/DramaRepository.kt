package com.skech.moglog.domain.repository

import com.skech.moglog.domain.model.Drama
import kotlinx.coroutines.flow.Flow

interface DramaRepository {
    fun getAllDramas(): Flow<List<Drama>>
    suspend fun searchDramas(query: String): List<Drama>

    suspend fun getDramaDetails(id: Int): Drama?
    suspend fun getDramaExtraDetails(id: Int): com.skech.moglog.domain.model.DramaExtraDetails?
    suspend fun saveDrama(drama: Drama)
    suspend fun updateDramaStatus(id: Int, status: String)
    suspend fun deleteDrama(drama: Drama)
    suspend fun clearAllDramas()
}
