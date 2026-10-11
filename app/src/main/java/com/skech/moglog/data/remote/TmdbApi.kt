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
        @Query("api_key") apiKey: String = "b30d871ab7f51c18054f0e6d0921da16",
        @Query("language") language: String = "en-US"
    ): SearchResponse

    @GET("tv/{tv_id}")
    suspend fun getDramaDetails(
        @Path("tv_id") id: Int,
        @Query("api_key") apiKey: String = "b30d871ab7f51c18054f0e6d0921da16",
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String = "credits,videos"
    ): DramaDto


    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
    }
}
