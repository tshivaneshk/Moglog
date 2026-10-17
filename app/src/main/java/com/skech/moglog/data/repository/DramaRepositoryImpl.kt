package com.skech.moglog.data.repository

import com.google.gson.Gson
import com.skech.moglog.data.local.ApiKeyManager
import com.skech.moglog.data.local.DramaDao
import com.skech.moglog.data.local.DramaDetailCacheDao
import com.skech.moglog.data.local.DramaDetailCacheEntity
import com.skech.moglog.data.local.toDomainModel
import com.skech.moglog.data.local.toEntity
import com.skech.moglog.data.remote.TmdbApi
import com.skech.moglog.data.remote.responses.DramaDto
import com.skech.moglog.domain.model.CastMember
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.domain.model.DramaExtraDetails
import com.skech.moglog.domain.model.SeasonInfo
import com.skech.moglog.domain.model.StreamingProvider
import com.skech.moglog.domain.repository.DramaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class DramaRepositoryImpl(
    private val api: TmdbApi,
    private val dao: DramaDao,
    private val detailCacheDao: DramaDetailCacheDao,
    private val apiKeyManager: ApiKeyManager
) : DramaRepository {

    private val gson = Gson()

    override fun getAllDramas(): Flow<List<Drama>> {
        return dao.getAllDramas().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getLocalDramaById(id: Int): Drama? {
        return dao.getDramaById(id)?.toDomainModel()
    }

    override suspend fun searchDramas(query: String): List<Drama> {
        return try {
            api.searchDramas(query = query).results.mapNotNull { it.toDomainSummary() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getTrendingDramas(): List<Drama> {
        return try {
            api.getTrendingDramas().results.mapNotNull { it.toDomainSummary() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getPopularDramas(): List<Drama> {
        return try {
            api.getPopularDramas().results.mapNotNull { it.toDomainSummary() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getAiringTodayDramas(): List<Drama> {
        return try {
            api.getAiringTodayDramas().results.mapNotNull { it.toDomainSummary() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getOnTheAirDramas(): List<Drama> {
        return try {
            api.getOnTheAirDramas().results.mapNotNull { it.toDomainSummary() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getDramaDetails(id: Int): Drama? {
        return try {
            val dto = api.getDramaDetails(id = id)
            dto.toDomainSummary()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getDramaExtraDetails(id: Int): DramaExtraDetails? {
        val now = System.currentTimeMillis()
        // 1. Check Room cache with 7-day TTL
        val cachedEntity = detailCacheDao.getCache(id)
        if (cachedEntity != null && (now - cachedEntity.cachedAt) < CACHE_TTL_MS) {
            try {
                return gson.fromJson(cachedEntity.payloadJson, DramaExtraDetails::class.java)
            } catch (_: Exception) {
            }
        }

        // 2. Fetch fresh from TMDB
        return try {
            val dto = api.getDramaDetails(id = id)
            val trailerKey = dto.videos?.results?.firstOrNull {
                it.site.equals("YouTube", ignoreCase = true) &&
                    (it.type.equals("Trailer", ignoreCase = true) || it.type.equals("Teaser", ignoreCase = true))
            }?.key

            val cast = dto.credits?.cast?.take(12)?.map {
                CastMember(
                    name = it.name ?: "",
                    character = it.character ?: "",
                    profilePath = it.profilePath?.let { path -> "${TmdbApi.IMAGE_BASE_URL}$path" }
                )
            } ?: emptyList()

            val genres = dto.genres?.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } } ?: emptyList()
            val releaseYear = dto.firstAirDate?.take(4)?.toIntOrNull() ?: 0
            val seasons = dto.seasons
                ?.filter { (it.seasonNumber ?: 0) > 0 }
                ?.map {
                    SeasonInfo(
                        seasonNumber = it.seasonNumber ?: 1,
                        name = it.name ?: "Season ${it.seasonNumber ?: 1}",
                        episodeCount = it.episodeCount ?: 0
                    )
                } ?: emptyList()

            val countryCode = Locale.getDefault().country.ifBlank { "US" }
            val resultsMap = dto.watchProviders?.results.orEmpty()
            val countryProviders = resultsMap[countryCode] ?: resultsMap["US"] ?: resultsMap.values.firstOrNull()
            val rawProviders = (
                countryProviders?.flatrate.orEmpty() +
                    countryProviders?.free.orEmpty() +
                    countryProviders?.ads.orEmpty()
                ).distinctBy { it.providerId }

            val watchProviders = rawProviders.take(8).map { p ->
                StreamingProvider(
                    id = p.providerId,
                    name = p.providerName ?: "Stream",
                    logoUrl = p.logoPath?.let { "${TmdbApi.LOGO_BASE_URL}$it" },
                    link = countryProviders?.link
                )
            }

            val recommendations = dto.recommendations?.results
                ?.mapNotNull { it.toDomainSummary() }
                ?.take(12)
                ?: emptyList()

            val details = DramaExtraDetails(
                trailerKey = trailerKey,
                cast = cast,
                genres = genres,
                releaseYear = releaseYear,
                totalSeasons = dto.numberOfSeasons ?: seasons.size,
                totalEpisodes = dto.numberOfEpisodes ?: 0,
                seasons = seasons,
                watchProviders = watchProviders,
                recommendations = recommendations,
                nextEpisodeAirDate = dto.nextEpisodeToAir?.airDate,
                nextEpisodeNumber = dto.nextEpisodeToAir?.episodeNumber ?: 0,
                nextSeasonNumber = dto.nextEpisodeToAir?.seasonNumber ?: 0
            )

            detailCacheDao.putCache(
                DramaDetailCacheEntity(
                    dramaId = id,
                    payloadJson = gson.toJson(details),
                    cachedAt = now
                )
            )
            // Remove any stale legacy SharedPreferences entry
            apiKeyManager.setExtraDetails(id, null)
            details
        } catch (_: Exception) {
            // Return stale cache if offline
            if (cachedEntity != null) {
                try {
                    return gson.fromJson(cachedEntity.payloadJson, DramaExtraDetails::class.java)
                } catch (_: Exception) {
                }
            }
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

    override suspend fun clearMetadataCache() {
        detailCacheDao.clearAll()
    }

    private fun DramaDto.toDomainSummary(): Drama? {
        if (name.isNullOrBlank()) return null
        val parsedYear = firstAirDate?.take(4)?.toIntOrNull() ?: 0
        val genreNames = genres?.mapNotNull { it.name }
            ?.takeIf { it.isNotEmpty() }
            ?: genreIds?.mapNotNull { TMDB_GENRE_MAP[it] }.orEmpty()

        return Drama(
            id = id,
            title = name,
            overview = overview ?: "",
            rating = voteAverage ?: 0.0,
            posterPath = posterPath?.let { "${TmdbApi.IMAGE_BASE_URL}$it" } ?: "",
            totalEpisodes = numberOfEpisodes ?: 0,
            totalSeasons = numberOfSeasons ?: 0,
            releaseYear = parsedYear,
            genres = genreNames.joinToString(", "),
            nextEpisodeAirDate = nextEpisodeToAir?.airDate,
            nextEpisodeNumber = nextEpisodeToAir?.episodeNumber ?: 0,
            nextSeasonNumber = nextEpisodeToAir?.seasonNumber ?: 0
        )
    }

    companion object {
        private const val CACHE_TTL_MS = 7L * 24L * 60L * 60L * 1000L // 7 days

        private val TMDB_GENRE_MAP = mapOf(
            10759 to "Action & Adventure",
            16 to "Animation",
            35 to "Comedy",
            80 to "Crime",
            99 to "Documentary",
            18 to "Drama",
            10751 to "Family",
            10762 to "Kids",
            9648 to "Mystery",
            10763 to "News",
            10764 to "Reality",
            10765 to "Sci-Fi & Fantasy",
            10766 to "Soap",
            10767 to "Talk",
            10768 to "War & Politics",
            37 to "Western"
        )
    }
}
