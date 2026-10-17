package com.skech.moglog.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.skech.moglog.ui.designsystem.ActionSheetItem
import com.skech.moglog.ui.designsystem.IosActionSheet
import com.skech.moglog.ui.designsystem.IosCircleIconButton
import com.skech.moglog.ui.designsystem.IosEmptyState
import com.skech.moglog.ui.designsystem.IosLargeTitleHeader
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.designsystem.rememberMoglogHaptics

/**
 * iOS grouped list Recycle Bin screen with:
 * - Swipe right to Restore, swipe left to Delete Forever
 * - Days remaining before 30-day auto-purge
 * - "Empty" action sheet confirmation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    deletedDramas: List<Drama>,
    onBack: () -> Unit,
    onRestore: (Drama) -> Unit,
    onDeletePermanently: (Drama) -> Unit,
    onEmptyBin: () -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()
    val context = LocalContext.current

    var showEmptyConfirmSheet by remember { mutableStateOf(false) }

    if (showEmptyConfirmSheet) {
        IosActionSheet(
            title = "Empty Recycle Bin?",
            message = "This will permanently delete ${deletedDramas.size} shows. This action cannot be undone.",
            actions = listOf(
                ActionSheetItem(
                    label = "Delete All Permanently",
                    isDestructive = true,
                    icon = MoglogIcons.Trash,
                    onClick = onEmptyBin
                )
            ),
            onDismissRequest = { showEmptyConfirmSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        IosLargeTitleHeader(
            title = stringResource(R.string.recycle_bin_title),
            collapsed = false,
            leadingActions = {
                IosCircleIconButton(
                    icon = MoglogIcons.ChevronLeft,
                    contentDescription = stringResource(R.string.detail_back_cd),
                    onClick = onBack
                )
            },
            trailingActions = {
                if (deletedDramas.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.recycle_bin_empty_action),
                        style = typo.callout.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.destructive,
                        modifier = Modifier
                            .iosClickable(
                                hapticOnClick = true,
                                onClick = { showEmptyConfirmSheet = true }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        )

        if (deletedDramas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                IosEmptyState(
                    icon = MoglogIcons.Trash,
                    title = stringResource(R.string.recycle_bin_empty_title),
                    subtitle = stringResource(R.string.recycle_bin_empty_subtitle)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = "SWIPE RIGHT TO RESTORE · SWIPE LEFT TO DELETE FOREVER",
                        style = typo.caption,
                        color = colors.secondaryLabel,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MoglogShapes.Card)
                            .background(colors.secondaryBackground)
                    ) {
                        deletedDramas.forEachIndexed { index, drama ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    when (value) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            haptics.success()
                                            onRestore(drama)
                                            false
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            haptics.warning()
                                            onDeletePermanently(drama)
                                            false
                                        }
                                        SwipeToDismissBoxValue.Settled -> false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val dir = dismissState.dismissDirection
                                    val bg = when (dir) {
                                        SwipeToDismissBoxValue.StartToEnd -> colors.accent
                                        SwipeToDismissBoxValue.EndToStart -> colors.destructive
                                        else -> Color.Transparent
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(bg)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = if (dir == SwipeToDismissBoxValue.StartToEnd) {
                                            Alignment.CenterStart
                                        } else {
                                            Alignment.CenterEnd
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (dir == SwipeToDismissBoxValue.StartToEnd) {
                                                MoglogIcons.Refresh
                                            } else {
                                                MoglogIcons.Trash
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.secondaryBackground)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = drama.title,
                                                style = typo.headline,
                                                color = colors.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val daysRemaining = if (drama.deletedAt > 0L) {
                                                val elapsedDays = ((System.currentTimeMillis() - drama.deletedAt) / (1000L * 60 * 60 * 24)).toInt()
                                                (30 - elapsedDays).coerceIn(1, 30)
                                            } else 30
                                            Text(
                                                text = "$daysRemaining days left before auto-purge",
                                                style = typo.caption,
                                                color = colors.secondaryLabel
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            IosCircleIconButton(
                                                icon = MoglogIcons.Refresh,
                                                contentDescription = stringResource(R.string.recycle_bin_restore),
                                                tint = colors.accent,
                                                onClick = { onRestore(drama) }
                                            )
                                            IosCircleIconButton(
                                                icon = MoglogIcons.Trash,
                                                contentDescription = stringResource(R.string.recycle_bin_delete_forever),
                                                tint = colors.destructive,
                                                onClick = { onDeletePermanently(drama) }
                                            )
                                        }
                                    }
                                    if (index < deletedDramas.lastIndex) {
                                        HorizontalDivider(
                                            thickness = colors.hairlineWidth,
                                            color = colors.separator,
                                            modifier = Modifier.padding(start = 70.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
