package com.skech.moglog.ui.detail

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.skech.moglog.R
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.domain.model.DramaExtraDetails
import com.skech.moglog.domain.model.Shortcut
import com.skech.moglog.ui.designsystem.ActionSheetItem
import com.skech.moglog.ui.designsystem.IosActionSheet
import com.skech.moglog.ui.designsystem.IosCircleIconButton
import com.skech.moglog.ui.designsystem.IosFrostedBar
import com.skech.moglog.ui.designsystem.IosPrimaryButton
import com.skech.moglog.ui.designsystem.IosSecondaryButton
import com.skech.moglog.ui.designsystem.IosSegmentedControl
import com.skech.moglog.ui.designsystem.IosToastBanner
import com.skech.moglog.ui.designsystem.IosToastData
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.designsystem.rememberMoglogHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Full-featured iOS Detail Screen:
 * - Full-bleed poster header with parallax scroll + gradient blend into background
 * - Optional Dynamic Tint extracted from poster via Palette
 * - Translucent circular Back and "…" action buttons
 * - Status segmented control (Watching / Planned / Done)
 * - Episode & Season stepper with auto-promote Planned -> Watching and completion sheet Watching -> Done
 * - Personal rating (0–10) & native autosaving notes/tags
 * - Expandable overview, Trailer & Web buttons, Regional Watch Providers, Quick Launch Shortcut, Cast, and Similar Shows rails
 * - Sticky bottom primary action bar ("Add to Library" / "Update Library")
 */
