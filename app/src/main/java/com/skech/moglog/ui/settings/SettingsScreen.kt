package com.skech.moglog.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseUser
import com.skech.moglog.BuildConfig
import com.skech.moglog.data.local.AppSettings
import com.skech.moglog.data.local.LibraryLayoutMode
import com.skech.moglog.data.local.SettingsStore
import com.skech.moglog.data.local.ThemeMode
import com.skech.moglog.data.security.SecureStore
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.ui.designsystem.ActionSheetItem
import com.skech.moglog.ui.designsystem.IosActionSheet
import com.skech.moglog.ui.designsystem.IosBottomSheet
import com.skech.moglog.ui.designsystem.IosGroupedRow
import com.skech.moglog.ui.designsystem.IosGroupedSection
import com.skech.moglog.ui.designsystem.IosLargeTitleHeader
import com.skech.moglog.ui.designsystem.IosPrimaryButton
import com.skech.moglog.ui.designsystem.IosSecondaryButton
import com.skech.moglog.ui.designsystem.IosSegmentedControl
import com.skech.moglog.ui.designsystem.IosSwitchRow
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.designsystem.LocalMoglogTypography
import com.skech.moglog.ui.designsystem.MoglogIcons
import com.skech.moglog.ui.designsystem.MoglogShapes
import com.skech.moglog.ui.designsystem.iosClickable
import com.skech.moglog.ui.viewmodel.ImportSummary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dramas: List<Drama>,
    settings: AppSettings,
    currentUser: FirebaseUser?,
    isSyncing: Boolean,
    secureStore: SecureStore,
    settingsStore: SettingsStore,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSyncToCloud: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onDeleteAccount: (Boolean, (Boolean, String?) -> Unit) -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onClearMetadataCache: () -> Unit,
    onExportLibrary: (Uri, String?, (Boolean, String) -> Unit) -> Unit,
    onImportLibrary: (Uri, String?, (ImportSummary) -> Unit) -> Unit,
    onShowToast: (String) -> Unit,
    bottomBarPadding: PaddingValues = PaddingValues(bottom = 84.dp)
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val isCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40
        }
    }

    val deletedCount = remember(dramas) { dramas.count { it.isDeleted } }

    var showSyncFreqSheet by remember { mutableStateOf(false) }
    var showTimeoutSheet by remember { mutableStateOf(false) }
    var showTmdbKeySheet by remember { mutableStateOf(false) }
    var showBackupPassphraseSheet by remember { mutableStateOf(false) }
    var showSignOutSheet by remember { mutableStateOf(false) }
    var showDeleteAccountSheet by remember { mutableStateOf(false) }
    var showExportModeSheet by remember { mutableStateOf(false) }
    var pendingEncryptedExport by remember { mutableStateOf(false) }
    var exportImportPassphrase by remember { mutableStateOf("") }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportLibrary(uri, null) { _, msg -> onShowToast(msg) }
        }
    }

    val exportEncryptedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val pass = exportImportPassphrase.ifBlank { secureStore.getBackupPassphrase() }
            onExportLibrary(uri, pass) { _, msg -> onShowToast(msg) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportLibrary(uri, secureStore.getBackupPassphrase().ifBlank { null }) { summary ->
                if (summary.errorMessage?.contains("Passphrase", ignoreCase = true) == true) {
                    pendingImportUri = uri
                } else if (summary.errorMessage != null) {
                    onShowToast(summary.errorMessage)
                } else {
                    onShowToast("Imported: ${summary.added} added, ${summary.updated} updated, ${summary.skipped} skipped")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
    ) {
        IosLargeTitleHeader(
            title = "Settings",
            collapsed = isCollapsed
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = bottomBarPadding.calculateBottomPadding() + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Account & Cloud Sync
            item {
                val lastSyncSubtitle = if (settings.lastSyncedAt > 0L) {
                    val formatted = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        .format(Date(settings.lastSyncedAt))
                    "Last synced $formatted"
                } else {
                    "Not synced yet"
                }

                IosGroupedSection(
                    header = "Account & Cloud Sync",
                    footer = if (currentUser != null) lastSyncSubtitle else "Sign in with Google to back up your watchlist across devices."
                ) {
                    if (currentUser == null) {
                        IosGroupedRow(
                            title = "Sign In with Google",
                            subtitle = "Enable encrypted cloud sync & backup",
                            icon = MoglogIcons.Person,
                            iconBackground = colors.accent,
                            showChevron = true,
                            showDivider = false,
                            onClick = onSignInClick
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentUser.photoUrl != null) {
                                AsyncImage(
                                    model = currentUser.photoUrl,
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(colors.accentSubtle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = MoglogIcons.Person,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.displayName ?: "Moglog User",
                                    style = typo.headline,
                                    color = colors.label
                                )
                                Text(
                                    text = currentUser.email ?: "",
                                    style = typo.footnote,
                                    color = colors.secondaryLabel
                                )
                            }
                        }

                        IosGroupedRow(
                            title = if (isSyncing) "Syncing…" else "Back Up Now",
                            icon = MoglogIcons.Upload,
                            iconBackground = colors.accent,
                            onClick = { if (!isSyncing) onSyncToCloud() }
                        )
                        IosGroupedRow(
                            title = "Restore from Cloud",
                            icon = MoglogIcons.Download,
                            iconBackground = colors.statusPlanned,
                            onClick = { if (!isSyncing) onRestoreFromCloud() }
                        )
                        IosGroupedRow(
                            title = "Auto-Sync Frequency",
                            valueText = settings.syncFrequency,
                            icon = MoglogIcons.Refresh,
                            iconBackground = colors.statusWatching,
                            showChevron = true,
                            onClick = { showSyncFreqSheet = true }
                        )
                        IosSwitchRow(
                            title = "End-to-End Encrypted Backup",
                            subtitle = "Encrypts cloud backup with your passphrase (AES-256-GCM)",
                            icon = MoglogIcons.Lock,
                            iconBackground = colors.accent,
                            checked = settings.encryptedCloudBackupEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !secureStore.hasBackupPassphrase()) {
                                    showBackupPassphraseSheet = true
                                } else {
                                    scope.launch { settingsStore.setEncryptedCloudBackupEnabled(enabled) }
                                }
                            }
                        )
                        if (settings.encryptedCloudBackupEnabled || secureStore.hasBackupPassphrase()) {
                            IosGroupedRow(
                                title = "Backup Passphrase",
                                valueText = if (secureStore.hasBackupPassphrase()) "Configured" else "Set",
                                icon = MoglogIcons.Key,
                                iconBackground = colors.statusPlanned,
                                showChevron = true,
                                onClick = { showBackupPassphraseSheet = true }
                            )
                        }
                        IosGroupedRow(
                            title = "Sign Out",
                            titleColor = colors.statusWatching,
                            onClick = { showSignOutSheet = true }
                        )
                        IosGroupedRow(
                            title = "Delete Cloud Account & Data",
                            titleColor = colors.destructive,
                            showDivider = false,
                            onClick = { showDeleteAccountSheet = true }
                        )
                    }
                }
            }

            // 2. Appearance
            item {
                IosGroupedSection(header = "Appearance") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Theme",
                            style = typo.subhead,
                            color = colors.secondaryLabel
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val themeOptions = listOf(ThemeMode.SYSTEM, ThemeMode.DARK, ThemeMode.LIGHT)
                        IosSegmentedControl(
                            items = themeOptions,
                            selectedItem = settings.themeMode,
                            onItemSelected = { mode ->
                                scope.launch { settingsStore.setThemeMode(mode) }
                            },
                            labelProvider = {
                                when (it) {
                                    ThemeMode.SYSTEM -> "System"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.LIGHT -> "Light"
                                }
                            }
                        )
                    }

                    IosSwitchRow(
                        title = "Dynamic Poster Accent Tint",
                        subtitle = "Subtle ambient glow on Detail pages",
                        icon = MoglogIcons.SunMoon,
                        iconBackground = colors.accent,
                        checked = settings.dynamicPosterTint,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsStore.setDynamicPosterTint(enabled) }
                        }
                    )

                    IosGroupedRow(
                        title = "Default Library View",
                        valueText = if (settings.layoutMode == LibraryLayoutMode.GRID) "Poster Grid" else "Compact List",
                        icon = if (settings.layoutMode == LibraryLayoutMode.GRID) MoglogIcons.Grid else MoglogIcons.ListRows,
                        iconBackground = colors.statusPlanned,
                        onClick = {
                            val next = if (settings.layoutMode == LibraryLayoutMode.GRID) {
                                LibraryLayoutMode.LIST
                            } else {
                                LibraryLayoutMode.GRID
                            }
                            scope.launch { settingsStore.setLayoutMode(next) }
                        }
                    )

                    IosGroupedRow(
                        title = "Poster Grid Density",
                        valueText = "${settings.gridColumns} Columns",
                        icon = MoglogIcons.Grid,
                        iconBackground = colors.statusWatching,
                        showDivider = false,
                        onClick = {
                            val next = when (settings.gridColumns) {
                                2 -> 3
                                3 -> 4
                                else -> 2
                            }
                            scope.launch { settingsStore.setGridColumns(next) }
                        }
                    )
                }
            }

            // 3. Security & App Lock
            item {
                IosGroupedSection(
                    header = "Security & Privacy",
                    footer = "Local database is encrypted at rest using 256-bit SQLCipher + Android Keystore."
                ) {
                    IosSwitchRow(
                        title = "Biometric / Device PIN Lock",
                        subtitle = "Require authentication when opening Moglog",
                        icon = MoglogIcons.Shield,
                        iconBackground = colors.accent,
                        checked = settings.appLockEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsStore.setAppLockEnabled(enabled) }
                        }
                    )
                    if (settings.appLockEnabled) {
                        val timeoutLabel = when (settings.appLockTimeoutSeconds) {
                            0 -> "Immediately"
                            60 -> "After 1 minute"
                            300 -> "After 5 minutes"
                            else -> "After ${settings.appLockTimeoutSeconds}s"
                        }
                        IosGroupedRow(
                            title = "Lock Timeout",
                            valueText = timeoutLabel,
                            icon = MoglogIcons.Lock,
                            iconBackground = colors.statusPlanned,
                            showChevron = true,
                            onClick = { showTimeoutSheet = true }
                        )
                    }
                    IosSwitchRow(
                        title = "Hide in App Switcher",
                        subtitle = "Prevent screenshots and blur recents preview (FLAG_SECURE)",
                        icon = MoglogIcons.Lock,
                        iconBackground = colors.statusWatching,
                        checked = settings.hideInRecents,
                        showDivider = false,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsStore.setHideInRecents(enabled) }
                        }
                    )
                }
            }

            // 4. Data & Storage
            item {
                IosGroupedSection(header = "Data & Storage") {
                    IosSwitchRow(
                        title = "Episode Air-Date Reminders",
                        subtitle = "Notify when a Watching show airs a new episode",
                        icon = MoglogIcons.Bell,
                        iconBackground = colors.statusPlanned,
                        checked = settings.episodeRemindersEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsStore.setEpisodeRemindersEnabled(enabled) }
                        }
                    )
                    IosGroupedRow(
                        title = "Export Library Backup",
                        subtitle = "Save as encrypted .moglog or JSON",
                        icon = MoglogIcons.Download,
                        iconBackground = colors.accent,
                        showChevron = true,
                        onClick = { showExportModeSheet = true }
                    )
                    IosGroupedRow(
                        title = "Import Library Backup",
                        subtitle = "Merge shows from .moglog or JSON file",
                        icon = MoglogIcons.Upload,
                        iconBackground = colors.statusPlanned,
                        showChevron = true,
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                        }
                    )
                    IosGroupedRow(
                        title = "Recycle Bin",
                        valueText = "$deletedCount",
                        icon = MoglogIcons.Trash,
                        iconBackground = colors.destructive,
                        showChevron = true,
                        onClick = onNavigateToRecycleBin
                    )
                    IosGroupedRow(
                        title = "Clear Metadata Cache",
                        subtitle = "Refresh cached TMDB details and image memory",
                        icon = MoglogIcons.Refresh,
                        iconBackground = colors.statusWatching,
                        showDivider = false,
                        onClick = {
                            onClearMetadataCache()
                            onShowToast("Metadata cache cleared")
                        }
                    )
                }
            }

            // 5. TMDB API Integration
            item {
                val hasKey = secureStore.getTmdbCredential().isNotBlank()
                IosGroupedSection(
                    header = "TMDB Integration",
                    footer = "Stored strictly on-device in Android Keystore via Google Tink."
                ) {
                    IosGroupedRow(
                        title = "TMDB API Key / Read Token",
                        valueText = if (hasKey) "Configured" else "Not Set",
                        icon = MoglogIcons.Key,
                        iconBackground = if (hasKey) colors.accent else colors.statusWatching,
                        showChevron = true,
                        showDivider = false,
                        onClick = { showTmdbKeySheet = true }
                    )
                }
            }

            // 6. About & Legal
            item {
                IosGroupedSection(
                    header = "About",
                    footer = "This product uses the TMDB API but is not endorsed or certified by TMDB."
                ) {
                    IosGroupedRow(
                        title = "Privacy & Security Architecture",
                        icon = MoglogIcons.Shield,
                        iconBackground = colors.accent,
                        showChevron = true,
                        onClick = onNavigateToPrivacy
                    )
                    IosGroupedRow(
                        title = "TMDB Terms of Use",
                        icon = MoglogIcons.ExternalLink,
                        iconBackground = colors.statusPlanned,
                        showChevron = true,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/terms-of-use"))
                                )
                            }
                        }
                    )
                    IosGroupedRow(
                        title = "Version",
                        valueText = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        showDivider = false
                    )
                }
            }
        }
    }

    if (showSyncFreqSheet) {
        val options = listOf("Manual Only", "On App Launch", "On Every Change")
        IosActionSheet(
            title = "Auto-Sync Frequency",
            message = "Choose when Moglog automatically backs up your library to the cloud.",
            actions = options.map { freq ->
                ActionSheetItem(
                    label = if (settings.syncFrequency == freq) "$freq ✓" else freq,
                    onClick = {
                        scope.launch { settingsStore.setSyncFrequency(freq) }
                    }
                )
            },
            onDismissRequest = { showSyncFreqSheet = false }
        )
    }

    if (showTimeoutSheet) {
        val timeouts = listOf(
            0 to "Immediately",
            60 to "After 1 minute",
            300 to "After 5 minutes"
        )
        IosActionSheet(
            title = "App Lock Timeout",
            actions = timeouts.map { (sec, label) ->
                ActionSheetItem(
                    label = if (settings.appLockTimeoutSeconds == sec) "$label ✓" else label,
                    onClick = {
                        scope.launch { settingsStore.setAppLockTimeoutSeconds(sec) }
                    }
                )
            },
            onDismissRequest = { showTimeoutSheet = false }
        )
    }

    if (showExportModeSheet) {
        IosActionSheet(
            title = "Export Library Backup",
            message = "Encrypted .moglog archives protect your notes and watch history with AES-256-GCM.",
            actions = listOf(
                ActionSheetItem(
                    label = "Export Encrypted Backup (.moglog)",
                    icon = MoglogIcons.Lock,
                    onClick = {
                        pendingEncryptedExport = true
                    }
                ),
                ActionSheetItem(
                    label = "Export Plain JSON (.json)",
                    icon = MoglogIcons.Download,
                    onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                        exportJsonLauncher.launch("moglog_backup_$stamp.json")
                    }
                )
            ),
            onDismissRequest = { showExportModeSheet = false }
        )
    }

    if (pendingEncryptedExport) {
        PassphraseSheet(
            title = "Encrypt Backup File",
            subtitle = "Enter a passphrase to encrypt this .moglog export with PBKDF2-HMAC-SHA256 + AES-256-GCM.",
            initialPassphrase = secureStore.getBackupPassphrase(),
            confirmLabel = "Choose Save Location",
            onDismiss = { pendingEncryptedExport = false },
            onConfirm = { pass ->
                exportImportPassphrase = pass
                pendingEncryptedExport = false
                val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                exportEncryptedLauncher.launch("moglog_backup_$stamp.moglog")
            }
        )
    }

    if (pendingImportUri != null) {
        val uriToRetry = pendingImportUri!!
        PassphraseSheet(
            title = "Decrypt Backup File",
            subtitle = "This .moglog archive is encrypted. Enter its passphrase to unlock and merge.",
            initialPassphrase = "",
            confirmLabel = "Decrypt & Import",
            onDismiss = { pendingImportUri = null },
            onConfirm = { pass ->
                pendingImportUri = null
                onImportLibrary(uriToRetry, pass) { summary ->
                    if (summary.errorMessage != null) {
                        onShowToast(summary.errorMessage)
                    } else {
                        onShowToast("Imported: ${summary.added} added, ${summary.updated} updated, ${summary.skipped} skipped")
                    }
                }
            }
        )
    }

    if (showBackupPassphraseSheet) {
        PassphraseSheet(
            title = "E2E Backup Passphrase",
            subtitle = "Used to encrypt and decrypt your cloud backup client-side. Keep this safe — we cannot recover a lost passphrase.",
            initialPassphrase = secureStore.getBackupPassphrase(),
            confirmLabel = "Save Passphrase",
            onDismiss = { showBackupPassphraseSheet = false },
            onConfirm = { pass ->
                secureStore.setBackupPassphrase(pass)
                scope.launch {
                    settingsStore.setEncryptedCloudBackupEnabled(pass.isNotBlank())
                }
                showBackupPassphraseSheet = false
                onShowToast("E2E backup passphrase saved")
            }
        )
    }

    if (showTmdbKeySheet) {
        TmdbCredentialSheet(
            initialKey = secureStore.getTmdbCredential(),
            isValidKey = { secureStore.isValidTmdbCredential(it) },
            onDismiss = { showTmdbKeySheet = false },
            onSave = { newKey ->
                secureStore.setTmdbCredential(newKey)
                showTmdbKeySheet = false
                onShowToast(if (newKey.isBlank()) "TMDB credential cleared" else "TMDB credential saved")
            }
        )
    }

    if (showSignOutSheet) {
        IosActionSheet(
            title = "Sign Out of Cloud Sync?",
            message = "Your local library and TMDB API key will remain on this device.",
            actions = listOf(
                ActionSheetItem(
                    label = "Sign Out",
                    isDestructive = true,
                    onClick = onSignOutClick
                )
            ),
            onDismissRequest = { showSignOutSheet = false }
        )
    }

    if (showDeleteAccountSheet) {
        IosActionSheet(
            title = "Delete Cloud Account & Data?",
            message = "This permanently deletes your Firestore cloud backup and Firebase Auth account.",
            actions = listOf(
                ActionSheetItem(
                    label = "Delete Cloud Data Only (Keep Local Library)",
                    isDestructive = true,
                    onClick = {
                        onDeleteAccount(false) { _, err ->
                            onShowToast(err ?: "Cloud account deleted")
                        }
                    }
                ),
                ActionSheetItem(
                    label = "Delete Everything (Cloud + Local Library)",
                    isDestructive = true,
                    onClick = {
                        onDeleteAccount(true) { _, err ->
                            onShowToast(err ?: "Account and local library deleted")
                        }
                    }
                )
            ),
            onDismissRequest = { showDeleteAccountSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassphraseSheet(
    title: String,
    subtitle: String,
    initialPassphrase: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    var passphrase by remember { mutableStateOf(initialPassphrase) }
    var visible by remember { mutableStateOf(false) }

    IosBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = title, style = typo.title3, color = colors.label)
            Text(text = subtitle, style = typo.footnote, color = colors.secondaryLabel)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MoglogShapes.Control)
                    .background(colors.tertiaryBackground)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    singleLine = true,
                    textStyle = typo.body.copy(color = colors.label),
                    cursorBrush = SolidColor(colors.accent),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (passphrase.isEmpty()) {
                            Text(
                                text = "Minimum 6 characters",
                                style = typo.body,
                                color = colors.tertiaryLabel
                            )
                        }
                        inner()
                    }
                )
                Text(
                    text = if (visible) "Hide" else "Show",
                    style = typo.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent,
                    modifier = Modifier
                        .iosClickable { visible = !visible }
                        .padding(start = 8.dp)
                )
            }

            IosPrimaryButton(
                text = confirmLabel,
                enabled = passphrase.trim().length >= 6,
                onClick = { onConfirm(passphrase.trim()) }
            )
            IosSecondaryButton(
                text = "Cancel",
                onClick = onDismiss
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TmdbCredentialSheet(
    initialKey: String,
    isValidKey: (String) -> Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val colors = LocalMoglogColors.current
    val typo = LocalMoglogTypography.current
    var keyInput by remember { mutableStateOf(initialKey) }
    var visible by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    IosBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "TMDB API Credential",
                style = typo.title3,
                color = colors.label
            )
            Text(
                text = "Accepts either a 32-character v3 API Key or a v4 Read Access Token (JWT). Encrypted at rest with AES-256-GCM.",
                style = typo.footnote,
                color = colors.secondaryLabel
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MoglogShapes.Control)
                    .background(colors.tertiaryBackground)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it
                        errorText = null
                    },
                    singleLine = true,
                    textStyle = typo.body.copy(color = colors.label),
                    cursorBrush = SolidColor(colors.accent),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (keyInput.isEmpty()) {
                            Text(
                                text = "Paste TMDB API key or Bearer token",
                                style = typo.body,
                                color = colors.tertiaryLabel
                            )
                        }
                        inner()
                    }
                )
                Text(
                    text = if (visible) "Hide" else "Show",
                    style = typo.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent,
                    modifier = Modifier
                        .iosClickable { visible = !visible }
                        .padding(start = 8.dp)
                )
            }

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    style = typo.footnote.copy(fontWeight = FontWeight.Medium),
                    color = colors.destructive
                )
            }

            IosPrimaryButton(
                text = "Save Credential",
                enabled = keyInput.trim().isNotEmpty(),
                onClick = {
                    val trimmed = keyInput.trim()
                    if (!isValidKey(trimmed)) {
                        errorText = "Enter a valid 32-char hex v3 API key or v4 JWT Read Access Token."
                    } else {
                        onSave(trimmed)
                    }
                }
            )

            if (initialKey.isNotBlank()) {
                IosSecondaryButton(
                    text = "Remove Saved Key",
                    onClick = { onSave("") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
