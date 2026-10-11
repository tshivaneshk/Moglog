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
    val completedAt: Long = 0L
)

fun DramaEntity.toDomainModel(): Drama {
    return Drama(id, title, overview, rating, posterPath, status, episodesWatched, totalEpisodes, isDeleted, addedAt, updatedAt, completedAt)
}

fun Drama.toEntity(): DramaEntity {
    return DramaEntity(id, title, overview, rating, posterPath, status, episodesWatched, totalEpisodes, isDeleted, addedAt, updatedAt, completedAt)
}
