package com.skech.moglog.data.local

import android.content.Context
import android.content.SharedPreferences
import com.skech.moglog.data.security.SecureStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade delegating secrets to [SecureStore] (Tink AEAD) and app settings to [SettingsStore] (DataStore).
 * Contains NO hardcoded API keys.
 */
@Singleton
class ApiKeyManager @Inject constructor(
    context: Context,
    private val secureStore: SecureStore,
    private val settingsStore: SettingsStore
) {
    private val legacyPrefs: SharedPreferences =
        context.getSharedPreferences("moglog_prefs", Context.MODE_PRIVATE)

    fun getApiKey(): String = secureStore.getTmdbCredential()

    fun getCustomApiKeyOrEmpty(): String = secureStore.getUserTmdbCredentialOrEmpty()

    fun setApiKey(apiKey: String) {
        secureStore.setTmdbCredential(apiKey)
    }

    fun isFirstLaunch(): Boolean = settingsStore.settingsState.value.isFirstLaunch

    fun setFirstLaunchCompleted() {
        legacyPrefs.edit().putBoolean("is_first_launch", false).apply()
    }

    fun getShortcut(dramaId: Int): String? {
        return legacyPrefs.getString("drama_shortcut_$dramaId", null)
    }

    fun setShortcut(dramaId: Int, shortcutJson: String?) {
        if (shortcutJson == null) {
            legacyPrefs.edit().remove("drama_shortcut_$dramaId").apply()
        } else {
            legacyPrefs.edit().putString("drama_shortcut_$dramaId", shortcutJson).apply()
        }
    }

    fun getExtraDetails(dramaId: Int): String? {
        return legacyPrefs.getString("drama_extra_details_$dramaId", null)
    }

    fun setExtraDetails(dramaId: Int, detailsJson: String?) {
        if (detailsJson == null) {
            legacyPrefs.edit().remove("drama_extra_details_$dramaId").apply()
        } else {
            legacyPrefs.edit().putString("drama_extra_details_$dramaId", detailsJson).apply()
        }
    }

    fun getSyncFrequency(): String = settingsStore.settingsState.value.syncFrequency

    fun setSyncFrequency(frequency: String) {
        legacyPrefs.edit().putString("sync_frequency", frequency).apply()
    }
}
