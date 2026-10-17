package com.skech.moglog.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skech.moglog.R
import com.skech.moglog.ui.designsystem.IosBottomSheet
import com.skech.moglog.ui.designsystem.IosGroupedRow
import com.skech.moglog.ui.designsystem.IosGroupedSection
import com.skech.moglog.ui.designsystem.IosPrimaryButton
import com.skech.moglog.ui.designsystem.IosSwitchRow
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.designsystem.rememberMoglogHaptics

enum class LibrarySortOption(val key: String, val label: String) {
    RECENTLY_ADDED("RECENTLY_ADDED", "Recently Added"),
    RECENTLY_UPDATED("RECENTLY_UPDATED", "Recently Updated"),
    RECENTLY_COMPLETED("RECENTLY_COMPLETED", "Recently Completed"),
    TMDB_RATING("TMDB_RATING", "TMDB Rating"),
    MY_RATING("MY_RATING", "My Rating"),
    TITLE_AZ("TITLE_AZ", "Title (A–Z)"),
    YEAR("YEAR", "Release Year");

    companion object {
        fun fromKey(key: String): LibrarySortOption =
            entries.find { it.key == key || it.label == key } ?: RECENTLY_ADDED
    }
}

data class LibraryFilterState(
    val sortOption: LibrarySortOption = LibrarySortOption.RECENTLY_ADDED,
    val selectedGenres: Set<String> = emptySet(),
    val minRating: Double = 0.0,
    val onlyWithNotes: Boolean = false
) {
    val isDefault: Boolean
        get() = sortOption == LibrarySortOption.RECENTLY_ADDED &&
            selectedGenres.isEmpty() &&
            minRating <= 0.0 &&
            !onlyWithNotes
}

/**
 * iOS Bottom Sheet with sort options, genre multi-select chips, minimum rating picker, and "Has Notes" filter.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SortFilterSheet(
    currentState: LibraryFilterState,
    availableGenres: List<String>,
    onApply: (LibraryFilterState) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()

    var sortOption by remember(currentState) { mutableStateOf(currentState.sortOption) }
    var selectedGenres by remember(currentState) { mutableStateOf(currentState.selectedGenres) }
    var minRating by remember(currentState) { mutableDoubleStateOf(currentState.minRating) }
    var onlyWithNotes by remember(currentState) { mutableStateOf(currentState.onlyWithNotes) }

    IosBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sort_and_filter),
                    style = typo.title2,
                    color = colors.label
                )
                Text(
                    text = stringResource(R.string.common_reset),
                    style = typo.callout.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.destructive,
                    modifier = Modifier
                        .iosClickable(
                            hapticOnClick = true,
                            onClick = {
                                onReset()
                                onDismiss()
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            // Sort By Section
            IosGroupedSection(header = "Sort By") {
                LibrarySortOption.entries.forEachIndexed { index, option ->
                    val isSelected = sortOption == option
                    IosGroupedRow(
                        title = option.label,
                        titleColor = if (isSelected) colors.accent else colors.label,
                        showDivider = index < LibrarySortOption.entries.lastIndex,
                        onClick = {
                            haptics.selectionChanged()
                            sortOption = option
                        },
                        trailing = {
                            if (isSelected) {
                                Icon(
                                    imageVector = MoglogIcons.Check,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Minimum Rating Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "MINIMUM RATING",
                    style = typo.footnote.copy(fontWeight = FontWeight.Medium),
                    color = colors.secondaryLabel,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                val ratingChoices = listOf(0.0 to "Any", 7.0 to "7.0+", 8.0 to "8.0+", 9.0 to "9.0+")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ratingChoices.forEach { (value, label) ->
                        val selected = minRating == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(SmoothRoundedCornerShape(10.dp))
                                .background(if (selected) colors.accent else colors.tertiaryBackground)
                                .iosClickable(
                                    onClick = {
                                        haptics.selectionChanged()
                                        minRating = value
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = typo.subhead.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (selected) colors.onAccent else colors.label
                            )
                        }
                    }
                }
            }

            // Genre Multi-Select
            if (availableGenres.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "GENRES",
                        style = typo.footnote.copy(fontWeight = FontWeight.Medium),
                        color = colors.secondaryLabel,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableGenres.forEach { genre ->
                            val selected = genre in selectedGenres
                            Box(
                                modifier = Modifier
                                    .clip(SmoothRoundedCornerShape(10.dp))
                                    .background(if (selected) colors.accent else colors.tertiaryBackground)
                                    .iosClickable(
                                        onClick = {
                                            haptics.selectionChanged()
                                            selectedGenres = if (selected) {
                                                selectedGenres - genre
                                            } else {
                                                selectedGenres + genre
                                            }
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = genre,
                                    style = typo.footnote.copy(
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = if (selected) colors.onAccent else colors.label
                                )
                            }
                        }
                    }
                }
            }

            // Has Notes Toggle
            IosGroupedSection {
                IosSwitchRow(
                    title = "Only Shows with Personal Notes",
                    checked = onlyWithNotes,
                    onCheckedChange = { onlyWithNotes = it },
                    icon = MoglogIcons.Note,
                    showDivider = false
                )
            }

            IosPrimaryButton(
                text = stringResource(R.string.common_apply),
                onClick = {
                    onApply(
                        LibraryFilterState(
                            sortOption = sortOption,
                            selectedGenres = selectedGenres,
                            minRating = minRating,
                            onlyWithNotes = onlyWithNotes
                        )
                    )
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
