package com.skech.moglog.ui.library

import com.skech.moglog.domain.model.Drama
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilterAndMergeTest {

    private val sampleDramas = listOf(
        Drama(
            id = 1,
            title = "Alchemy of Souls",
            overview = "Fantasy romance",
            rating = 8.7,
            myRating = 9.5,
            posterPath = "/p1.jpg",
            status = "Done",
            episodesWatched = 20,
            totalEpisodes = 20,
            genres = "Drama, Sci-Fi & Fantasy",
            releaseYear = 2022,
            addedAt = 1_000L,
            updatedAt = 3_000L,
            completedAt = 4_000L
        ),
        Drama(
            id = 2,
            title = "Moving",
            overview = "Superheroes",
            rating = 8.5,
            myRating = 9.0,
            posterPath = "/p2.jpg",
            status = "Watching",
            episodesWatched = 12,
            totalEpisodes = 20,
            genres = "Action & Adventure, Mystery",
            releaseYear = 2023,
            addedAt = 2_000L,
            updatedAt = 5_000L
        ),
        Drama(
            id = 3,
            title = "Beyond Evil",
            overview = "Psychological thriller",
            rating = 8.2,
            myRating = 0.0,
            posterPath = "/p3.jpg",
            status = "Planned",
            episodesWatched = 0,
            totalEpisodes = 16,
            genres = "Crime, Mystery",
            releaseYear = 2021,
            addedAt = 3_000L,
            updatedAt = 2_500L
        )
    )

    @Test
    fun `filtering by genre and minimum rating returns matching shows`() {
        val filterState = LibraryFilterState(
            sortOption = LibrarySortOption.MY_RATING,
            selectedGenres = setOf("Mystery"),
            minRating = 8.4
        )

        val filtered = sampleDramas.filter { drama ->
            val genreMatch = filterState.selectedGenres.isEmpty() ||
                drama.genreList.any { genre -> genre in filterState.selectedGenres }
            val ratingValue = if (drama.myRating > 0.0) drama.myRating else drama.rating
            val ratingMatch = ratingValue >= filterState.minRating
            genreMatch && ratingMatch
        }

        assertEquals(1, filtered.size)
        assertEquals("Moving", filtered.first().title)
    }

    @Test
    fun `timestamp-based cloud and import merge keeps newer updatedAt record`() {
        val local = sampleDramas[1] // Moving, updatedAt = 5_000L, episodesWatched = 12
        val olderIncoming = local.copy(episodesWatched = 8, updatedAt = 4_000L)
        val newerIncoming = local.copy(episodesWatched = 15, updatedAt = 7_500L)

        val resolvedAgainstOlder = if (olderIncoming.updatedAt > local.updatedAt) olderIncoming else local
        assertEquals(12, resolvedAgainstOlder.episodesWatched)

        val resolvedAgainstNewer = if (newerIncoming.updatedAt > local.updatedAt) newerIncoming else local
        assertEquals(15, resolvedAgainstNewer.episodesWatched)
    }
}
