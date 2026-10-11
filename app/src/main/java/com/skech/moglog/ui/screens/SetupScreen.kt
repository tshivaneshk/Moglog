package com.skech.moglog.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onFinishSetup: () -> Unit,
    onSaveKey: (String) -> Unit
) {
    var showKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showKeyDialog) {
        Dialog(onDismissRequest = { showKeyDialog = false; showError = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Enter API Key", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { 
                            apiKeyInput = it
                            showError = false
                        },
                        placeholder = { Text("e.g., 1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showError
                    )
                    if (showError) {
                        Text("Invalid API Key. Must be 32 characters (hexadecimal).", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showKeyDialog = false; showError = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                val cleanKey = apiKeyInput.trim()
                                if (cleanKey.matches(Regex("^[a-fA-F0-9]{32}$"))) {
                                    onSaveKey(cleanKey)
                                    showKeyDialog = false
                                    onFinishSetup()
                                } else {
                                    showError = true
                                }
                            }
                        ) { Text("Save") }
                    }
                }
            }
        }
    }

    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = { Text("Warning", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("API Key must be added to use search and discovery features. Are you sure you want to proceed without one?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWarningDialog = false
                        onFinishSetup()
                    }
                ) { Text("Proceed Anyway") }
            },
            dismissButton = {
                Button(onClick = { showWarningDialog = false }) { Text("Go Back") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome to Moglog",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "To get the best experience and track thousands of shows, Moglog uses the TMDB API. You can add your own free API key to avoid rate limits.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = { showKeyDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Already Have TMDB API Key", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/signup"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Create New TMDB Account", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = { showWarningDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Add Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Note: Go to settings to view detailed instructions to create and get a TMDB API key.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
