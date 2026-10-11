package com.skech.moglog.data.remote.responses

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("results") val results: List<DramaDto>
)

data class DramaDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerializedName("videos") val videos: VideosDto? = null,
    @SerializedName("credits") val credits: CreditsDto? = null
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
