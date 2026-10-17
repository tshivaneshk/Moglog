package com.skech.moglog.data.remote.responses

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("results") val results: List<DramaDto> = emptyList()
)

data class DramaDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerializedName("genres") val genres: List<GenreDto>? = null,
    @SerializedName("genre_ids") val genreIds: List<Int>? = null,
    @SerializedName("next_episode_to_air") val nextEpisodeToAir: NextEpisodeDto? = null,
    @SerializedName("seasons") val seasons: List<SeasonDto>? = null,
    @SerializedName("videos") val videos: VideosDto? = null,
    @SerializedName("credits") val credits: CreditsDto? = null,
    @SerializedName("watch/providers") val watchProviders: WatchProvidersResponseDto? = null,
    @SerializedName("recommendations") val recommendations: SearchResponse? = null
)

data class GenreDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?
)

data class NextEpisodeDto(
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("episode_number") val episodeNumber: Int?,
    @SerializedName("season_number") val seasonNumber: Int?,
    @SerializedName("name") val name: String?
)

data class SeasonDto(
    @SerializedName("id") val id: Int,
    @SerializedName("season_number") val seasonNumber: Int?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("name") val name: String?
)

data class VideosDto(
    @SerializedName("results") val results: List<VideoDto>?
)

data class VideoDto(
    @SerializedName("key") val key: String?,
    @SerializedName("site") val site: String?,
    @SerializedName("type") val type: String?
)

data class CreditsDto(
    @SerializedName("cast") val cast: List<CastDto>?
)

data class CastDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("character") val character: String?,
    @SerializedName("profile_path") val profilePath: String?
)

data class WatchProvidersResponseDto(
    @SerializedName("results") val results: Map<String, CountryWatchProvidersDto>?
)

data class CountryWatchProvidersDto(
    @SerializedName("link") val link: String?,
    @SerializedName("flatrate") val flatrate: List<WatchProviderDto>?,
    @SerializedName("free") val free: List<WatchProviderDto>?,
    @SerializedName("ads") val ads: List<WatchProviderDto>?,
    @SerializedName("buy") val buy: List<WatchProviderDto>?
)

data class WatchProviderDto(
    @SerializedName("provider_id") val providerId: Int,
    @SerializedName("provider_name") val providerName: String?,
    @SerializedName("logo_path") val logoPath: String?
)
