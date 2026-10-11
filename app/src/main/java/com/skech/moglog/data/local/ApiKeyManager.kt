package com.skech.moglog.data.local

import android.content.Context
import android.content.SharedPreferences

class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("moglog_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "tmdb_api_key"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_NOTE = "moglog_personal_note"
        const val DEFAULT_API_KEY = "b30d871ab7f51c18054f0e6d0921da16"
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun getCustomApiKeyOrEmpty(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun getNote(): String {
        return prefs.getString(KEY_NOTE, "") ?: ""
    }

    fun setNote(note: String) {
        prefs.edit().putString(KEY_NOTE, note).apply()
    }

    fun getShortcut(dramaId: Int): String? {
        return prefs.getString("drama_shortcut_$dramaId", null)
    }

    fun setShortcut(dramaId: Int, shortcutJson: String?) {
        if (shortcutJson == null) {
            prefs.edit().remove("drama_shortcut_$dramaId").apply()
        } else {
            prefs.edit().putString("drama_shortcut_$dramaId", shortcutJson).apply()
        }
    }

    fun getExtraDetails(dramaId: Int): String? {
        return prefs.getString("drama_extra_details_$dramaId", null)
    }

    fun setExtraDetails(dramaId: Int, detailsJson: String?) {
        if (detailsJson == null) {
            prefs.edit().remove("drama_extra_details_$dramaId").apply()
        } else {
            prefs.edit().putString("drama_extra_details_$dramaId", detailsJson).apply()
        }
    }

    fun getSyncFrequency(): String {
        return prefs.getString("sync_frequency", "Manual") ?: "Manual"
    }

    fun setSyncFrequency(frequency: String) {
        prefs.edit().putString("sync_frequency", frequency).apply()
    }
}
