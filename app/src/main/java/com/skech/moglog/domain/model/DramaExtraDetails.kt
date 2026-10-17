package com.skech.moglog.domain.model

data class DramaExtraDetails(
    val trailerKey: String? = null,
    val cast: List<CastMember> = emptyList(),
    val genres: List<String> = emptyList(),
    val releaseYear: Int = 0,
    val totalSeasons: Int = 0,
    val totalEpisodes: Int = 0,
    val seasons: List<SeasonInfo> = emptyList(),
    val watchProviders: List<StreamingProvider> = emptyList(),
    val recommendations: List<Drama> = emptyList(),
    val nextEpisodeAirDate: String? = null,
    val nextEpisodeNumber: Int = 0,
    val nextSeasonNumber: Int = 0
)

data class CastMember(
    val name: String,
    val character: String,
    val profilePath: String?
)

data class SeasonInfo(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int
)

data class StreamingProvider(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val link: String?
)
