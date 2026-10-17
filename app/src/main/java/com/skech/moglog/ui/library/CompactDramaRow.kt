package com.skech.moglog.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.skech.moglog.R
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.designsystem.rememberMoglogHaptics
import java.util.Locale

/**
 * Compact iOS list row with poster thumbnail, title, status pill, episode progress bar, rating,
 * and bidirectional swipe actions:
 * - Swipe Right (StartToEnd): +1 Episode (or Mark Done)
 * - Swipe Left (EndToStart): Move to Recycle Bin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactDramaRow(
    drama: Drama,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeIncrementEpisode: () -> Unit,
    onSwipeDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    showDivider: Boolean = true
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()
    val context = LocalContext.current
    val statusColor = colors.statusColor(drama.status)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptics.success()
                    onSwipeIncrementEpisode()
                    false // Snap back after triggering action
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptics.warning()
                    onSwipeDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isSelectionMode,
        enableDismissFromEndToStart = !isSelectionMode,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> colors.accent
                SwipeToDismissBoxValue.EndToStart -> colors.destructive
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                }
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = MoglogIcons.Plus,
                            contentDescription = null,
                            tint = colors.onAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.plus_one_ep),
                            style = typo.subhead.copy(fontWeight = FontWeight.Bold),
                            color = colors.onAccent
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = MoglogIcons.Trash,
                        contentDescription = stringResource(R.string.action_move_to_bin),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.secondaryBackground)
                .iosClickable(
                    pressedScale = 0.99f,
                    onLongClick = onLongClick,
                    onClick = onClick
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.accent else Color.Transparent)
                            .border(1.5.dp, if (isSelected) colors.accent else colors.secondaryLabel, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = MoglogIcons.Check,
                                contentDescription = null,
                                tint = colors.onAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Poster thumb
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(drama.posterPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = drama.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(48.dp)
                        .height(68.dp)
                        .clip(SmoothRoundedCornerShape(8.dp))
                        .background(colors.tertiaryBackground)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = drama.title,
                            style = typo.headline,
                            color = colors.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Status chip
                        Box(
                            modifier = Modifier
                                .clip(SmoothRoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.16f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = drama.status,
                                style = typo.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress or subtitle info
                    val epLabel = if (drama.totalEpisodes > 0) {
                        "S${drama.currentSeason} · ${drama.episodesWatched}/${drama.totalEpisodes} eps"
                    } else if (drama.episodesWatched > 0) {
                        "S${drama.currentSeason} · ${drama.episodesWatched} eps watched"
                    } else {
                        drama.genreList.take(2).joinToString(" · ").ifBlank { "No episodes logged" }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = epLabel,
                            style = typo.footnote,
                            color = colors.secondaryLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        val ratingVal = if (drama.myRating > 0.0) drama.myRating else drama.rating
                        if (ratingVal > 0.0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = MoglogIcons.StarFilled,
                                    contentDescription = null,
                                    tint = if (drama.myRating > 0.0) colors.accent else colors.starGold,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f", ratingVal),
                                    style = typo.caption,
                                    color = colors.secondaryLabel
                                )
                            }
                        }
                    }

                    if (drama.totalEpisodes > 0 && drama.episodesWatched > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(colors.tertiaryBackground)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(drama.progressFraction)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                        }
                    }
                }

                if (!isSelectionMode && drama.status == "Watching") {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(SmoothRoundedCornerShape(8.dp))
                            .background(colors.accentSubtle)
                            .iosClickable(
                                hapticOnClick = true,
                                onClick = onSwipeIncrementEpisode
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.plus_one_ep),
                            style = typo.caption.copy(fontWeight = FontWeight.Bold),
                            color = colors.accent
                        )
                    }
                }
            }

            if (showDivider) {
                HorizontalDivider(
                    thickness = colors.hairlineWidth,
                    color = colors.separator,
                    modifier = Modifier.padding(start = 74.dp)
                )
            }
        }
    }
}
