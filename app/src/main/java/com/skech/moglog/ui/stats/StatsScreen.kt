package com.skech.moglog.ui.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.skech.moglog.ui.designsystem.IosEmptyState
import com.skech.moglog.ui.designsystem.IosLargeTitleHeader
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.SmoothRoundedCornerShape
import com.skech.moglog.ui.designsystem.iosClickable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun StatsScreen(
    dramas: List<Drama>,
    onDramaClick: (Int) -> Unit,
    bottomBarPadding: PaddingValues = PaddingValues(bottom = 84.dp)
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val context = LocalContext.current

    val activeDramas = remember(dramas) { dramas.filter { !it.isDeleted } }
    val watchingCount = remember(activeDramas) { activeDramas.count { it.status == "Watching" } }
    val plannedCount = remember(activeDramas) { activeDramas.count { it.status == "Planned" } }
    val doneCount = remember(activeDramas) { activeDramas.count { it.status == "Done" } }
    val totalEpisodesWatched = remember(activeDramas) {
        activeDramas.sumOf {
            if (it.status == "Done" && it.episodesWatched == 0 && it.totalEpisodes > 0) {
                it.totalEpisodes
            } else {
                it.episodesWatched
            }
        }
    }
    // Average 45 min per drama episode
    val estimatedHours = remember(totalEpisodesWatched) { (totalEpisodesWatched * 45) / 60 }

    val genreBreakdown = remember(activeDramas) {
        activeDramas
            .flatMap { it.genreList }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(6)
    }

    // Monthly activity (last 12 months) based on completedAt / updatedAt / addedAt
    val monthlyActivity = remember(activeDramas) {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("MMM", Locale.US)
        val buckets = mutableListOf<Pair<String, Int>>()
        for (i in 11 downTo 0) {
            val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -i) }
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val label = fmt.format(c.time)
            val count = activeDramas.count { d ->
                val ts = when {
                    d.completedAt > 0L -> d.completedAt
                    d.updatedAt > 0L -> d.updatedAt
                    else -> d.addedAt
                }
                if (ts <= 0L) false else {
                    val dc = Calendar.getInstance().apply { timeInMillis = ts }
                    dc.get(Calendar.YEAR) == year && dc.get(Calendar.MONTH) == month
                }
            }
            buckets.add(label to count)
        }
        buckets
    }

    val currentStreakMonths = remember(monthlyActivity) {
        var streak = 0
        for (i in monthlyActivity.indices.reversed()) {
            if (monthlyActivity[i].second > 0) streak++ else break
        }
        streak
    }

    val topRatedShows = remember(activeDramas) {
        activeDramas
            .sortedByDescending { if (it.myRating > 0.0) it.myRating else it.rating }
            .take(5)
    }

    val listState = rememberLazyListState()
    val isHeaderCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 90
        }
    }

    var animateCharts by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateCharts = true }
    val chartProgress by animateFloatAsState(
        targetValue = if (animateCharts) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "stats_chart_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        IosLargeTitleHeader(
            title = stringResource(R.string.stats_title),
            collapsed = isHeaderCollapsed
        )

        if (activeDramas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottomBarPadding),
                contentAlignment = Alignment.Center
            ) {
                IosEmptyState(
                    icon = MoglogIcons.Stats,
                    title = stringResource(R.string.stats_title),
                    subtitle = stringResource(R.string.stats_empty_subtitle)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = bottomBarPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 2x2 Summary Metric Grid (Apple Health style)
                item(key = "summary_metrics") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatSummaryCard(
                                label = stringResource(R.string.stats_total_shows),
                                value = "${activeDramas.size}",
                                subtitle = "$doneCount completed",
                                accentColor = colors.accent,
                                modifier = Modifier.weight(1f)
                            )
                            StatSummaryCard(
                                label = stringResource(R.string.stats_episodes_watched),
                                value = "$totalEpisodesWatched",
                                subtitle = "Across all shows",
                                accentColor = colors.statusWatching,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatSummaryCard(
                                label = stringResource(R.string.stats_hours_watched),
                                value = "${estimatedHours}h",
                                subtitle = "~${(estimatedHours / 24.0).let { String.format(Locale.US, "%.1f", it) }} days",
                                accentColor = colors.statusPlanned,
                                modifier = Modifier.weight(1f)
                            )
                            StatSummaryCard(
                                label = stringResource(R.string.stats_completion_streak),
                                value = "${currentStreakMonths}m",
                                subtitle = "Active months",
                                accentColor = colors.starGold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Status Donut Card
                item(key = "status_donut") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MoglogShapes.Card)
                            .background(colors.secondaryBackground)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.stats_status_breakdown),
                            style = typo.headline,
                            color = colors.label
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Box(
                                modifier = Modifier.size(136.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val slices = listOf(
                                    watchingCount.toFloat() to colors.statusWatching,
                                    plannedCount.toFloat() to colors.statusPlanned,
                                    doneCount.toFloat() to colors.statusDone
                                )
                                val total = slices.sumOf { it.first.toDouble() }.toFloat().coerceAtLeast(1f)

                                Canvas(modifier = Modifier.size(116.dp)) {
                                    var startAngle = -90f
                                    slices.forEach { (count, sliceColor) ->
                                        if (count > 0f) {
                                            val sweep = (count / total) * 360f * chartProgress
                                            drawArc(
                                                color = sliceColor,
                                                startAngle = startAngle,
                                                sweepAngle = sweep,
                                                useCenter = false,
                                                style = Stroke(width = 32f, cap = StrokeCap.Butt)
                                            )
                                            startAngle += sweep
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${activeDramas.size}",
                                        style = typo.title2,
                                        color = colors.label
                                    )
                                    Text(
                                        text = "Shows",
                                        style = typo.caption,
                                        color = colors.secondaryLabel
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatusLegendRow("Watching", watchingCount, colors.statusWatching)
                                StatusLegendRow("Planned", plannedCount, colors.statusPlanned)
                                StatusLegendRow("Done", doneCount, colors.statusDone)
                            }
                        }
                    }
                }

                // Genre Breakdown Card
                if (genreBreakdown.isNotEmpty()) {
                    item(key = "genre_breakdown") {
                        val maxGenreCount = genreBreakdown.maxOf { it.value }.coerceAtLeast(1)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MoglogShapes.Card)
                                .background(colors.secondaryBackground)
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.stats_genre_breakdown),
                                style = typo.headline,
                                color = colors.label
                            )
                            genreBreakdown.forEach { (genre, count) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = genre,
                                            style = typo.subhead.copy(fontWeight = FontWeight.Medium),
                                            color = colors.label
                                        )
                                        Text(
                                            text = "$count",
                                            style = typo.footnote,
                                            color = colors.secondaryLabel
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(CircleShape)
                                            .background(colors.tertiaryBackground)
                                    ) {
                                        val fraction = (count.toFloat() / maxGenreCount.toFloat()) * chartProgress
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                                                .height(7.dp)
                                                .clip(CircleShape)
                                                .background(colors.accent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 12-Month Activity Bar Chart
                item(key = "monthly_activity") {
                    val maxMonth = monthlyActivity.maxOf { it.second }.coerceAtLeast(1)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MoglogShapes.Card)
                            .background(colors.secondaryBackground)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.stats_monthly_activity),
                            style = typo.headline,
                            color = colors.label
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            monthlyActivity.forEach { (monthLabel, count) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    val barFraction = if (count == 0) 0.06f else (count.toFloat() / maxMonth.toFloat()) * chartProgress
                                    Box(
                                        modifier = Modifier
                                            .width(14.dp)
                                            .weight(1f, fill = false)
                                            .fillMaxHeight(barFraction.coerceIn(0.06f, 1f))
                                            .clip(SmoothRoundedCornerShape(4.dp))
                                            .background(if (count > 0) colors.statusPlanned else colors.tertiaryBackground)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = monthLabel.take(1),
                                        style = typo.caption,
                                        color = colors.secondaryLabel
                                    )
                                }
                            }
                        }
                    }
                }

                // Top-Rated Shows List
                if (topRatedShows.isNotEmpty()) {
                    item(key = "top_rated") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MoglogShapes.Card)
                                .background(colors.secondaryBackground)
                        ) {
                            Text(
                                text = stringResource(R.string.stats_top_rated),
                                style = typo.headline,
                                color = colors.label,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                            topRatedShows.forEachIndexed { index, drama ->
                                val score = if (drama.myRating > 0.0) drama.myRating else drama.rating
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .iosClickable(onClick = { onDramaClick(drama.id) })
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = typo.headline,
                                        color = colors.secondaryLabel,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(drama.posterPath)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = drama.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 38.dp, height = 54.dp)
                                            .clip(SmoothRoundedCornerShape(6.dp))
                                            .background(colors.tertiaryBackground)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = drama.title,
                                            style = typo.subhead.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = drama.status,
                                            style = typo.caption,
                                            color = colors.secondaryLabel
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = MoglogIcons.StarFilled,
                                            contentDescription = null,
                                            tint = if (drama.myRating > 0.0) colors.accent else colors.starGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f", score),
                                            style = typo.subhead.copy(fontWeight = FontWeight.Bold),
                                            color = colors.label
                                        )
                                    }
                                }
                                if (index < topRatedShows.lastIndex) {
                                    HorizontalDivider(
                                        thickness = colors.hairlineWidth,
                                        color = colors.separator,
                                        modifier = Modifier.padding(start = 90.dp)
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

@Composable
private fun StatSummaryCard(
    label: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current

    Column(
        modifier = modifier
            .clip(MoglogShapes.Card)
            .background(colors.secondaryBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = typo.caption.copy(fontWeight = FontWeight.SemiBold),
            color = accentColor
        )
        Text(
            text = value,
            style = typo.largeTitle,
            color = colors.label
        )
        Text(
            text = subtitle,
            style = typo.caption,
            color = colors.secondaryLabel
        )
    }
}

@Composable
private fun StatusLegendRow(
    label: String,
    count: Int,
    color: Color
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = typo.subhead,
            color = colors.secondaryLabel,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = "$count",
            style = typo.subhead.copy(fontWeight = FontWeight.Bold),
            color = colors.label
        )
    }
}
