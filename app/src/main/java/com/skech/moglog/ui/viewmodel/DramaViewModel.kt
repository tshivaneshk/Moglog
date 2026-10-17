package com.skech.moglog.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skech.moglog.data.local.ApiKeyManager
import com.skech.moglog.data.local.AppSettings
import com.skech.moglog.data.local.LibraryLayoutMode
import com.skech.moglog.data.local.SettingsStore
import com.skech.moglog.data.local.ThemeMode
import com.skech.moglog.data.repository.CloudSyncRepository
import com.skech.moglog.data.security.SecureStore
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.domain.model.DramaExtraDetails
import com.skech.moglog.domain.model.Shortcut
import com.skech.moglog.domain.repository.DramaRepository
import com.skech.moglog.domain.repository.ShortcutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverRailsState(
    val isLoading: Boolean = false,
    val trending: List<Drama> = emptyList(),
    val popular: List<Drama> = emptyList(),
    val airingToday: List<Drama> = emptyList(),
    val onTheAir: List<Drama> = emptyList(),
    val error: String? = null
)

data class ImportSummary(
    val added: Int,
    val updated: Int,
    val skipped: Int,
    val errorMessage: String? = null
)

@HiltViewModel
class DramaViewModel @Inject constructor(
    private val repository: DramaRepository,
    private val shortcutRepository: ShortcutRepository,
    val apiKeyManager: ApiKeyManager,
    val secureStore: SecureStore,
    val settingsStore: SettingsStore,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    val myDramas: StateFlow<List<Drama>> = repository.getAllDramas()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val appSettings: StateFlow<AppSettings> = settingsStore.settingsState

    val hasApiKey: StateFlow<Boolean> = secureStore.hasApiKeyFlow

    // Transient cache of discovered/searched dramas so navigating by ID works for unsaved shows too
    private val transientDramas = mutableMapOf<Int, Drama>()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Drama>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _discoverRails = MutableStateFlow(DiscoverRailsState())
    val discoverRails: StateFlow<DiscoverRailsState> = _discoverRails.asStateFlow()

    private var searchJob: Job? = null

    init {
        if (secureStore.getTmdbCredential().isNotBlank()) {
            loadDiscoverRails(forceRefresh = false)
        }
    }

    fun loadDiscoverRails(forceRefresh: Boolean = false) {
        if (secureStore.getTmdbCredential().isBlank()) return
        val current = _discoverRails.value
        if (!forceRefresh && current.trending.isNotEmpty()) return

        viewModelScope.launch {
            _discoverRails.value = current.copy(isLoading = true, error = null)
            try {
                val trending = repository.getTrendingDramas()
                val popular = repository.getPopularDramas()
                val airingToday = repository.getAiringTodayDramas()
                val onTheAir = repository.getOnTheAirDramas()

                (trending + popular + airingToday + onTheAir).forEach {
                    transientDramas[it.id] = it
                }

                _discoverRails.value = DiscoverRailsState(
                    isLoading = false,
                    trending = trending,
                    popular = popular,
                    airingToday = airingToday,
                    onTheAir = onTheAir
                )
            } catch (e: Exception) {
                _discoverRails.value = current.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Unable to load Discover feed"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isLoading.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(450) // debounce
            _isLoading.value = true
            val results = repository.searchDramas(query)
            results.forEach { transientDramas[it.id] = it }
            _searchResults.value = results
            _isLoading.value = false
            if (results.isNotEmpty()) {
                settingsStore.addRecentSearch(query)
            }
        }
    }

    suspend fun resolveDramaById(id: Int): Drama? {
        val local = myDramas.value.find { it.id == id } ?: repository.getLocalDramaById(id)
        if (local != null) return local
        val cachedTransient = transientDramas[id]
        val remote = repository.getDramaDetails(id)
        if (remote != null) {
            transientDramas[id] = remote
            return remote
        }
        return cachedTransient
    }

    fun saveDrama(drama: Drama) {
        viewModelScope.launch {
            val existing = myDramas.value.find { it.id == drama.id }
            val now = System.currentTimeMillis()
            val newDrama = drama.copy(
                addedAt = if (existing != null && existing.addedAt > 0) existing.addedAt else now,
                updatedAt = now,
                completedAt = when {
                    drama.status == "Done" && existing?.status != "Done" -> now
                    existing != null -> existing.completedAt
                    else -> 0L
                }
            )
            repository.saveDrama(newDrama)
            if (settingsStore.settingsState.value.syncFrequency == "On Every Change") {
                val currentList = myDramas.value.toMutableList()
                val idx = currentList.indexOfFirst { it.id == newDrama.id }
                if (idx != -1) currentList[idx] = newDrama else currentList.add(newDrama)
                cloudSyncRepository.syncToCloud(currentList)
                settingsStore.setLastSyncedAt(now)
            }
        }
    }

    suspend fun getDramaExtraDetails(id: Int): DramaExtraDetails? {
        val details = repository.getDramaExtraDetails(id)
        if (details != null) {
            // Backfill local show metadata (genres, releaseYear, totalSeasons, totalEpisodes, nextEpisodeAirDate) if missing
            val existing = myDramas.value.find { it.id == id }
            if (existing != null) {
                val updated = existing.copy(
                    genres = existing.genres.ifBlank { details.genres.joinToString(", ") },
                    releaseYear = if (existing.releaseYear > 0) existing.releaseYear else details.releaseYear,
                    totalSeasons = if (existing.totalSeasons > 0) existing.totalSeasons else details.totalSeasons,
                    totalEpisodes = if (existing.totalEpisodes > 0) existing.totalEpisodes else details.totalEpisodes,
                    nextEpisodeAirDate = details.nextEpisodeAirDate ?: existing.nextEpisodeAirDate,
                    nextEpisodeNumber = if (details.nextEpisodeNumber > 0) details.nextEpisodeNumber else existing.nextEpisodeNumber,
                    nextSeasonNumber = if (details.nextSeasonNumber > 0) details.nextSeasonNumber else existing.nextSeasonNumber
                )
                if (updated != existing) {
                    repository.saveDrama(updated)
                }
            }
        }
        return details
    }

    suspend fun getShortcutForDrama(dramaId: Int): Shortcut? {
        return shortcutRepository.getShortcutForDrama(dramaId)
    }

    fun saveShortcutForDrama(dramaId: Int, shortcut: Shortcut?) {
        viewModelScope.launch {
            shortcutRepository.saveShortcutForDrama(dramaId, shortcut)
        }
    }

    fun updateDramaStatus(id: Int, status: String) {
        viewModelScope.launch {
            val existing = myDramas.value.find { it.id == id }
            if (existing != null) {
                saveDrama(existing.copy(status = status))
            }
        }
    }

    fun deleteDrama(drama: Drama) {
        viewModelScope.launch {
            repository.deleteDrama(drama)
        }
    }

    fun moveToRecycleBin(drama: Drama) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.saveDrama(drama.copy(isDeleted = true, deletedAt = now, updatedAt = now))
        }
    }

    fun restoreDrama(drama: Drama) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.saveDrama(drama.copy(isDeleted = false, deletedAt = 0L, updatedAt = now))
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            val deleted = myDramas.value.filter { it.isDeleted }
            deleted.forEach { repository.deleteDrama(it) }
        }
    }

    fun setLayoutMode(mode: LibraryLayoutMode) {
        viewModelScope.launch { settingsStore.setLayoutMode(mode) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setDynamicPosterTint(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicPosterTint(enabled) }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { settingsStore.setGridColumns(columns) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { settingsStore.clearRecentSearches() }
    }

    fun clearMetadataCache(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearMetadataCache()
            onDone()
        }
    }

    fun exportDramas(
        context: Context,
        uri: Uri,
        passphrase: String? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val currentDramas = myDramas.value
                val json = Gson().toJson(currentDramas)
                val out = context.contentResolver.openOutputStream(uri)
                if (out == null) {
                    onResult(false, "Unable to open destination file.")
                    return@launch
                }
                out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                onResult(true, "Exported ${currentDramas.size} shows.")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Export failed.")
            }
        }
    }

    fun importDramas(
        context: Context,
        uri: Uri,
        passphrase: String? = null,
        onResult: (ImportSummary) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                    val maxBytes = 10 * 1024 * 1024 // 10 MB limit
                    val buffer = stream.readBytes()
                    if (buffer.size > maxBytes) {
                        onResult(ImportSummary(0, 0, 0, "Backup file exceeds 10 MB limit."))
                        return@launch
                    }
                    buffer
                } ?: run {
                    onResult(ImportSummary(0, 0, 0, "Unable to read selected file."))
                    return@launch
                }

                val json = String(bytes, Charsets.UTF_8)
                if (json.isBlank()) {
                    onResult(ImportSummary(0, 0, 0, "Selected file is empty."))
                    return@launch
                }

                val type = object : TypeToken<List<Drama>>() {}.type
                val importedDramas: List<Drama> = Gson().fromJson(json, type) ?: emptyList()

                val localMap = myDramas.value.associateBy { it.id }
                var added = 0
                var updated = 0
                var skipped = 0

                importedDramas.distinctBy { it.id }.forEach { incoming ->
                    if (incoming.id <= 0 || incoming.title.isBlank()) {
                        skipped++
                        return@forEach
                    }
                    val sanitized = incoming.copy(
                        rating = incoming.rating.coerceIn(0.0, 10.0),
                        myRating = incoming.myRating.coerceIn(0.0, 10.0),
                        episodesWatched = incoming.episodesWatched.coerceAtLeast(0),
                        totalEpisodes = incoming.totalEpisodes.coerceAtLeast(0),
                        status = if (incoming.status in listOf("Watching", "Planned", "Done")) incoming.status else "Planned"
                    )
                    val existing = localMap[sanitized.id]
                    if (existing == null) {
                        repository.saveDrama(sanitized)
                        added++
                    } else if (sanitized.updatedAt > existing.updatedAt) {
                        repository.saveDrama(sanitized)
                        updated++
                    } else {
                        skipped++
                    }
                }
                onResult(ImportSummary(added, updated, skipped))
            } catch (e: Exception) {
                onResult(ImportSummary(0, 0, 0, "Invalid backup format: ${e.localizedMessage ?: "Parse error"}"))
            }
        }
    }
}