@Composable
fun DetailScreen(
    drama: Drama,
    isAlreadyInList: Boolean,
    dynamicPosterTintEnabled: Boolean,
    initialShortcut: Shortcut?,
    onSaveShortcut: (Shortcut?) -> Unit,
    fetchExtraDetails: suspend (Int) -> DramaExtraDetails?,
    onBack: () -> Unit,
    onSaveDrama: (Drama) -> Unit,
    onDeleteDrama: (Drama) -> Unit,
    onNavigateToDrama: (Int) -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()
    val context = LocalContext.current

    var selectedStatus by remember(drama.id, drama.status) { mutableStateOf(drama.status) }
    var episodesWatched by remember(drama.id, drama.episodesWatched) { mutableIntStateOf(drama.episodesWatched) }
    var currentSeason by remember(drama.id, drama.currentSeason) { mutableIntStateOf(drama.currentSeason.coerceAtLeast(1)) }
    var myRating by remember(drama.id, drama.myRating) { mutableDoubleStateOf(drama.myRating) }
    var notes by remember(drama.id, drama.notes) { mutableStateOf(drama.notes) }
    var tags by remember(drama.id, drama.tags) { mutableStateOf(drama.tags) }
    var currentShortcut by remember(initialShortcut) { mutableStateOf(initialShortcut) }

    var extraDetails by remember(drama.id) { mutableStateOf<DramaExtraDetails?>(null) }
    var overviewExpanded by remember { mutableStateOf(false) }
    var showMoreMenuSheet by remember { mutableStateOf(false) }
    var showPromoteToDoneSheet by remember { mutableStateOf(false) }
    var dynamicAccent by remember(drama.id) { mutableStateOf<Color?>(null) }
    var toast by remember { mutableStateOf<IosToastData?>(null) }

    val effectiveAccent = dynamicAccent ?: colors.accent

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2500)
            toast = null
        }
    }

    LaunchedEffect(drama.id) {
        extraDetails = fetchExtraDetails(drama.id)
    }

    // Extract dynamic poster tint via Palette if enabled
    LaunchedEffect(drama.posterPath, dynamicPosterTintEnabled) {
        if (!dynamicPosterTintEnabled || drama.posterPath.isBlank()) {
            dynamicAccent = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(drama.posterPath)
                    .allowHardware(false)
                    .build()
                val res = loader.execute(req)
                val bmp = ((res as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                if (bmp != null) {
                    val palette = Palette.from(bmp).generate()
                    val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.dominantSwatch
                    if (swatch != null) {
                        dynamicAccent = Color(swatch.rgb)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    val totalEpisodes = remember(drama.totalEpisodes, extraDetails) {
        if (drama.totalEpisodes > 0) drama.totalEpisodes else extraDetails?.totalEpisodes ?: 0
    }
    val totalSeasons = remember(drama.totalSeasons, extraDetails) {
        if (drama.totalSeasons > 0) drama.totalSeasons else extraDetails?.totalSeasons ?: 0
    }
    val releaseYear = remember(drama.releaseYear, extraDetails) {
        if (drama.releaseYear > 0) drama.releaseYear else extraDetails?.releaseYear ?: 0
    }
    val genresList = remember(drama.genres, extraDetails) {
        drama.genreList.ifEmpty { extraDetails?.genres ?: emptyList() }
    }

    fun buildCurrentDramaSnapshot(
        statusOverride: String = selectedStatus,
        episodesOverride: Int = episodesWatched
    ): Drama {
        return drama.copy(
            status = statusOverride,
            episodesWatched = episodesOverride,
            totalEpisodes = totalEpisodes,
            currentSeason = currentSeason,
            currentEpisode = episodesOverride,
            totalSeasons = totalSeasons,
            releaseYear = releaseYear,
            genres = genresList.joinToString(", "),
            myRating = myRating,
            notes = notes,
            tags = tags,
            nextEpisodeAirDate = extraDetails?.nextEpisodeAirDate ?: drama.nextEpisodeAirDate,
            nextEpisodeNumber = extraDetails?.nextEpisodeNumber ?: drama.nextEpisodeNumber,
            nextSeasonNumber = extraDetails?.nextSeasonNumber ?: drama.nextSeasonNumber
        )
    }

    // Autosave changes when show is already in the user's library
    LaunchedEffect(selectedStatus, episodesWatched, currentSeason, myRating, notes, tags) {
        if (isAlreadyInList) {
            delay(350)
            onSaveDrama(buildCurrentDramaSnapshot())
        }
    }

    fun shareShow() {
        val text = buildString {
            append(drama.title)
            if (releaseYear > 0) append(" ($releaseYear)")
            if (myRating > 0.0) append(" — My Rating: ${String.format(Locale.US, "%.1f", myRating)}/10")
            append("\nhttps://www.themoviedb.org/tv/${drama.id}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, drama.title))
    }

    if (showPromoteToDoneSheet) {
        IosActionSheet(
            title = stringResource(R.string.detail_promote_to_done_title, drama.title),
            message = stringResource(R.string.detail_promote_to_done_subtitle),
            actions = listOf(
                ActionSheetItem(
                    label = stringResource(R.string.detail_mark_as_done),
                    icon = MoglogIcons.Check,
                    onClick = {
                        selectedStatus = "Done"
                        onSaveDrama(buildCurrentDramaSnapshot(statusOverride = "Done"))
                    }
                ),
                ActionSheetItem(
                    label = stringResource(R.string.detail_keep_watching),
                    onClick = {
                        onSaveDrama(buildCurrentDramaSnapshot(statusOverride = "Watching"))
                    }
                )
            ),
            onDismissRequest = { showPromoteToDoneSheet = false }
        )
    }

    if (showMoreMenuSheet) {
        val actions = buildList {
            add(
                ActionSheetItem(
                    label = stringResource(R.string.action_share),
                    icon = MoglogIcons.Share,
                    onClick = { shareShow() }
                )
            )
            add(
                ActionSheetItem(
                    label = "Open on TMDB in Browser",
                    icon = MoglogIcons.ExternalLink,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/tv/${drama.id}"))
                        )
                    }
                )
            )
            if (isAlreadyInList) {
                add(
                    ActionSheetItem(
                        label = stringResource(R.string.action_move_to_bin),
                        icon = MoglogIcons.Trash,
                        isDestructive = true,
                        onClick = { onDeleteDrama(drama) }
                    )
                )
            }
        }
        IosActionSheet(
            title = drama.title,
            actions = actions,
            onDismissRequest = { showMoreMenuSheet = false }
        )
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Full-bleed Parallax Poster Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .graphicsLayer {
                    translationY = -scrollState.value * 0.35f
                }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                colors.background.copy(alpha = 0.85f),
                                colors.background
                            )
                        )
                    )
            )
        }

        // Scrollable Detail Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 110.dp)
        ) {
            Spacer(modifier = Modifier.height(320.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Title + Metadata Line
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isAlreadyInList) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(effectiveAccent)
                            )
                            Text(
                                text = stringResource(R.string.detail_in_library).uppercase(),
                                style = typo.caption.copy(fontWeight = FontWeight.Bold),
                                color = effectiveAccent
                            )
                        }
                    }

                    Text(
                        text = drama.title,
                        style = typo.largeTitle,
                        color = colors.label
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = MoglogIcons.StarFilled,
                                contentDescription = null,
                                tint = colors.starGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f", drama.rating),
                                style = typo.subhead.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.label
                            )
                        }
                        if (releaseYear > 0) {
                            Text("· $releaseYear", style = typo.subhead, color = colors.secondaryLabel)
                        }
                        if (totalSeasons > 0) {
                            val seasonLabel = if (totalSeasons == 1) "1 Season" else "$totalSeasons Seasons"
                            Text("· $seasonLabel", style = typo.subhead, color = colors.secondaryLabel)
                        }
                        if (genresList.isNotEmpty()) {
                            Text(
                                text = "· ${genresList.take(2).joinToString(", ")}",
                                style = typo.subhead,
                                color = colors.secondaryLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Status Segmented Control
                val statuses = listOf("Watching", "Planned", "Done")
                IosSegmentedControl(
                    items = statuses,
                    selectedItem = selectedStatus,
                    onItemSelected = { newStatus ->
                        selectedStatus = newStatus
                        if (newStatus == "Done" && totalEpisodes > 0 && episodesWatched == 0) {
                            episodesWatched = totalEpisodes
                        }
                    },
                    labelProvider = { it }
                )

                // Episode & Season Progress Stepper
                ProgressStepperCard(
                    episodesWatched = episodesWatched,
                    totalEpisodes = totalEpisodes,
                    currentSeason = currentSeason,
                    totalSeasons = totalSeasons,
                    seasons = extraDetails?.seasons ?: emptyList(),
                    nextEpisodeAirDate = extraDetails?.nextEpisodeAirDate ?: drama.nextEpisodeAirDate,
                    nextSeasonNumber = extraDetails?.nextSeasonNumber ?: drama.nextSeasonNumber,
                    nextEpisodeNumber = extraDetails?.nextEpisodeNumber ?: drama.nextEpisodeNumber,
                    accentColor = effectiveAccent,
                    onSeasonChange = { currentSeason = it },
                    onEpisodesWatchedChange = { newCount ->
                        val prevCount = episodesWatched
                        episodesWatched = newCount
                        // Auto-promote Planned -> Watching on first episode
                        if (prevCount == 0 && newCount > 0 && selectedStatus == "Planned") {
                            selectedStatus = "Watching"
                            toast = IosToastData(message = "Moved to Watching")
                        }
                        // Prompt Watching -> Done when reaching final episode
                        if (totalEpisodes > 0 && newCount >= totalEpisodes && selectedStatus == "Watching") {
                            showPromoteToDoneSheet = true
                        }
                    }
                )

                // Overview Card with "more" expand
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MoglogShapes.Card)
                        .background(colors.secondaryBackground)
                        .animateContentSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.detail_synopsis_title),
                        style = typo.headline,
                        color = colors.label
                    )
                    val overviewText = drama.overview.ifBlank { stringResource(R.string.detail_synopsis_empty) }
                    Text(
                        text = overviewText,
                        style = typo.subhead,
                        color = colors.secondaryLabel,
                        maxLines = if (overviewExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (overviewText.length > 160) {
                        Text(
                            text = if (overviewExpanded) stringResource(R.string.detail_read_less) else stringResource(R.string.detail_read_more),
                            style = typo.footnote.copy(fontWeight = FontWeight.SemiBold),
                            color = effectiveAccent,
                            modifier = Modifier.iosClickable(
                                hapticOnClick = true,
                                onClick = { overviewExpanded = !overviewExpanded }
                            )
                        )
                    }

                    // Trailer + Web Search Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val trailerKey = extraDetails?.trailerKey
                        if (!trailerKey.isNullOrBlank()) {
                            IosSecondaryButton(
                                text = stringResource(R.string.detail_watch_trailer),
                                leadingIcon = MoglogIcons.Play,
                                contentColor = colors.destructive,
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$trailerKey"))
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        IosSecondaryButton(
                            text = stringResource(R.string.detail_search_web),
                            leadingIcon = MoglogIcons.ExternalLink,
                            contentColor = effectiveAccent,
                            onClick = {
                                val url = "https://www.google.com/search?q=${Uri.encode("${drama.title} TV show")}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // My Rating & Native Per-Show Notes / Tags
                RatingAndNotesCard(
                    myRating = myRating,
                    onRatingChange = { myRating = it },
                    notes = notes,
                    onNotesChange = { notes = it },
                    tags = tags,
                    onTagsChange = { tags = it },
                    accentColor = effectiveAccent
                )

                // Where to Watch + Quick Launch Shortcut
                WatchProvidersAndShortcutSection(
                    dramaId = drama.id,
                    dramaTitle = drama.title,
                    watchProviders = extraDetails?.watchProviders ?: emptyList(),
                    shortcut = currentShortcut,
                    accentColor = effectiveAccent,
                    onSaveShortcut = { newShortcut ->
                        currentShortcut = newShortcut
                        onSaveShortcut(newShortcut)
                    }
                )

                // Cast Row
                val cast = extraDetails?.cast.orEmpty()
                if (cast.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.detail_cast_title),
                            style = typo.title3,
                            color = colors.label,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(cast, key = { "${it.name}_${it.character}" }) { member ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(78.dp)
                                ) {
                                    if (member.profilePath != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(member.profilePath)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = member.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(colors.secondaryBackground)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(colors.secondaryBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = MoglogIcons.Person,
                                                contentDescription = null,
                                                tint = colors.secondaryLabel
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = member.name,
                                        style = typo.caption.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = member.character,
                                        style = typo.caption,
                                        color = colors.secondaryLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Similar / Recommended Shows Rail
                val recommendations = extraDetails?.recommendations.orEmpty()
                if (recommendations.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.detail_similar_title),
                            style = typo.title3,
                            color = colors.label,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(recommendations, key = { "rec_${it.id}" }) { rec ->
                                Column(
                                    modifier = Modifier
                                        .width(116.dp)
                                        .iosClickable(
                                            pressedScale = 0.96f,
                                            onClick = { onNavigateToDrama(rec.id) }
                                        )
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(rec.posterPath)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = rec.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.68f)
                                            .clip(MoglogShapes.Poster)
                                            .background(colors.secondaryBackground)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = rec.title,
                                        style = typo.caption.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Floating Translucent Navigation Buttons (Back chevron & "…" menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosCircleIconButton(
                icon = MoglogIcons.ChevronLeft,
                contentDescription = stringResource(R.string.detail_back_cd),
                onClick = onBack,
                size = 38.dp
            )
            IosCircleIconButton(
                icon = MoglogIcons.MoreHorizontal,
                contentDescription = stringResource(R.string.detail_more_cd),
                onClick = { showMoreMenuSheet = true },
                size = 38.dp
            )
        }

        // Sticky Bottom Primary Action Bar
        IosFrostedBar(
            showTopBorder = true,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IosPrimaryButton(
                    text = if (isAlreadyInList) {
                        stringResource(R.string.detail_save_changes)
                    } else {
                        stringResource(R.string.detail_add_to_library)
                    },
                    containerColor = effectiveAccent,
                    onClick = {
                        haptics.success()
                        onSaveDrama(buildCurrentDramaSnapshot())
                        if (!isAlreadyInList) {
                            toast = IosToastData(message = "Added \"${drama.title}\" to $selectedStatus")
                        } else {
                            onBack()
                        }
                    }
                )
            }
        }

        IosToastBanner(
            toast = toast,
            onDismiss = { toast = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp)
        )
    }
}
