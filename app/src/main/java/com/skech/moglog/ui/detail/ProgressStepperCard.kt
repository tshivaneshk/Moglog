package com.skech.moglog.ui.detail

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skech.moglog.R
import com.skech.moglog.domain.model.SeasonInfo
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.designsystem.rememberMoglogHaptics

/**
 * Interactive iOS Episode & Season Progress Stepper Card:
 * - Season selector chips (when show has multiple seasons)
 * - Episode count display + "−" / "+" stepper buttons
 * - Visual progress bar
 * - Next episode air-date banner if available
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressStepperCard(
    episodesWatched: Int,
    totalEpisodes: Int,
    currentSeason: Int,
    totalSeasons: Int,
    seasons: List<SeasonInfo>,
    nextEpisodeAirDate: String?,
    nextSeasonNumber: Int,
    nextEpisodeNumber: Int,
    accentColor: androidx.compose.ui.graphics.Color,
    onSeasonChange: (Int) -> Unit,
    onEpisodesWatchedChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MoglogShapes.Card)
            .background(colors.secondaryBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.detail_progress_title),
                    style = typo.headline,
                    color = colors.label
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (totalEpisodes > 0) {
                        stringResource(R.string.detail_episodes_watched_format, episodesWatched, totalEpisodes)
                    } else {
                        stringResource(R.string.detail_episodes_watched_unknown_total, episodesWatched)
                    },
                    style = typo.footnote,
                    color = colors.secondaryLabel
                )
            }

            // − / + Stepper Pill
            Row(
                modifier = Modifier
                    .clip(MoglogShapes.Control)
                    .background(colors.tertiaryBackground)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 32.dp)
                        .clip(SmoothRoundedCornerShape(8.dp))
                        .background(colors.secondaryBackground)
                        .iosClickable(
                            enabled = episodesWatched > 0,
                            onClick = {
                                haptics.lightImpact()
                                onEpisodesWatchedChange((episodesWatched - 1).coerceAtLeast(0))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MoglogIcons.Minus,
                        contentDescription = "Previous episode",
                        tint = if (episodesWatched > 0) colors.label else colors.tertiaryLabel,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "$episodesWatched",
                    style = typo.headline,
                    color = colors.label,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                val canIncrement = totalEpisodes <= 0 || episodesWatched < totalEpisodes
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 32.dp)
                        .clip(SmoothRoundedCornerShape(8.dp))
                        .background(if (canIncrement) accentColor else colors.secondaryBackground)
                        .iosClickable(
                            enabled = canIncrement,
                            onClick = {
                                haptics.success()
                                val next = episodesWatched + 1
                                onEpisodesWatchedChange(
                                    if (totalEpisodes > 0) next.coerceAtMost(totalEpisodes) else next
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MoglogIcons.Plus,
                        contentDescription = "Next episode",
                        tint = if (canIncrement) colors.onAccent else colors.tertiaryLabel,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Progress Bar
        val fraction = if (totalEpisodes > 0) {
            (episodesWatched.toFloat() / totalEpisodes.toFloat()).coerceIn(0f, 1f)
        } else if (episodesWatched > 0) {
            ((episodesWatched % 12) / 12f).coerceAtLeast(0.08f)
        } else {
            0f
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(colors.tertiaryBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }

        // Season Picker Row (if multiple seasons)
        val seasonCount = if (seasons.isNotEmpty()) seasons.size else totalSeasons
        if (seasonCount > 1) {
            HorizontalDivider(thickness = colors.hairlineWidth, color = colors.separator)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val seasonNumbers = if (seasons.isNotEmpty()) {
                    seasons.map { it.seasonNumber }
                } else {
                    (1..seasonCount.coerceAtMost(12)).toList()
                }
                seasonNumbers.forEach { sNum ->
                    val isSelected = currentSeason == sNum
                    Box(
                        modifier = Modifier
                            .clip(SmoothRoundedCornerShape(8.dp))
                            .background(if (isSelected) accentColor else colors.tertiaryBackground)
                            .iosClickable(
                                onClick = {
                                    haptics.selectionChanged()
                                    onSeasonChange(sNum)
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "S$sNum",
                            style = typo.caption.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) colors.onAccent else colors.secondaryLabel
                        )
                    }
                }
            }
        }

        // Next Episode Air-Date Banner
        if (!nextEpisodeAirDate.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SmoothRoundedCornerShape(10.dp))
                    .background(colors.tertiaryBackground)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = MoglogIcons.Bell,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (nextSeasonNumber > 0 && nextEpisodeNumber > 0) {
                        stringResource(
                            R.string.detail_next_air_date,
                            nextSeasonNumber,
                            nextEpisodeNumber,
                            nextEpisodeAirDate
                        )
                    } else {
                        "Next episode airs $nextEpisodeAirDate"
                    },
                    style = typo.footnote.copy(fontWeight = FontWeight.Medium),
                    color = colors.label
                )
            }
        }
    }
}
