package com.skech.moglog.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skech.moglog.domain.model.Shortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable

fun Drawable.toImageBitmap(): androidx.compose.ui.graphics.ImageBitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return bitmap.asImageBitmap()
    }
    val width = if (intrinsicWidth > 0) intrinsicWidth else 1
    val height = if (intrinsicHeight > 0) intrinsicHeight else 1
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

data class InstalledApp(val name: String, val packageName: String, val icon: Drawable?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    shortcuts: List<Shortcut>,
    onBack: () -> Unit,
    onSaveShortcut: (Shortcut) -> Unit,
    onDeleteShortcut: (Shortcut) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var showAddDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showAppDialog by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }

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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add to Watch", fontWeight = FontWeight.Bold) },
            text = { Text("What type of shortcut do you want to add?") },
            confirmButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    showAppDialog = true
                }) {
                    Text("App", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    showLinkDialog = true
                }) {
                    Text("Link", fontWeight = FontWeight.Bold)
                }
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

                        onSaveShortcut(Shortcut(title = linkTitle, data = url, type = "LINK", iconUrl = autoIconUrl))
                        showLinkDialog = false
                    }
                }) {
                    Text("Save")
                }
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
                                        onSaveShortcut(Shortcut(title = app.name, data = app.packageName, type = "APP"))
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watch Shortcuts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Shortcut")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (shortcuts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Shortcuts Added",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(shortcuts, key = { it.id }) { shortcut ->
                        ShortcutItem(
                            shortcut = shortcut,
                            pm = pm,
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
                            onDelete = { onDeleteShortcut(shortcut) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ShortcutItem(
    shortcut: Shortcut,
    pm: PackageManager,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Shortcut?") },
            text = { Text("Are you sure you want to delete '${shortcut.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteDialog = true }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (shortcut.type == "APP") {
                val icon = remember(shortcut.data) {
                    try {
                        pm.getApplicationIcon(shortcut.data)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (icon != null) {
                    Image(
                        bitmap = icon.toImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                } else {
                    Icon(androidx.compose.material.icons.Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                val fallbackUrl = remember(shortcut.data) {
                    val host = try { java.net.URL(shortcut.data).host } catch(e: Exception) { "" }
                    if (host.isNotEmpty()) "https://www.google.com/s2/favicons?domain=${host}&sz=128" else null
                }
                val urlToLoad = shortcut.iconUrl.takeIf { !it.isNullOrBlank() } ?: fallbackUrl

                if (urlToLoad != null) {
                    coil.compose.AsyncImage(
                        model = urlToLoad,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Icon(androidx.compose.material.icons.Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = shortcut.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
