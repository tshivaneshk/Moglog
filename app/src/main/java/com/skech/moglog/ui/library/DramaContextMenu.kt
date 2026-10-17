package com.skech.moglog.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.skech.moglog.ui.designsystem.IosBottomSheet
import com.skech.moglog.ui.designsystem.IosGroupedRow
import com.skech.moglog.ui.designsystem.IosGroupedSection
import com.skech.moglog.ui.designsystem.IosSegmentedControl
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.designsystem.rememberMoglogHaptics

/**
 * Long-press iOS context menu sheet for a drama card/row:
 * - Poster + title preview header
 * - Inline status switcher (Watching / Planned / Done)
 * - Watch Next Episode (+1)
 * - Quick Rate (1–10 stars)
 * - Open Details / Edit Note
 * - Share Show
 * - Enter Multi-Select Mode
 * - Move to Recycle Bin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DramaContextMenuSheet(
    drama: Drama,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onIncrementEpisode: () -> Unit,
    onQuickRate: (Double) -> Unit,
    onOpenDetails: () -> Unit,
    onShare: () -> Unit,
    onStartMultiSelect: () -> Unit,
    onMoveToRecycleBin: () -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()
    val context = LocalContext.current

    IosBottomSheet(onDismissRequest = onDismiss, skipPartiallyExpanded = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        .size(width = 52.dp, height = 74.dp)
                        .clip(SmoothRoundedCornerShape(10.dp))
                        .background(colors.tertiaryBackground)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = drama.title,
                        style = typo.title3,
                        color = colors.label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val sub = buildString {
                        append(drama.status)
                        if (drama.episodesWatched > 0) {
                            append(" · Ep ${drama.episodesWatched}")
                            if (drama.totalEpisodes > 0) append("/${drama.totalEpisodes}")
                        }
                    }
                    Text(
                        text = sub,
                        style = typo.footnote,
                        color = colors.secondaryLabel
                    )
                }
            }

            // Inline Status Switcher
            val statuses = listOf("Watching", "Planned", "Done")
            IosSegmentedControl(
                items = statuses,
                selectedItem = drama.status,
                onItemSelected = { newStatus ->
                    onStatusChange(newStatus)
                    onDismiss()
                },
                labelProvider = { it }
            )

            // Quick Personal Rating Row (5 stars = 2..10 points)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SmoothRoundedCornerShape(14.dp))
                    .background(colors.tertiaryBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.action_rate_show),
                        style = typo.subhead.copy(fontWeight = FontWeight.Medium),
                        color = colors.label
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (star in 1..5) {
                            val score = star * 2.0
                            val filled = drama.myRating >= score - 1.0
                            Icon(
                                imageVector = if (filled) MoglogIcons.StarFilled else MoglogIcons.StarOutline,
                                contentDescription = "Rate ${score.toInt()}",
                                tint = if (filled) colors.starGold else colors.secondaryLabel,
                                modifier = Modifier
                                    .size(24.dp)
                                    .iosClickable(
                                        onClick = {
                                            haptics.selectionChanged()
                                            onQuickRate(score)
                                            onDismiss()
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            // Context Menu Actions
            IosGroupedSection {
                IosGroupedRow(
                    title = stringResource(R.string.action_mark_next_ep),
                    icon = MoglogIcons.Plus,
                    iconBackground = colors.accent,
                    onClick = {
                        haptics.success()
                        onIncrementEpisode()
                        onDismiss()
                    }
                )
                IosGroupedRow(
                    title = stringResource(R.string.action_edit_note),
                    icon = MoglogIcons.Note,
                    iconBackground = colors.statusPlanned,
                    onClick = {
                        onDismiss()
                        onOpenDetails()
                    }
                )
                IosGroupedRow(
                    title = stringResource(R.string.action_share),
                    icon = MoglogIcons.Share,
                    iconBackground = colors.statusWatching,
                    onClick = {
                        onDismiss()
                        onShare()
                    }
                )
                IosGroupedRow(
                    title = stringResource(R.string.action_select_multiple),
                    icon = MoglogIcons.Check,
                    iconBackground = colors.tertiaryLabel,
                    onClick = {
                        onDismiss()
                        onStartMultiSelect()
                    }
                )
                IosGroupedRow(
                    title = stringResource(R.string.action_move_to_bin),
                    titleColor = colors.destructive,
                    icon = MoglogIcons.Trash,
                    iconBackground = colors.destructive,
                    showDivider = false,
                    onClick = {
                        haptics.warning()
                        onDismiss()
                        onMoveToRecycleBin()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
