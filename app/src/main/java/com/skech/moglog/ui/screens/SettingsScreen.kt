package com.skech.moglog.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import com.skech.moglog.ui.viewmodel.DramaViewModel
import com.skech.moglog.ui.viewmodel.AuthViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DramaViewModel,
    authViewModel: AuthViewModel? = null,
    onBack: () -> Unit
) {
    var showDeleteWarning by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }


    
    val context = LocalContext.current
    val currentUser = authViewModel?.currentUser?.collectAsState()?.value
    var syncFrequency by remember { mutableStateOf(viewModel.apiKeyManager.getSyncFrequency()) }

    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { 
            viewModel.exportDramas(context, it)
            Toast.makeText(context, "Export successful!", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importDramas(context, it)
            Toast.makeText(context, "Import successful!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            var showApiDialog by remember { mutableStateOf(false) }
            var currentKey by remember { mutableStateOf(viewModel.apiKeyManager.getCustomApiKeyOrEmpty()) }
            var apiInput by remember { mutableStateOf(currentKey) }
            var showApiError by remember { mutableStateOf(false) }
            var showInstructions by remember { mutableStateOf(false) }


            if (showApiDialog) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { showApiDialog = false; showApiError = false }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("API Key", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = apiInput,
                                onValueChange = { 
                                    apiInput = it
                                    showApiError = false
                                },
                                placeholder = { Text("e.g., 1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = showApiError
                            )
                            if (showApiError) {
                                Text("Invalid API Key. Must be 32 characters (hexadecimal).", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                if (currentKey.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            viewModel.apiKeyManager.setApiKey("")
                                            currentKey = ""
                                            showApiDialog = false
                                            showApiError = false
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Delete") }
                                }
                                TextButton(onClick = { showApiDialog = false; showApiError = false }) { Text("Cancel") }
                                Button(
                                    onClick = {
                                        val cleanKey = apiInput.trim()
                                        if (cleanKey.matches(Regex("^[a-fA-F0-9]{32}$"))) {
                                            viewModel.apiKeyManager.setApiKey(cleanKey)
                                            currentKey = cleanKey
                                            showApiDialog = false
                                        } else {
                                            showApiError = true
                                        }
                                    }
                                ) { Text("Save") }
                            }
                        }
                    }
                }
            }

            Text(
                "API Configuration", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Manage your TMDb API Key to ensure uninterrupted access to the global show catalog.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val hasKey = currentKey.isNotBlank()
                    if (!hasKey) {
                        Text("API Key not added", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { 
                            if (hasKey) {
                                val fragmentActivity = context as? FragmentActivity
                                if (fragmentActivity != null) {
                                    val biometricManager = BiometricManager.from(context)
                                    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                    if (biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
                                        val executor = ContextCompat.getMainExecutor(context)
                                        val biometricPrompt = BiometricPrompt(fragmentActivity, executor,
                                            object : BiometricPrompt.AuthenticationCallback() {
                                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                    super.onAuthenticationSucceeded(result)
                                                    apiInput = currentKey
                                                    showApiDialog = true 
                                                }
                                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                    super.onAuthenticationError(errorCode, errString)
                                                    Toast.makeText(context, "Authentication failed: $errString", Toast.LENGTH_SHORT).show()
                                                }
                                            })
                                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                            .setTitle("Authentication Required")
                                            .setSubtitle("Verify your identity to access the API Key")
                                            .setAllowedAuthenticators(authenticators)
                                            .build()
                                        biometricPrompt.authenticate(promptInfo)
                                    } else {
                                        // Fallback if no biometric/pin is enrolled or supported
                                        apiInput = currentKey
                                        showApiDialog = true
                                    }
                                } else {
                                    apiInput = currentKey
                                    showApiDialog = true 
                                }
                            } else {
                                apiInput = currentKey
                                showApiDialog = true 
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (hasKey) "Edit API Key" else "Add API Key", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    }
                    
                    OutlinedButton(
                        onClick = { showInstructions = !showInstructions },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("View Instructions", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    }

                    androidx.compose.animation.AnimatedVisibility(visible = showInstructions) {
                        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Get a free TMDb API key:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            
                            val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                                append("1. Go to ")
                                pushStringAnnotation(tag = "URL", annotation = "https://www.themoviedb.org/signup")
                                withStyle(style = androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("https://www.themoviedb.org/signup")
                                }
                                pop()
                                append(" and sign up.")
                            }
                            @Suppress("DEPRECATION")
                            androidx.compose.foundation.text.ClickableText(
                                text = annotatedString,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                onClick = { offset ->
                                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                        .firstOrNull()?.let {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it.item))
                                            context.startActivity(intent)
                                        }
                                }
                            )
                            
                            Text("2. Verify your email and log in.", style = MaterialTheme.typography.bodySmall)
                            Text("3. Go to Profile -> Settings -> API.", style = MaterialTheme.typography.bodySmall)
                            Text("4. Click \"Create API Key\" -> choose \"Developer (v3 auth)\".", style = MaterialTheme.typography.bodySmall)
                            Text("5. Fill details:\n   * App Name: Moglog\n   * URL: https://example.com\n   * Type: Mobile Application", style = MaterialTheme.typography.bodySmall)
                            Text("6. Submit and copy your API key.", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Keep your API key private. Paste it in the app to enable search.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Text(
                "Data Management", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Backup your entire list of shows to a secure JSON file on your device. You can import this file later to restore your library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Button(
                        onClick = { exportLauncher.launch("moglog_backup.json") },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Export List", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    }
                    
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Import List", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    }
                }
            }
            

            if (currentUser != null) {
                Text(
                    "Cloud Synchronization", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Choose when Moglog should sync your list to the cloud.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val options = listOf("Manual Only", "On App Launch", "On Every Change")
                        options.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        syncFrequency = option
                                        viewModel.apiKeyManager.setSyncFrequency(option)
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = (syncFrequency == option),
                                    onClick = {
                                        syncFrequency = option
                                        viewModel.apiKeyManager.setSyncFrequency(option)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                
                Text(
                    "Account Security", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Permanently delete your Moglog account. This will wipe your data from our cloud servers. This action cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Button(
                            onClick = { showDeleteWarning = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Account", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                        }
                    }
                }
            }

            Text(
                "About", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Developer: T Shivanesh Kumar", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("SKECH®", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("Moglog v1.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }

    if (showDeleteWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteWarning = false },
            title = { Text("Delete Account?", color = MaterialTheme.colorScheme.error) },
            text = { Text("Are you absolutely sure you want to delete your account? All your backed-up data on the cloud will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        authViewModel?.deleteAccount { success, errMsg ->
                            isDeleting = false
                            showDeleteWarning = false
                            if (success) {
                                Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_LONG).show()
                                onBack()
                            } else {
                                Toast.makeText(context, errMsg ?: "Failed to delete account. Please try again.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isDeleting) "Deleting..." else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

}
