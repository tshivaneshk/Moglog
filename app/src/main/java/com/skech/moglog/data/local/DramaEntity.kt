package com.skech.moglog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skech.moglog.domain.model.Drama

@Entity(tableName = "dramas")
data class DramaEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val overview: String,
    val rating: Double,
    val posterPath: String,
    val status: String,
    val episodesWatched: Int = 0,
    val totalEpisodes: Int = 0,
    val isDeleted: Boolean = false,
    val addedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val completedAt: Long = 0L,
    val myRating: Double = 0.0,
    val notes: String = "",
    val tags: String = "",
    val genres: String = "",
    val releaseYear: Int = 0,
    val totalSeasons: Int = 0,
    val currentSeason: Int = 1,
    val currentEpisode: Int = 0,
    val nextEpisodeAirDate: String? = null,
    val nextEpisodeNumber: Int = 0,
    val nextSeasonNumber: Int = 0,
    val deletedAt: Long = 0L
)

fun DramaEntity.toDomainModel(): Drama {
    return Drama(
        id = id,
        title = title,
        overview = overview,
        rating = rating,
        posterPath = posterPath,
        status = status,
        episodesWatched = episodesWatched,
        totalEpisodes = totalEpisodes,
        isDeleted = isDeleted,
        addedAt = addedAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        myRating = myRating,
        notes = notes,
        tags = tags,
        genres = genres,
        releaseYear = releaseYear,
        totalSeasons = totalSeasons,
        currentSeason = currentSeason,
        currentEpisode = currentEpisode,
        nextEpisodeAirDate = nextEpisodeAirDate,
        nextEpisodeNumber = nextEpisodeNumber,
        nextSeasonNumber = nextSeasonNumber,
        deletedAt = deletedAt
    )
}

fun Drama.toEntity(): DramaEntity {
    return DramaEntity(
        id = id,
        title = title,
        overview = overview,
        rating = rating,
        posterPath = posterPath,
        status = status,
        episodesWatched = episodesWatched,
        totalEpisodes = totalEpisodes,
        isDeleted = isDeleted,
        addedAt = addedAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        myRating = myRating,
        notes = notes,
        tags = tags,
        genres = genres,
        releaseYear = releaseYear,
        totalSeasons = totalSeasons,
        currentSeason = currentSeason,
        currentEpisode = currentEpisode,
        nextEpisodeAirDate = nextEpisodeAirDate,
        nextEpisodeNumber = nextEpisodeNumber,
        nextSeasonNumber = nextSeasonNumber,
        deletedAt = deletedAt
    )
}
