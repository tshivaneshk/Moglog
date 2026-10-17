package com.skech.moglog.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.skech.moglog.ui.designsystem.IosCircleIconButton
import com.skech.moglog.ui.designsystem.IosEmptyState
import com.skech.moglog.ui.designsystem.IosGroupedRow
import com.skech.moglog.ui.designsystem.IosGroupedSection
import com.skech.moglog.ui.designsystem.IosLargeTitleHeader
import com.skech.moglog.ui.designsystem.IosSearchBar
import com.skech.moglog.ui.designsystem.IosShimmerBox
import com.skech.moglog.ui.designsystem.IosToastBanner
import com.skech.moglog.ui.designsystem.IosToastData
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.designsystem.rememberMoglogHaptics
import com.skech.moglog.ui.viewmodel.DiscoverRailsState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun DiscoverScreen(
    hasApiKey: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<Drama>,
    isSearching: Boolean,
    recentSearches: List<String>,
    onClearRecentSearches: () -> Unit,
    discoverRails: DiscoverRailsState,
    onRefreshRails: () -> Unit,
    savedDramaIds: Set<Int>,
    onDramaClick: (Int) -> Unit,
    onQuickAddToPlanned: (Drama) -> Unit,
    onNavigateToSettings: () -> Unit,
    bottomBarPadding: PaddingValues = PaddingValues(bottom = 84.dp)
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()

    val listState = rememberLazyListState()
    val isHeaderCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 90
        }
    }

    var toast by remember { mutableStateOf<IosToastData?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2600)
            toast = null
        }
    }

    LaunchedEffect(hasApiKey) {
        if (hasApiKey && discoverRails.trending.isEmpty() && !discoverRails.isLoading) {
            onRefreshRails()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            IosLargeTitleHeader(
                title = stringResource(R.string.discover_title),
                collapsed = isHeaderCollapsed,
                trailingActions = {
                    if (hasApiKey && query.isBlank()) {
                        IosCircleIconButton(
                            icon = MoglogIcons.Refresh,
                            contentDescription = "Refresh Discover",
                            onClick = onRefreshRails
                        )
                    }
                },
                bottomContent = {
                    IosSearchBar(
                        query = query,
                        onQueryChange = onQueryChange,
                        placeholder = stringResource(R.string.discover_search_placeholder),
                        showCancelButton = query.isNotEmpty(),
                        onCancel = { onQueryChange("") }
                    )
                }
            )

            if (!hasApiKey) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottomBarPadding),
                    contentAlignment = Alignment.Center
                ) {
                    IosEmptyState(
                        icon = MoglogIcons.Key,
                        title = stringResource(R.string.discover_no_key_title),
                        subtitle = stringResource(R.string.discover_no_key_subtitle),
                        actionLabel = stringResource(R.string.discover_configure_key),
                        onAction = onNavigateToSettings
                    )
                }
            } else if (query.isNotBlank()) {
                // Active Search State
                if (isSearching) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = bottomBarPadding.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(6) {
                            IosShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                            )
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottomBarPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        IosEmptyState(
                            icon = MoglogIcons.Search,
                            title = stringResource(R.string.discover_no_results, query),
                            subtitle = "Check the spelling or try searching by original or English title."
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = bottomBarPadding.calculateBottomPadding() + 16.dp
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MoglogShapes.Card)
                                    .background(colors.secondaryBackground)
                            ) {
                                searchResults.forEachIndexed { index, drama ->
                                    val isSaved = drama.id in savedDramaIds
                                    DiscoverSearchResultRow(
                                        drama = drama,
                                        isSaved = isSaved,
                                        showDivider = index < searchResults.lastIndex,
                                        onClick = { onDramaClick(drama.id) },
                                        onQuickAdd = {
                                            if (!isSaved) {
                                                haptics.success()
                                                onQuickAddToPlanned(drama)
                                                toast = IosToastData(
                                                    message = "Added \"${drama.title}\" to Planned"
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Idle State: Recent Searches + TMDB Discovery Rails
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = bottomBarPadding.calculateBottomPadding() + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (recentSearches.isNotEmpty()) {
                        item(key = "recent_searches") {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.discover_recent_searches).uppercase(),
                                        style = typo.footnote.copy(fontWeight = FontWeight.Medium),
                                        color = colors.secondaryLabel
                                    )
                                    Text(
                                        text = stringResource(R.string.discover_clear_recent),
                                        style = typo.footnote.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.accent,
                                        modifier = Modifier.iosClickable(
                                            hapticOnClick = true,
                                            onClick = onClearRecentSearches
                                        )
                                    )
                                }
                                IosGroupedSection {
                                    recentSearches.take(5).forEachIndexed { index, recent ->
                                        IosGroupedRow(
                                            title = recent,
                                            icon = MoglogIcons.Search,
                                            iconBackground = colors.tertiaryBackground,
                                            iconTint = colors.secondaryLabel,
                                            showChevron = true,
                                            showDivider = index < recentSearches.take(5).lastIndex,
                                            onClick = { onQueryChange(recent) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (discoverRails.isLoading && discoverRails.trending.isEmpty()) {
                        items(3) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                IosShimmerBox(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(20.dp),
                                    cornerRadius = 6.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    repeat(3) {
                                        IosShimmerBox(
                                            modifier = Modifier
                                                .width(126.dp)
                                                .height(186.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (discoverRails.trending.isNotEmpty()) {
                            item(key = "rail_trending") {
                                DiscoverPosterRail(
                                    title = stringResource(R.string.rail_trending),
                                    dramas = discoverRails.trending,
                                    savedDramaIds = savedDramaIds,
                                    onDramaClick = onDramaClick,
                                    onQuickAdd = { drama ->
                                        haptics.success()
                                        onQuickAddToPlanned(drama)
                                        toast = IosToastData(message = "Added \"${drama.title}\" to Planned")
                                    }
                                )
                            }
                        }

                        if (discoverRails.popular.isNotEmpty()) {
                            item(key = "rail_popular") {
                                DiscoverPosterRail(
                                    title = stringResource(R.string.rail_popular),
                                    dramas = discoverRails.popular,
                                    savedDramaIds = savedDramaIds,
                                    onDramaClick = onDramaClick,
                                    onQuickAdd = { drama ->
                                        haptics.success()
                                        onQuickAddToPlanned(drama)
                                        toast = IosToastData(message = "Added \"${drama.title}\" to Planned")
                                    }
                                )
                            }
                        }

                        if (discoverRails.airingToday.isNotEmpty()) {
                            item(key = "rail_airing_today") {
                                DiscoverPosterRail(
                                    title = stringResource(R.string.rail_airing_today),
                                    dramas = discoverRails.airingToday,
                                    savedDramaIds = savedDramaIds,
                                    onDramaClick = onDramaClick,
                                    onQuickAdd = { drama ->
                                        haptics.success()
                                        onQuickAddToPlanned(drama)
                                        toast = IosToastData(message = "Added \"${drama.title}\" to Planned")
                                    }
                                )
                            }
                        }

                        if (discoverRails.onTheAir.isNotEmpty()) {
                            item(key = "rail_on_the_air") {
                                DiscoverPosterRail(
                                    title = stringResource(R.string.rail_on_the_air),
                                    dramas = discoverRails.onTheAir,
                                    savedDramaIds = savedDramaIds,
                                    onDramaClick = onDramaClick,
                                    onQuickAdd = { drama ->
                                        haptics.success()
                                        onQuickAddToPlanned(drama)
                                        toast = IosToastData(message = "Added \"${drama.title}\" to Planned")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        IosToastBanner(
            toast = toast,
            onDismiss = { toast = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomBarPadding.calculateBottomPadding() + 8.dp)
        )
    }
}

@Composable
private fun DiscoverPosterRail(
    title: String,
    dramas: List<Drama>,
    savedDramaIds: Set<Int>,
    onDramaClick: (Int) -> Unit,
    onQuickAdd: (Drama) -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = typo.title3,
            color = colors.label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dramas, key = { "${title}_${it.id}" }) { drama ->
                val isSaved = drama.id in savedDramaIds
                Column(
                    modifier = Modifier
                        .width(128.dp)
                        .iosClickable(
                            pressedScale = 0.96f,
                            onClick = { onDramaClick(drama.id) }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.68f)
                            .clip(MoglogShapes.Poster)
                            .background(colors.secondaryBackground)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(drama.posterPath)
                                .crossfade(true)
                                .build(),
                            contentDescription = drama.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Quick Add (+) or In-Library Checkmark badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSaved) colors.accent else Color.Black.copy(alpha = 0.65f)
                                )
                                .iosClickable(
                                    onClick = {
                                        if (!isSaved) onQuickAdd(drama) else onDramaClick(drama.id)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSaved) MoglogIcons.Check else MoglogIcons.Plus,
                                contentDescription = if (isSaved) "In Library" else "Add to Planned",
                                tint = if (isSaved) colors.onAccent else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = MoglogIcons.StarFilled,
                            contentDescription = null,
                            tint = colors.starGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", drama.rating),
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
        }
    }
}

@Composable
private fun DiscoverSearchResultRow(
    drama: Drama,
    isSaved: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .iosClickable(pressedScale = 0.99f, onClick = onClick)
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
                    .size(width = 50.dp, height = 72.dp)
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
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = MoglogIcons.StarFilled,
                        contentDescription = null,
                        tint = colors.starGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f", drama.rating),
                        style = typo.caption,
                        color = colors.secondaryLabel
                    )
                    if (drama.releaseYear > 0) {
                        Text(
                            text = "· ${drama.releaseYear}",
                            style = typo.caption,
                            color = colors.secondaryLabel
                        )
                    }
                    if (drama.genres.isNotBlank()) {
                        Text(
                            text = "· ${drama.genreList.take(2).joinToString(", ")}",
                            style = typo.caption,
                            color = colors.tertiaryLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (drama.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = drama.overview,
                        style = typo.footnote,
                        color = colors.secondaryLabel,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Inline one-tap Add (+) -> Checkmark pill
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSaved) colors.accentSubtle else colors.accent)
                    .iosClickable(onClick = onQuickAdd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSaved) MoglogIcons.Check else MoglogIcons.Plus,
                    contentDescription = if (isSaved) "In Library" else "Add to Planned",
                    tint = if (isSaved) colors.accent else colors.onAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                thickness = colors.hairlineWidth,
                color = colors.separator,
                modifier = Modifier.padding(start = 76.dp)
            )
        }
    }
}
