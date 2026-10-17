package com.skech.moglog.data.remote

import com.skech.moglog.data.remote.responses.DramaDto
import com.skech.moglog.data.remote.responses.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("search/tv")
    suspend fun searchDramas(
        @Query("query") query: String,
        @Query("language") language: String = "en-US"
    ): SearchResponse

    @GET("tv/{tv_id}")
    suspend fun getDramaDetails(
        @Path("tv_id") id: Int,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "credits,videos,watch/providers,recommendations"
    ): DramaDto

    @GET("trending/tv/week")
    suspend fun getTrendingDramas(
        @Query("language") language: String = "en-US"
    ): SearchResponse

    @GET("tv/popular")
    suspend fun getPopularDramas(
        @Query("language") language: String = "en-US"
    ): SearchResponse

    @GET("tv/airing_today")
    suspend fun getAiringTodayDramas(
        @Query("language") language: String = "en-US"
    ): SearchResponse

    @GET("tv/on_the_air")
    suspend fun getOnTheAirDramas(
        @Query("language") language: String = "en-US"
    ): SearchResponse

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
        const val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w780"
        const val LOGO_BASE_URL = "https://image.tmdb.org/t/p/w154"
    }
}
