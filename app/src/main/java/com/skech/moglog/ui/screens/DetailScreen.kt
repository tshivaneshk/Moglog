package com.skech.moglog.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import coil.compose.AsyncImage
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.domain.model.Shortcut
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    drama: Drama,
    onBack: () -> Unit,
    onSaveDrama: (Drama) -> Unit,
    onDeleteDrama: (Drama) -> Unit = {},
    isAlreadyInList: Boolean = false,
    shortcutJson: String? = null,
    onSaveShortcut: (String?) -> Unit = {},
    fetchExtraDetails: suspend (Int) -> com.skech.moglog.domain.model.DramaExtraDetails? = { null }
) {
    var selectedStatus by remember { mutableStateOf(drama.status) }
    var episodesWatched by remember { mutableStateOf(drama.episodesWatched) }
    val statuses = listOf("Watching", "Planned", "Done")
    val context = LocalContext.current
    val pm = context.packageManager
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showAppDialog by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    
    var currentShortcut by remember(shortcutJson) {
        mutableStateOf(if (shortcutJson != null) Gson().fromJson(shortcutJson, Shortcut::class.java) else null)
    }

    var extraDetails by remember { mutableStateOf<com.skech.moglog.domain.model.DramaExtraDetails?>(null) }
    
    LaunchedEffect(drama.id) {
        extraDetails = fetchExtraDetails(drama.id)
    }

    LaunchedEffect(showAppDialog) {
        if (showAppDialog && installedApps.isEmpty()) {
            withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                val apps = resolveInfos.map {
                    InstalledApp(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName,
                        icon = it.loadIcon(pm)
                    )
                }.distinctBy { it.packageName }.sortedBy { it.name }
                withContext(Dispatchers.Main) {
                    installedApps = apps
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Shortcut", fontWeight = FontWeight.Bold) },
                text = { Text("What type of shortcut do you want to add for this show?") },
                confirmButton = {
                    TextButton(onClick = {
                        showAddDialog = false
                        showAppDialog = true
                    }) { Text("App", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddDialog = false
                        showLinkDialog = true
                    }) { Text("Link", fontWeight = FontWeight.Bold) }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showLinkDialog) {
            var linkTitle by remember { mutableStateOf("") }
            var linkUrl by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showLinkDialog = false },
                title = { Text("Add Link", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = linkTitle,
                            onValueChange = { linkTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = linkUrl,
                            onValueChange = { linkUrl = it },
                            label = { Text("URL (https://...)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (linkTitle.isNotBlank() && linkUrl.isNotBlank()) {
                            val url = if (linkUrl.startsWith("http")) linkUrl else "https://$linkUrl"
                            val host = try { java.net.URL(url).host } catch (e: Exception) { "" }
                            val autoIconUrl = if (host.isNotEmpty()) "https://www.google.com/s2/favicons?domain=${host}&sz=128" else null
                            val newShortcut = Shortcut(title = linkTitle, data = url, type = "LINK", iconUrl = autoIconUrl)
                            currentShortcut = newShortcut
                            onSaveShortcut(Gson().toJson(newShortcut))
                            showLinkDialog = false
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showLinkDialog = false }) { Text("Cancel") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showAppDialog) {
            AlertDialog(
                onDismissRequest = { showAppDialog = false },
                title = { Text("Select App", fontWeight = FontWeight.Bold) },
                text = {
                    if (installedApps.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                            items(installedApps) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newShortcut = Shortcut(title = app.name, data = app.packageName, type = "APP")
                                            currentShortcut = newShortcut
                                            onSaveShortcut(Gson().toJson(newShortcut))
                                            showAppDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon.toImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Box(modifier = Modifier.size(40.dp).background(Color.Gray, CircleShape))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(app.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAppDialog = false }) { Text("Close") }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left side: Poster
                Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    ),
                                    startX = 0f
                                )
                            )
                    )
                }
                
                // Right side: Content
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = drama.title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    if (isAlreadyInList) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Already in your list", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⭐", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${drama.rating} / 10",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Synopsis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = drama.overview.ifBlank { "No synopsis available." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    


                    Text(
                        text = "Set Status",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        statuses.forEach { status ->
                            val isSelected = selectedStatus == status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { selectedStatus = status },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = status,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(drama.title + " show")}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Web", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold)
                        }

                        val details = extraDetails
                        if (details?.trailerKey != null) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${details.trailerKey}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                            ) {
                                Text("Trailer", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                    
                    val details2 = extraDetails
                    if (details2?.cast?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Cast & Crew", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(details2.cast) { member ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                                    if (member.profilePath != null) {
                                        AsyncImage(
                                            model = member.profilePath,
                                            contentDescription = member.name,
                                            modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.DarkGray),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.DarkGray), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(member.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(member.character, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                                .clickable {
                                    onSaveDrama(drama.copy(status = selectedStatus, episodesWatched = episodesWatched))
                                    onBack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isAlreadyInList) "Update My List" else "Save to My List", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(450.dp)) {
                AsyncImage(
                    model = drama.posterPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                ),
                                startY = 0f
                            )
                        )
                )
            }
    
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(380.dp))
    
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        
                        // Pill handle
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                                .align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
    
                        Text(
                            text = drama.title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        if (isAlreadyInList) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Already in your list", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
    
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⭐", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${drama.rating} / 10",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
    
                        Spacer(modifier = Modifier.height(24.dp))
                        

                        

                        Text(
                            text = "Synopsis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = drama.overview.ifBlank { "No synopsis available." },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
    
                        Spacer(modifier = Modifier.height(32.dp))
                        

    
                        Text(
                            text = "Set Status",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
    
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            statuses.forEach { status ->
                                val isSelected = selectedStatus == status
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .clickable { selectedStatus = status },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = status,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
    
                        Spacer(modifier = Modifier.height(40.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(drama.title + " show")}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Web", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold)
                            }
    
                            val details = extraDetails
                            if (details?.trailerKey != null) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${details.trailerKey}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                                ) {
                                    Text("Trailer", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                                }
                            }
                        }
                        
                        val details2 = extraDetails
                        if (details2?.cast?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("Cast & Crew", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(details2.cast) { member ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                                        if (member.profilePath != null) {
                                            AsyncImage(
                                                model = member.profilePath,
                                                contentDescription = member.name,
                                                modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.DarkGray),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.DarkGray), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(member.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(member.character, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                                    }
                                }
                            }
                        }
    
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(2f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                                    .clickable {
                                        onSaveDrama(drama.copy(status = selectedStatus, episodesWatched = episodesWatched))
                                        onBack()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isAlreadyInList) "Update My List" else "Save to My List", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
    
            }

        // Back Button (Moved to the end so it draws on top of the Column and intercepts touches)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
        }

        // Delete Button
        IconButton(
            onClick = { onDeleteDrama(drama) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
        }

        // Shortcut Icon (only if status == "Watching")
        if (selectedStatus == "Watching") {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 72.dp, end = 16.dp)
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), CircleShape)
                    .clip(CircleShape)
            ) {
                if (currentShortcut == null) {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Add, contentDescription = "Add Shortcut", tint = MaterialTheme.colorScheme.onBackground)
                    }
                } else {
                    val shortcut = currentShortcut!!
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                onClick = {
                                    try {
                                        if (shortcut.type == "APP") {
                                            val launchIntent = pm.getLaunchIntentForPackage(shortcut.data)
                                            if (launchIntent != null) {
                                                context.startActivity(launchIntent)
                                            }
                                        } else {
                                            val url = if (shortcut.data.startsWith("http")) shortcut.data else "https://${shortcut.data}"
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(browserIntent)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                onLongClick = {
                                    currentShortcut = null
                                    onSaveShortcut(null) // Delete shortcut
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (shortcut.type == "APP") {
                            val icon = remember(shortcut.data) {
                                try { pm.getApplicationIcon(shortcut.data) } catch (e: Exception) { null }
                            }
                            if (icon != null) {
                                Image(bitmap = icon.toImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().padding(8.dp))
                            } else {
                                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                            }
                        } else {
                            val fallbackUrl = remember(shortcut.data) {
                                val host = try { java.net.URL(shortcut.data).host } catch(e: Exception) { "" }
                                if (host.isNotEmpty()) "https://www.google.com/s2/favicons?domain=${host}&sz=128" else null
                            }
                            val urlToLoad = shortcut.iconUrl.takeIf { !it.isNullOrBlank() } ?: fallbackUrl
                            if (urlToLoad != null) {
                                AsyncImage(model = urlToLoad, contentDescription = null, modifier = Modifier.fillMaxSize().padding(8.dp))
                            } else {
                                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            }
        }
    }
}
