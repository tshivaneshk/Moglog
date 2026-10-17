package com.skech.moglog.domain.repository

import com.skech.moglog.domain.model.Drama
import com.skech.moglog.domain.model.DramaExtraDetails
import kotlinx.coroutines.flow.Flow

interface DramaRepository {
    fun getAllDramas(): Flow<List<Drama>>
    suspend fun getLocalDramaById(id: Int): Drama?
    suspend fun searchDramas(query: String): List<Drama>
    suspend fun getTrendingDramas(): List<Drama>
    suspend fun getPopularDramas(): List<Drama>
    suspend fun getAiringTodayDramas(): List<Drama>
    suspend fun getOnTheAirDramas(): List<Drama>
    suspend fun getDramaDetails(id: Int): Drama?
    suspend fun getDramaExtraDetails(id: Int): DramaExtraDetails?
    suspend fun saveDrama(drama: Drama)
    suspend fun updateDramaStatus(id: Int, status: String)
    suspend fun deleteDrama(drama: Drama)
    suspend fun clearAllDramas()
    suspend fun clearMetadataCache()
}
