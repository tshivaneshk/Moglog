package com.skech.moglog.data.repository

import com.skech.moglog.data.local.DramaDao
import com.skech.moglog.data.local.toDomainModel
import com.skech.moglog.data.local.toEntity
import com.skech.moglog.data.remote.TmdbApi
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.domain.repository.DramaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson

import com.skech.moglog.data.local.ApiKeyManager

class DramaRepositoryImpl(
    private val api: TmdbApi,
    private val dao: DramaDao,
    private val apiKeyManager: ApiKeyManager
) : DramaRepository {

    override fun getAllDramas(): Flow<List<Drama>> {
        return dao.getAllDramas().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun searchDramas(query: String): List<Drama> {
        return try {
            api.searchDramas(query = query, apiKey = apiKeyManager.getApiKey()).results.mapNotNull { dto ->
                if (dto.name.isNullOrBlank()) return@mapNotNull null
                Drama(
                    id = dto.id,
                    title = dto.name,
                    overview = dto.overview ?: "",
                    rating = dto.voteAverage ?: 0.0,
                    posterPath = dto.posterPath?.let { "${TmdbApi.IMAGE_BASE_URL}$it" } ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }



    override suspend fun getDramaDetails(id: Int): Drama? {
        return try {
            val dto = api.getDramaDetails(id = id, apiKey = apiKeyManager.getApiKey())
            if (dto.name.isNullOrBlank()) return null
            Drama(
                id = dto.id,
                title = dto.name,
                overview = dto.overview ?: "",
                rating = dto.voteAverage ?: 0.0,
                posterPath = dto.posterPath?.let { "${TmdbApi.IMAGE_BASE_URL}$it" } ?: "",
                totalEpisodes = dto.numberOfEpisodes ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getDramaExtraDetails(id: Int): com.skech.moglog.domain.model.DramaExtraDetails? {
        val cached = apiKeyManager.getExtraDetails(id)
        if (cached != null) {
            try {
                return Gson().fromJson(cached, com.skech.moglog.domain.model.DramaExtraDetails::class.java)
            } catch (e: Exception) {
                // Ignore and fetch fresh if parsing fails
            }
        }
        return try {
            val dto = api.getDramaDetails(id = id, apiKey = apiKeyManager.getApiKey())
            val trailerKey = dto.videos?.results?.firstOrNull { it.type == "Trailer" && it.site == "YouTube" }?.key
            val cast = dto.credits?.cast?.take(10)?.map {
                com.skech.moglog.domain.model.CastMember(
                    name = it.name ?: "",
                    character = it.character ?: "",
                    profilePath = it.profilePath?.let { path -> "${TmdbApi.IMAGE_BASE_URL}$path" }
                )
            } ?: emptyList()
            val details = com.skech.moglog.domain.model.DramaExtraDetails(trailerKey, cast)
            apiKeyManager.setExtraDetails(id, Gson().toJson(details))
            details
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveDrama(drama: Drama) {
        dao.insertDrama(drama.toEntity())
    }

    override suspend fun updateDramaStatus(id: Int, status: String) {
        dao.updateStatus(id, status)
    }

    override suspend fun deleteDrama(drama: Drama) {
        dao.deleteDrama(drama.toEntity())
    }

    override suspend fun clearAllDramas() {
        dao.clearAllDramas()
    }
}
