package com.skech.moglog.ui.library

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skech.moglog.R
import com.skech.moglog.data.local.LibraryLayoutMode
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.ui.designsystem.ActionSheetItem
import com.skech.moglog.ui.designsystem.IosActionSheet
import com.skech.moglog.ui.designsystem.IosCircleIconButton
import com.skech.moglog.ui.designsystem.IosEmptyState
import com.skech.moglog.ui.designsystem.IosLargeTitleHeader
import com.skech.moglog.ui.designsystem.IosSearchBar
import com.skech.moglog.ui.designsystem.IosSegmentedControl
import com.skech.moglog.ui.designsystem.IosToastBanner
import com.skech.moglog.ui.designsystem.IosToastData
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.designsystem.rememberMoglogHaptics
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun LibraryScreen(
    dramas: List<Drama>,
    layoutMode: LibraryLayoutMode,
    gridColumns: Int,
    onToggleLayoutMode: (LibraryLayoutMode) -> Unit,
    onDramaClick: (Int) -> Unit,
    onNavigateToDiscover: () -> Unit,
    onUpdateDrama: (Drama) -> Unit,
    onMoveToRecycleBin: (Drama) -> Unit,
    onRestoreDrama: (Drama) -> Unit,
    bottomBarPadding: PaddingValues = PaddingValues(bottom = 84.dp)
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val haptics = rememberMoglogHaptics()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val statusTabs = listOf("All", "Watching", "Planned", "Done")
    var selectedStatus by rememberSaveable { mutableStateOf("All") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterState by remember { mutableStateOf(LibraryFilterState()) }
    var showSortFilterSheet by remember { mutableStateOf(false) }

    // Long-press context menu target
    var contextMenuDrama by remember { mutableStateOf<Drama?>(null) }

    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var showBulkStatusSheet by remember { mutableStateOf(false) }

    // Undo Toast state
    var activeToast by remember { mutableStateOf<IosToastData?>(null) }
    LaunchedEffect(activeToast) {
        if (activeToast != null) {
            delay(3800)
            activeToast = null
        }
    }

    val activeDramas = remember(dramas) { dramas.filter { !it.isDeleted } }
    val watchingContinueList = remember(activeDramas) {
        activeDramas
            .filter { it.status == "Watching" }
            .sortedByDescending { it.updatedAt }
    }
    val availableGenres = remember(activeDramas) {
        activeDramas.flatMap { it.genreList }.distinct().sorted()
    }

    val displayedDramas = remember(activeDramas, selectedStatus, searchQuery, filterState) {
        var list = when (selectedStatus) {
            "All" -> activeDramas
            else -> activeDramas.filter { it.status == selectedStatus }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            list = list.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.notes.contains(q, ignoreCase = true) ||
                    it.tags.contains(q, ignoreCase = true)
            }
        }
        if (filterState.minRating > 0.0) {
            list = list.filter {
                val effective = if (it.myRating > 0.0) it.myRating else it.rating
                effective >= filterState.minRating
            }
        }
        if (filterState.selectedGenres.isNotEmpty()) {
            list = list.filter { drama ->
                val dramaGenres = drama.genreList.toSet()
                filterState.selectedGenres.any { it in dramaGenres }
            }
        }
        if (filterState.onlyWithNotes) {
            list = list.filter { it.notes.isNotBlank() || it.tags.isNotBlank() }
        }
        when (filterState.sortOption) {
            LibrarySortOption.RECENTLY_ADDED -> list.sortedByDescending { it.addedAt }
            LibrarySortOption.RECENTLY_UPDATED -> list.sortedByDescending { it.updatedAt }
            LibrarySortOption.RECENTLY_COMPLETED -> list.sortedByDescending { it.completedAt }
            LibrarySortOption.TMDB_RATING -> list.sortedByDescending { it.rating }
            LibrarySortOption.MY_RATING -> list.sortedByDescending { if (it.myRating > 0) it.myRating else it.rating }
            LibrarySortOption.TITLE_AZ -> list.sortedBy { it.title.lowercase(Locale.ROOT) }
            LibrarySortOption.YEAR -> list.sortedByDescending { it.releaseYear }
        }
    }

    val gridState = rememberLazyGridState()
    val isHeaderCollapsed by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 90
        }
    }

    fun incrementEpisodeWithToast(drama: Drama) {
        val nextEp = drama.episodesWatched + 1
        val cappedEp = if (drama.totalEpisodes > 0) nextEp.coerceAtMost(drama.totalEpisodes) else nextEp
        val newStatus = when {
            drama.totalEpisodes > 0 && cappedEp >= drama.totalEpisodes -> "Done"
            drama.status == "Planned" -> "Watching"
            else -> drama.status
        }
        val previousSnapshot = drama
        onUpdateDrama(
            drama.copy(
                episodesWatched = cappedEp,
                currentEpisode = cappedEp,
                status = newStatus
            )
        )
        activeToast = IosToastData(
            message = if (newStatus == "Done" && drama.status != "Done") {
                "Completed \"${drama.title}\"!"
            } else {
                "${drama.title} · Episode $cappedEp"
            },
            actionLabel = "Undo",
            onAction = { onUpdateDrama(previousSnapshot) }
        )
    }

    fun deleteToBinWithUndo(drama: Drama) {
        onMoveToRecycleBin(drama)
        activeToast = IosToastData(
            message = "Moved \"${drama.title}\" to Recycle Bin",
            actionLabel = "Undo",
            onAction = { onRestoreDrama(drama) }
        )
    }

    fun shareDrama(drama: Drama) {
        val shareText = buildString {
            append("Watching \"${drama.title}\" on Moglog")
            if (drama.myRating > 0.0) {
                append(" ★ ${String.format(Locale.US, "%.1f", drama.myRating)}/10")
            } else if (drama.rating > 0.0) {
                append(" (TMDB ${String.format(Locale.US, "%.1f", drama.rating)}/10)")
            }
            if (drama.episodesWatched > 0) {
                append("\nProgress: Episode ${drama.episodesWatched}")
                if (drama.totalEpisodes > 0) append("/${drama.totalEpisodes}")
            }
            append("\nhttps://www.themoviedb.org/tv/${drama.id}")
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(sendIntent, drama.title))
    }

    if (showSortFilterSheet) {
        SortFilterSheet(
            currentState = filterState,
            availableGenres = availableGenres,
            onApply = { filterState = it },
            onReset = { filterState = LibraryFilterState() },
            onDismiss = { showSortFilterSheet = false }
        )
    }

    if (contextMenuDrama != null) {
        val target = contextMenuDrama!!
        DramaContextMenuSheet(
            drama = target,
            onDismiss = { contextMenuDrama = null },
            onStatusChange = { newStatus ->
                val prev = target
                onUpdateDrama(target.copy(status = newStatus))
                activeToast = IosToastData(
                    message = "Marked \"${target.title}\" as $newStatus",
                    actionLabel = "Undo",
                    onAction = { onUpdateDrama(prev) }
                )
            },
            onIncrementEpisode = { incrementEpisodeWithToast(target) },
            onQuickRate = { score ->
                onUpdateDrama(target.copy(myRating = score))
                activeToast = IosToastData(
                    message = "Rated \"${target.title}\" ${score.toInt()}/10"
                )
            },
            onOpenDetails = { onDramaClick(target.id) },
            onShare = { shareDrama(target) },
            onStartMultiSelect = {
                isSelectionMode = true
                selectedIds = setOf(target.id)
            },
            onMoveToRecycleBin = { deleteToBinWithUndo(target) }
        )
    }

    if (showBulkStatusSheet) {
        IosActionSheet(
            title = "Change Status for ${selectedIds.size} Shows",
            actions = listOf("Watching", "Planned", "Done").map { status ->
                ActionSheetItem(
                    label = "Mark as $status",
                    onClick = {
                        val targets = activeDramas.filter { it.id in selectedIds }
                        targets.forEach { onUpdateDrama(it.copy(status = status)) }
                        isSelectionMode = false
                        selectedIds = emptySet()
                    }
                )
            },
            onDismissRequest = { showBulkStatusSheet = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Collapsing iOS Large Title Bar
            IosLargeTitleHeader(
                title = if (isSelectionMode) {
                    stringResource(R.string.multi_select_title, selectedIds.size)
                } else {
                    stringResource(R.string.library_title)
                },
                collapsed = isHeaderCollapsed,
                leadingActions = if (isSelectionMode) {
                    {
                        Text(
                            text = stringResource(R.string.multi_select_cancel),
                            style = typo.headline,
                            color = colors.accent,
                            modifier = Modifier.iosClickable(
                                hapticOnClick = true,
                                onClick = {
                                    isSelectionMode = false
                                    selectedIds = emptySet()
                                }
                            )
                        )
                    }
                } else null,
                trailingActions = {
                    if (isSelectionMode) {
                        if (selectedIds.isNotEmpty()) {
                            Text(
                                text = "Status",
                                style = typo.subhead.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accent,
                                modifier = Modifier
                                    .iosClickable(
                                        hapticOnClick = true,
                                        onClick = { showBulkStatusSheet = true }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            IosCircleIconButton(
                                icon = MoglogIcons.Trash,
                                contentDescription = "Delete selected",
                                tint = colors.destructive,
                                onClick = {
                                    val targets = activeDramas.filter { it.id in selectedIds }
                                    targets.forEach { onMoveToRecycleBin(it) }
                                    isSelectionMode = false
                                    selectedIds = emptySet()
                                }
                            )
                        }
                    } else {
                        // Layout Toggle (Grid vs List)
                        IosCircleIconButton(
                            icon = if (layoutMode == LibraryLayoutMode.GRID) MoglogIcons.ListRows else MoglogIcons.Grid,
                            contentDescription = stringResource(R.string.toggle_layout_cd),
                            onClick = {
                                val next = if (layoutMode == LibraryLayoutMode.GRID) {
                                    LibraryLayoutMode.LIST
                                } else {
                                    LibraryLayoutMode.GRID
                                }
                                onToggleLayoutMode(next)
                            }
                        )
                        // Sort & Filter Sheet Trigger
                        Box {
                            IosCircleIconButton(
                                icon = MoglogIcons.Filter,
                                contentDescription = stringResource(R.string.sort_and_filter),
                                tint = if (!filterState.isDefault) colors.accent else colors.label,
                                onClick = { showSortFilterSheet = true }
                            )
                            if (!filterState.isDefault) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent)
                                )
                            }
                        }
                        // Add Show (+) -> Discover
                        IosCircleIconButton(
                            icon = MoglogIcons.Plus,
                            contentDescription = stringResource(R.string.add_show_cd),
                            tint = colors.onAccent,
                            containerColor = colors.accent,
                            onClick = onNavigateToDiscover
                        )
                    }
                },
                bottomContent = {
                    IosSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = stringResource(R.string.library_search_placeholder),
                        showCancelButton = searchQuery.isNotEmpty(),
                        onCancel = { searchQuery = "" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    IosSegmentedControl(
                        items = statusTabs,
                        selectedItem = selectedStatus,
                        onItemSelected = { selectedStatus = it },
                        labelProvider = { tab ->
                            val count = when (tab) {
                                "All" -> activeDramas.size
                                else -> activeDramas.count { it.status == tab }
                            }
                            if (count > 0 && tab == selectedStatus) "$tab ($count)" else tab
                        }
                    )
                }
            )

            // Main Content
            if (displayedDramas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottomBarPadding),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeDramas.isEmpty()) {
                        IosEmptyState(
                            icon = MoglogIcons.Library,
                            title = stringResource(R.string.empty_library_title),
                            subtitle = stringResource(R.string.empty_library_subtitle),
                            actionLabel = stringResource(R.string.empty_library_cta),
                            onAction = onNavigateToDiscover
                        )
                    } else {
                        IosEmptyState(
                            icon = MoglogIcons.Search,
                            title = stringResource(R.string.empty_filter_title),
                            subtitle = stringResource(R.string.empty_filter_subtitle),
                            actionLabel = if (!filterState.isDefault || searchQuery.isNotEmpty()) {
                                stringResource(R.string.clear_filters)
                            } else null,
                            onAction = {
                                searchQuery = ""
                                filterState = LibraryFilterState()
                                selectedStatus = "All"
                            }
                        )
                    }
                }
            } else {
                val columns = when {
                    layoutMode == LibraryLayoutMode.LIST -> GridCells.Fixed(1)
                    isLandscape -> GridCells.Adaptive(minSize = 128.dp)
                    else -> GridCells.Fixed(gridColumns.coerceIn(2, 4))
                }

                LazyVerticalGrid(
                    state = gridState,
                    columns = columns,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = bottomBarPadding.calculateBottomPadding() + 20.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (layoutMode == LibraryLayoutMode.GRID) 16.dp else 0.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Persistent Continue Watching Rail when on "All" tab with no active search
                    if (selectedStatus == "All" && searchQuery.isBlank() && watchingContinueList.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "continue_watching_header") {
                            ContinueWatchingSection(
                                watchingDramas = watchingContinueList,
                                onDramaClick = { onDramaClick(it.id) },
                                onIncrementEpisode = { incrementEpisodeWithToast(it) },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    items(displayedDramas, key = { it.id }) { drama ->
                        val isItemSelected = drama.id in selectedIds
                        val handleItemClick = {
                            if (isSelectionMode) {
                                haptics.selectionChanged()
                                selectedIds = if (isItemSelected) {
                                    selectedIds - drama.id
                                } else {
                                    selectedIds + drama.id
                                }
                            } else {
                                onDramaClick(drama.id)
                            }
                        }

                        if (layoutMode == LibraryLayoutMode.GRID) {
                            PosterGridItem(
                                drama = drama,
                                isSelectionMode = isSelectionMode,
                                isSelected = isItemSelected,
                                onClick = handleItemClick,
                                onLongClick = { contextMenuDrama = drama },
                                modifier = Modifier.animateItem()
                            )
                        } else {
                            CompactDramaRow(
                                drama = drama,
                                isSelectionMode = isSelectionMode,
                                isSelected = isItemSelected,
                                onClick = handleItemClick,
                                onLongClick = { contextMenuDrama = drama },
                                onSwipeIncrementEpisode = { incrementEpisodeWithToast(drama) },
                                onSwipeDelete = { deleteToBinWithUndo(drama) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }

        // Floating iOS Undo Toast Banner above bottom tab bar
        IosToastBanner(
            toast = activeToast,
            onDismiss = { activeToast = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomBarPadding.calculateBottomPadding() + 8.dp)
        )
    }
}
