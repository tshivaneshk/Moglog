package com.skech.moglog.data.repository

import com.google.gson.Gson
import com.skech.moglog.data.local.ApiKeyManager
import com.skech.moglog.data.local.DramaDao
import com.skech.moglog.data.local.toDomainModel
import com.skech.moglog.data.local.toEntity
import com.skech.moglog.data.remote.TmdbApi
import com.skech.moglog.domain.model.CastMember
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.domain.model.DramaExtraDetails
import com.skech.moglog.domain.repository.DramaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
            api.searchDramas(query = query).results.mapNotNull { dto ->
                if (dto.name.isNullOrBlank()) return@mapNotNull null
                Drama(
                    id = dto.id,
                    title = dto.name,
                    overview = dto.overview ?: "",
                    rating = dto.voteAverage ?: 0.0,
                    posterPath = dto.posterPath?.let { "${TmdbApi.IMAGE_BASE_URL}$it" } ?: ""
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getDramaDetails(id: Int): Drama? {
        return try {
            val dto = api.getDramaDetails(id = id)
            if (dto.name.isNullOrBlank()) return null
            Drama(
                id = dto.id,
                title = dto.name,
                overview = dto.overview ?: "",
                rating = dto.voteAverage ?: 0.0,
                posterPath = dto.posterPath?.let { "${TmdbApi.IMAGE_BASE_URL}$it" } ?: "",
                totalEpisodes = dto.numberOfEpisodes ?: 0
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getDramaExtraDetails(id: Int): DramaExtraDetails? {
        val cached = apiKeyManager.getExtraDetails(id)
        if (cached != null) {
            try {
                return Gson().fromJson(cached, DramaExtraDetails::class.java)
            } catch (_: Exception) {
            }
        }
        return try {
            val dto = api.getDramaDetails(id = id)
            val trailerKey = dto.videos?.results?.firstOrNull { it.type == "Trailer" && it.site == "YouTube" }?.key
            val cast = dto.credits?.cast?.take(10)?.map {
                CastMember(
                    name = it.name ?: "",
                    character = it.character ?: "",
                    profilePath = it.profilePath?.let { path -> "${TmdbApi.IMAGE_BASE_URL}$path" }
                )
            } ?: emptyList()
            val details = DramaExtraDetails(trailerKey, cast)
            apiKeyManager.setExtraDetails(id, Gson().toJson(details))
            details
        } catch (_: Exception) {
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
