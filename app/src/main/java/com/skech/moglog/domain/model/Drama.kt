package com.skech.moglog.domain.model

data class Drama(
    val id: Int,
    val title: String,
    val overview: String,
    val rating: Double,
    val posterPath: String,
    val status: String = "Planned", // "Planned", "Watching", "Done"
    val episodesWatched: Int = 0,
    val totalEpisodes: Int = 0,
    val isDeleted: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = 0L
)
