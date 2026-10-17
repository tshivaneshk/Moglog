package com.skech.moglog.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.designsystem.rememberMoglogHaptics

/**
 * Persistent mini "Continue Watching" rail at the top of the Library screen.
 * Shows Watching items sorted by most recently updated, with a one-tap "+1 Ep" button.
 */
@Composable
fun ContinueWatchingSection(
    watchingDramas: List<Drama>,
    onDramaClick: (Drama) -> Unit,
    onIncrementEpisode: (Drama) -> Unit,
    modifier: Modifier = Modifier
) {
    if (watchingDramas.isEmpty()) return

    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.continue_watching_title),
            style = typo.headline,
            color = colors.label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(watchingDramas.take(10), key = { "cw_${it.id}" }) { drama ->
                Row(
                    modifier = Modifier
                        .width(245.dp)
                        .clip(MoglogShapes.Card)
                        .background(colors.secondaryBackground)
                        .iosClickable(
                            pressedScale = 0.97f,
                            onClick = { onDramaClick(drama) }
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(drama.posterPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = drama.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 44.dp, height = 62.dp)
                            .clip(SmoothRoundedCornerShape(8.dp))
                            .background(colors.tertiaryBackground)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = drama.title,
                            style = typo.subhead.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val nextEp = drama.episodesWatched + 1
                        val progressText = if (drama.totalEpisodes > 0) {
                            "Ep ${drama.episodesWatched}/${drama.totalEpisodes}"
                        } else {
                            "Next: Ep $nextEp"
                        }
                        Text(
                            text = progressText,
                            style = typo.caption,
                            color = colors.secondaryLabel
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(colors.tertiaryBackground)
                        ) {
                            val fraction = if (drama.totalEpisodes > 0) {
                                drama.progressFraction
                            } else {
                                ((drama.episodesWatched % 12) / 12f).coerceAtLeast(0.1f)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(colors.statusWatching)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(SmoothRoundedCornerShape(10.dp))
                            .background(colors.accent)
                            .iosClickable(
                                onClick = {
                                    haptics.success()
                                    onIncrementEpisode(drama)
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+1",
                            style = typo.footnote.copy(fontWeight = FontWeight.Bold),
                            color = colors.onAccent
                        )
                    }
                }
            }
        }
    }
}
