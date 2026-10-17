package com.skech.moglog.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.moglogDataStore: DataStore<Preferences> by preferencesDataStore(name = "moglog_settings")

enum class ThemeMode(val key: String) {
    SYSTEM("SYSTEM"),
    DARK("DARK"),
    LIGHT("LIGHT");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.find { it.key == key } ?: SYSTEM
    }
}

enum class LibraryLayoutMode(val key: String) {
    GRID("GRID"),
    LIST("LIST");

    companion object {
        fun fromKey(key: String?): LibraryLayoutMode = entries.find { it.key == key } ?: GRID
    }
}

data class AppSettings(
    val isFirstLaunch: Boolean = true,
    val syncFrequency: String = "Manual Only",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicPosterTint: Boolean = true,
    val gridColumns: Int = 3,
    val layoutMode: LibraryLayoutMode = LibraryLayoutMode.GRID,
    val appLockEnabled: Boolean = false,
    val appLockTimeoutSeconds: Int = 60,
    val hideInRecents: Boolean = false,
    val encryptedCloudBackupEnabled: Boolean = false,
    val episodeRemindersEnabled: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val recentSearches: List<String> = emptyList(),
    val sqlCipherMigrated: Boolean = false
)

@Singleton
class SettingsStore @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val legacyPrefs = context.getSharedPreferences("moglog_prefs", Context.MODE_PRIVATE)

    val settingsFlow: Flow<AppSettings> = context.moglogDataStore.data.map { prefs ->
        val rawRecent = prefs[KEY_RECENT_SEARCHES].orEmpty()
        val recentList = if (rawRecent.isBlank()) {
            emptyList()
        } else {
            rawRecent.split(RECENT_SEARCH_DELIMITER).filter { it.isNotBlank() }
        }
        AppSettings(
            isFirstLaunch = prefs[KEY_FIRST_LAUNCH] ?: !legacyPrefs.contains("is_first_launch") || legacyPrefs.getBoolean("is_first_launch", true),
            syncFrequency = prefs[KEY_SYNC_FREQUENCY] ?: legacyPrefs.getString("sync_frequency", "Manual Only") ?: "Manual Only",
            themeMode = ThemeMode.fromKey(prefs[KEY_THEME_MODE]),
            dynamicPosterTint = prefs[KEY_DYNAMIC_TINT] ?: true,
            gridColumns = (prefs[KEY_GRID_COLUMNS] ?: 3).coerceIn(2, 4),
            layoutMode = LibraryLayoutMode.fromKey(prefs[KEY_LAYOUT_MODE]),
            appLockEnabled = prefs[KEY_APP_LOCK_ENABLED] ?: false,
            appLockTimeoutSeconds = prefs[KEY_APP_LOCK_TIMEOUT_SEC] ?: 60,
            hideInRecents = prefs[KEY_HIDE_IN_RECENTS] ?: false,
            encryptedCloudBackupEnabled = prefs[KEY_ENCRYPTED_CLOUD_BACKUP] ?: false,
            episodeRemindersEnabled = prefs[KEY_EPISODE_REMINDERS] ?: false,
            lastSyncedAt = prefs[KEY_LAST_SYNCED_AT] ?: 0L,
            recentSearches = recentList,
            sqlCipherMigrated = prefs[KEY_SQLCIPHER_MIGRATED] ?: false
        )
    }

    val settingsState: StateFlow<AppSettings> = settingsFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings(
            isFirstLaunch = legacyPrefs.getBoolean("is_first_launch", true),
            syncFrequency = legacyPrefs.getString("sync_frequency", "Manual Only") ?: "Manual Only"
        )
    )

    init {
        scope.launch {
            migrateLegacyPrefsIfNeeded()
        }
    }

    private suspend fun migrateLegacyPrefsIfNeeded() {
        context.moglogDataStore.edit { prefs ->
            if (prefs[KEY_LEGACY_MIGRATED] != true) {
                if (legacyPrefs.contains("is_first_launch")) {
                    prefs[KEY_FIRST_LAUNCH] = legacyPrefs.getBoolean("is_first_launch", true)
                }
                val legacySync = legacyPrefs.getString("sync_frequency", null)
                if (!legacySync.isNullOrBlank()) {
                    prefs[KEY_SYNC_FREQUENCY] = legacySync
                }
                prefs[KEY_LEGACY_MIGRATED] = true
            }
        }
    }

    suspend fun setFirstLaunchCompleted() {
        legacyPrefs.edit().putBoolean("is_first_launch", false).apply()
        context.moglogDataStore.edit { it[KEY_FIRST_LAUNCH] = false }
    }

    suspend fun setSyncFrequency(frequency: String) {
        legacyPrefs.edit().putString("sync_frequency", frequency).apply()
        context.moglogDataStore.edit { it[KEY_SYNC_FREQUENCY] = frequency }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.moglogDataStore.edit { it[KEY_THEME_MODE] = mode.key }
    }

    suspend fun setDynamicPosterTint(enabled: Boolean) {
        context.moglogDataStore.edit { it[KEY_DYNAMIC_TINT] = enabled }
    }

    suspend fun setGridColumns(columns: Int) {
        context.moglogDataStore.edit { it[KEY_GRID_COLUMNS] = columns.coerceIn(2, 4) }
    }

    suspend fun setLayoutMode(mode: LibraryLayoutMode) {
        context.moglogDataStore.edit { it[KEY_LAYOUT_MODE] = mode.key }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.moglogDataStore.edit { it[KEY_APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setAppLockTimeoutSeconds(seconds: Int) {
        context.moglogDataStore.edit { it[KEY_APP_LOCK_TIMEOUT_SEC] = seconds }
    }

    suspend fun setHideInRecents(enabled: Boolean) {
        context.moglogDataStore.edit { it[KEY_HIDE_IN_RECENTS] = enabled }
    }

    suspend fun setEncryptedCloudBackupEnabled(enabled: Boolean) {
        context.moglogDataStore.edit { it[KEY_ENCRYPTED_CLOUD_BACKUP] = enabled }
    }

    suspend fun setEpisodeRemindersEnabled(enabled: Boolean) {
        context.moglogDataStore.edit { it[KEY_EPISODE_REMINDERS] = enabled }
    }

    suspend fun setLastSyncedAt(timestampMs: Long) {
        context.moglogDataStore.edit { it[KEY_LAST_SYNCED_AT] = timestampMs }
    }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        context.moglogDataStore.edit { prefs ->
            val current = prefs[KEY_RECENT_SEARCHES].orEmpty()
                .split(RECENT_SEARCH_DELIMITER)
                .filter { it.isNotBlank() && !it.equals(clean, ignoreCase = true) }
            val updated = (listOf(clean) + current).take(8)
            prefs[KEY_RECENT_SEARCHES] = updated.joinToString(RECENT_SEARCH_DELIMITER)
        }
    }

    suspend fun clearRecentSearches() {
        context.moglogDataStore.edit { it.remove(KEY_RECENT_SEARCHES) }
    }

    companion object {
        private const val RECENT_SEARCH_DELIMITER = "\u001F"

        private val KEY_LEGACY_MIGRATED = booleanPreferencesKey("legacy_migrated")
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        private val KEY_SYNC_FREQUENCY = stringPreferencesKey("sync_frequency")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_TINT = booleanPreferencesKey("dynamic_poster_tint")
        private val KEY_GRID_COLUMNS = intPreferencesKey("grid_columns")
        private val KEY_LAYOUT_MODE = stringPreferencesKey("layout_mode")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_APP_LOCK_TIMEOUT_SEC = intPreferencesKey("app_lock_timeout_sec")
        private val KEY_HIDE_IN_RECENTS = booleanPreferencesKey("hide_in_recents")
        private val KEY_ENCRYPTED_CLOUD_BACKUP = booleanPreferencesKey("encrypted_cloud_backup")
        private val KEY_EPISODE_REMINDERS = booleanPreferencesKey("episode_reminders")
        private val KEY_LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
        private val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        private val KEY_SQLCIPHER_MIGRATED = booleanPreferencesKey("sqlcipher_migrated")
    }
}
