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
    val completedAt: Long = 0L,
    val myRating: Double = 0.0,
    val notes: String = "",
    val tags: String = "",
    val genres: String = "", // Comma-separated genre names, e.g. "Drama, Sci-Fi & Fantasy"
    val releaseYear: Int = 0,
    val totalSeasons: Int = 0,
    val currentSeason: Int = 1,
    val currentEpisode: Int = 0,
    val nextEpisodeAirDate: String? = null,
    val nextEpisodeNumber: Int = 0,
    val nextSeasonNumber: Int = 0,
    val deletedAt: Long = 0L
) {
    val genreList: List<String>
        get() = if (genres.isBlank()) emptyList() else genres.split(",").map { it.trim() }.filter { it.isNotBlank() }

    val tagList: List<String>
        get() = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotBlank() }

    val progressFraction: Float
        get() = if (totalEpisodes > 0) (episodesWatched.toFloat() / totalEpisodes.toFloat()).coerceIn(0f, 1f) else 0f
}
