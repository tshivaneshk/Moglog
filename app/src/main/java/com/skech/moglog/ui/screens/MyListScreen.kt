package com.skech.moglog.ui.screens

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.path

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.skech.moglog.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.skech.moglog.ui.viewmodel.AuthViewModel
import coil.compose.AsyncImage
import com.skech.moglog.domain.model.Drama

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    dramas: List<Drama>,
    onNavigateToSearch: () -> Unit,
    onNavigateToWatch: () -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onDramaClick: (Drama) -> Unit,
    onStatusChange: (Int, String) -> Unit = { _, _ -> },
    onUpdateDrama: (Drama) -> Unit = {},
    authViewModel: AuthViewModel? = null,
    onSignInClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var selectedFilter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("All") }
    var sortOrder by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Recently Added") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var minRatingFilter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0.0) }
    
    var searchQuery by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var isLocalSearchActive by remember { mutableStateOf(false) }
    
    val filters = listOf("All", "Watching", "Planned", "Done")
    
    val activeDramas = dramas.filter { !it.isDeleted }
    
    val filteredDramas = when (selectedFilter) {
        "All" -> activeDramas
        else -> activeDramas.filter { it.status == selectedFilter }
    }
    
    val searchedDramas = if (searchQuery.isBlank()) {
        filteredDramas
    } else {
        filteredDramas.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }
    
    val ratedDramas = if (minRatingFilter > 0.0) {
        searchedDramas.filter { it.rating >= minRatingFilter }
    } else {
        searchedDramas
    }
    
    val sortedDramas = when (sortOrder) {
        "Recently Added" -> ratedDramas.sortedByDescending { it.addedAt }
        "Last Updated" -> ratedDramas.sortedByDescending { it.updatedAt }
        "Recently Completed" -> ratedDramas.sortedByDescending { it.completedAt }
        "Highest Rated" -> ratedDramas.sortedByDescending { it.rating }
        "Title (A-Z)" -> ratedDramas.sortedBy { it.title }
        "Recently Updated" -> ratedDramas.sortedByDescending { it.updatedAt }
        else -> ratedDramas
    }

    var showStatsDialog by remember { mutableStateOf(false) }
    if (showStatsDialog) {
        StatisticsChartDialog(
            activeDramas = activeDramas,
            onDismiss = { showStatsDialog = false }
        )
    }
    
    if (showFilterDialog) {
        AdvancedFilterDialog(
            currentTab = selectedFilter,
            currentSort = sortOrder,
            currentMinRating = minRatingFilter,
            onApply = { newSort, newRating ->
                sortOrder = newSort
                minRatingFilter = newRating
                showFilterDialog = false
            },
            onClear = {
                sortOrder = "Recently Added"
                minRatingFilter = 0.0
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerShape = RoundedCornerShape(32.dp),
                modifier = Modifier.width(320.dp).fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = 32.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val currentUser = authViewModel?.currentUser?.collectAsState()?.value
                    val isSyncing = authViewModel?.isSyncing?.collectAsState()?.value ?: false

                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // App Logo at the top
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RetroTvLogo(showText = false)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.List, contentDescription = null) },
                            label = { Text("My List", fontWeight = FontWeight.SemiBold) },
                            selected = true,
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Search My List", fontWeight = FontWeight.SemiBold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                isLocalSearchActive = true
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            label = { Text("Shortcuts", fontWeight = FontWeight.SemiBold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToWatch()
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Star, contentDescription = null) },
                            label = { Text("Statistics", fontWeight = FontWeight.SemiBold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showStatsDialog = true
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            label = { Text("Recycle Bin", fontWeight = FontWeight.SemiBold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToRecycleBin()
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Info, contentDescription = null) },
                            label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToSettings()
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(64.dp))
                    // Auth Profile Section at the bottom
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentUser != null && currentUser.photoUrl != null) {
                            coil.compose.AsyncImage(
                                model = currentUser.photoUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Default.Person, contentDescription = "Guest", tint = Color.LightGray)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (currentUser != null) {
                            Text("Logged in as", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentUser.displayName ?: "Google User", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(currentUser.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = onSignInClick,
                                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Switch", fontSize = 12.sp)
                                }
                                androidx.compose.material3.Button(
                                    onClick = { authViewModel?.signOut() },
                                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Sign Out", fontSize = 12.sp)
                                }
                            }

                            val syncFreq = "Manual Only"
                            if (syncFreq == "Manual Only") {
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { authViewModel?.syncToCloud(dramas) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isSyncing) "Syncing..." else "Sync to Cloud")
                                }
                            }
                        } else {
                            Text("Guest User", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.Button(
                                onClick = onSignInClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Add Google Account")
                            }
                        }
                    }
                }
            }
        }
    }
    ) {
        Scaffold { padding ->
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                
                // Content Grid
                AnimatedContent(
                    targetState = sortedDramas.isEmpty(),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    }, label = "list_content"
                ) { isEmpty ->
                    if (isEmpty) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "No Shows Found",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (selectedFilter == "All" && searchQuery.isBlank()) {
                                    Text(
                                        "Tap the + icon to add some!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = if (isLandscape) GridCells.Adaptive(minSize = 100.dp) else GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 170.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxSize().animateContentSize()
                        ) {
                            items(sortedDramas, key = { it.id }) { drama ->
                                PremiumDramaCard(
                                    drama = drama,
                                    onClick = { onDramaClick(drama) },
                                    onUpdateDrama = onUpdateDrama,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
                
                // Floating Top Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLocalSearchActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search shows...") },
                                singleLine = true,
                                shape = RoundedCornerShape(32.dp),
                                colors = androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { 
                                isLocalSearchActive = false 
                                searchQuery = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                            }
                            
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                RetroTvLogo(showText = !isLandscape)
                            }
                            
                            Row {
                                Box {
                                    IconButton(onClick = { showFilterDialog = true }) {
                                        Icon(customFilterIcon, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                // Floating Action Button
                FloatingActionButton(
                    onClick = onNavigateToSearch,
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 96.dp).size(56.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Show")
                }
                
                // Floating Tab Bar (iPhone Dock Style)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .wrapContentWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp))
                            .padding(4.dp)
                            .height(48.dp)
                            .wrapContentWidth()
                    ) {
                        val segmentWidth = 80.dp
                        val selectedIndex = filters.indexOf(selectedFilter).takeIf { it >= 0 } ?: 0
                        
                        val offset by androidx.compose.animation.core.animateDpAsState(
                            targetValue = segmentWidth * selectedIndex,
                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 400f)
                        )
                        
                        // Animated Bubble
                        Box(
                            modifier = Modifier
                                .offset(x = offset)
                                .width(segmentWidth)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(28.dp))
                        )
                        
                        Row(modifier = Modifier.wrapContentWidth()) {
                            filters.forEach { filter ->
                                val isSelected = filter == selectedFilter
                                Box(
                                    modifier = Modifier
                                        .width(segmentWidth)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) { selectedFilter = filter },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = filter,
                                        color = if (isSelected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Gray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
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
fun RetroTvLogo(showText: Boolean = true) {
    Box(
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).wrapContentWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Antennae
        val antennaColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        Canvas(modifier = Modifier.width(40.dp).height(12.dp).align(Alignment.TopCenter).offset(y = (-10).dp)) {
            drawLine(
                color = antennaColor,
                start = Offset(size.width / 2, size.height),
                end = Offset(0f, 0f),
                strokeWidth = 4f
            )
            drawLine(
                color = antennaColor,
                start = Offset(size.width / 2, size.height),
                end = Offset(size.width, 0f),
                strokeWidth = 4f
            )
        }
        
        // Glowing Backdrop
        Box(
            modifier = Modifier
                .width(if (showText) 170.dp else 60.dp)
                .height(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // TV Body
        Box(
            modifier = Modifier
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color(0xFF2ECC71), // Emerald
                            androidx.compose.ui.graphics.Color(0xFF0C86F3), // Blue
                            androidx.compose.ui.graphics.Color(0xFF5E5CE6), // Purple
                            androidx.compose.ui.graphics.Color(0xFF94C105)  // Lime Green
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = if (showText) 24.dp else 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (showText) "Moglog" else "M",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineMedium.copy(
                    shadow = Shadow(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        blurRadius = 30f
                    )
                ),
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 2.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PremiumDramaCard(
    drama: Drama,
    onClick: () -> Unit,
    onUpdateDrama: (Drama) -> Unit = {},
    authViewModel: AuthViewModel? = null,
    onSignInClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showStatus: Boolean = true
) {
    val statusColor = when(drama.status) {
        "Watching" -> Color(0xFFFF9800) // Vibrant Orange
        "Planned" -> Color(0xFF2196F3)  // Bright Blue
        "Done" -> Color(0xFF4CAF50)     // Bright Green
        else -> MaterialTheme.colorScheme.secondary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "bounce"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.65f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ) { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(drama.posterPath)
                    .crossfade(true)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient Overlay
            val bgColor = MaterialTheme.colorScheme.background
            val gradient = remember(bgColor) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        bgColor.copy(alpha = 0.3f),
                        bgColor.copy(alpha = 0.95f)
                    ),
                    startY = 100f
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient)
            )

            // Status Badge top right
            if (showStatus) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(statusColor.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = drama.status,
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = drama.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⭐", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = drama.rating.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsChartDialog(
    activeDramas: List<Drama>,
    onDismiss: () -> Unit
) {
    val totalDramas = activeDramas.size
    val watching = activeDramas.count { it.status == "Watching" }
    val planned = activeDramas.count { it.status == "Planned" }
    val done = activeDramas.count { it.status == "Done" }
    val episodesWatched = activeDramas.sumOf { it.episodesWatched }

    val watchingColor = Color(0xFFFF9800)
    val plannedColor = Color(0xFF2196F3)
    val doneColor = Color(0xFF4CAF50)

    val slices = listOf(
        watching.toFloat() to watchingColor,
        planned.toFloat() to plannedColor,
        done.toFloat() to doneColor
    )
    val totalSlices = slices.sumOf { it.first.toDouble() }.toFloat()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Statistics", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (totalSlices > 0) {
                    var animationPlayed by remember { mutableStateOf(false) }
                    val sweepAngle by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (animationPlayed) 360f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "pie_chart_anim"
                    )
                    
                    LaunchedEffect(Unit) { animationPlayed = true }

                    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
                            var startAngle = -90f
                            slices.forEach { (value, color) ->
                                if (value > 0) {
                                    val angle = (value / totalSlices) * sweepAngle
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = angle,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 45f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                    startAngle += angle
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$totalDramas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Box(modifier = Modifier.height(160.dp), contentAlignment = Alignment.Center) {
                        Text("No shows tracked yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LegendItem("Watching", watching, watchingColor)
                    LegendItem("Planned", planned, plannedColor)
                    LegendItem("Done", done, doneColor)
                }
                
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Awesome", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun LegendItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "$count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}




val customFilterIcon: ImageVector
    get() = ImageVector.Builder(
        name = "FilterList",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(androidx.compose.ui.graphics.Color.White)) {
            moveTo(10f, 18f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(-4f)
            verticalLineToRelative(2f)
            close()
            moveTo(3f, 6f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(18f)
            verticalLineTo(6f)
            horizontalLineTo(3f)
            close()
            moveTo(6f, 13f)
            horizontalLineToRelative(12f)
            verticalLineToRelative(-2f)
            horizontalLineTo(6f)
            verticalLineToRelative(2f)
            close()
        }
    }.build()


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterDialog(
    currentTab: String,
    currentSort: String,
    currentMinRating: Double,
    onApply: (String, Double) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var sortSelection by remember { mutableStateOf(currentSort) }
    var minRatingSelection by remember { mutableStateOf(currentMinRating) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Filter & Sort ($currentTab)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // Sort Options
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                val sortOptions = mutableListOf<String>()
                when (currentTab) {
                    "Done" -> {
                        sortOptions.add("Recently Completed")
                        sortOptions.add("Highest Rated")
                        sortOptions.add("Title (A-Z)")
                    }
                    "Watching" -> {
                        sortOptions.add("Title (A-Z)")
                        sortOptions.add("Recently Updated")
                        sortOptions.add("Highest Rated")
                    }
                    else -> {
                        sortOptions.add("Recently Added")
                        sortOptions.add("Highest Rated")
                        sortOptions.add("Title (A-Z)")
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortOptions.forEach { option ->
                        val isSelected = sortSelection == option
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { sortSelection = option }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Options
                Text(
                    text = "Minimum Rating",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                val ratingOptions = listOf(0.0 to "Any", 8.0 to "8.0+", 9.0 to "9.0+")
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ratingOptions.forEach { (value, label) ->
                        val isSelected = minRatingSelection == value
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { minRatingSelection = value }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentSort != "Recently Added" || currentMinRating > 0.0) {
                        TextButton(onClick = onClear) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onApply(sortSelection, minRatingSelection) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}
