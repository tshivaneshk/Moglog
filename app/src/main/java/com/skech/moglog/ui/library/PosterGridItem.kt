package com.skech.moglog.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable
import java.util.Locale

/**
 * Clean iOS-style poster card:
 * - 14dp smooth corners, no heavy border or shadow
 * - Small status capsule in top-right (or selection checkmark in multi-select mode)
 * - Thin progress bar at bottom edge of poster for Watching items
 * - Title & rating below the poster (not over a heavy dark gradient)
 */
@Composable
fun PosterGridItem(
    drama: Drama,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val context = LocalContext.current
    val statusColor = colors.statusColor(drama.status)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .iosClickable(
                pressedScale = 0.96f,
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(MoglogShapes.Poster)
                .background(colors.secondaryBackground)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, colors.accent, MoglogShapes.Poster)
                    } else {
                        Modifier
                    }
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(drama.posterPath)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = drama.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Status Dot / Capsule or Multi-select Checkmark
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.accent else Color.Black.copy(alpha = 0.55f))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = MoglogIcons.Check,
                            contentDescription = "Selected",
                            tint = colors.onAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .clip(SmoothRoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.68f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = if (drama.status == "Watching" && drama.episodesWatched > 0) {
                                if (drama.totalEpisodes > 0) "${drama.episodesWatched}/${drama.totalEpisodes}"
                                else "Ep ${drama.episodesWatched}"
                            } else {
                                drama.status
                            },
                            style = typo.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }

            // Progress bar along bottom of poster for Watching items
            if (drama.status == "Watching" && drama.totalEpisodes > 0 && drama.episodesWatched > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(drama.progressFraction)
                            .height(4.dp)
                            .background(colors.statusWatching)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = drama.title,
            style = typo.footnote.copy(fontWeight = FontWeight.SemiBold),
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val displayRating = if (drama.myRating > 0.0) drama.myRating else drama.rating
            Icon(
                imageVector = MoglogIcons.StarFilled,
                contentDescription = null,
                tint = if (drama.myRating > 0.0) colors.accent else colors.starGold,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = String.format(Locale.US, "%.1f", displayRating),
                style = typo.caption,
                color = colors.secondaryLabel
            )
            if (drama.releaseYear > 0) {
                Text(
                    text = "· ${drama.releaseYear}",
                    style = typo.caption,
                    color = colors.tertiaryLabel
                )
            }
        }
    }
}
