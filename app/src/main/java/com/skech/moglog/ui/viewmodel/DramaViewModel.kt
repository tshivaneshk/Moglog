package com.skech.moglog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.domain.repository.DramaRepository
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

@HiltViewModel
class DramaViewModel @Inject constructor(
    private val repository: DramaRepository,
    val apiKeyManager: com.skech.moglog.data.local.ApiKeyManager,
    private val cloudSyncRepository: com.skech.moglog.data.repository.CloudSyncRepository
) : ViewModel() {

    val myDramas: StateFlow<List<Drama>> = repository.getAllDramas()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())



    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Drama>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(500) // debounce
            _isLoading.value = true
            val results = repository.searchDramas(query)
            _searchResults.value = results
            _isLoading.value = false
        }
    }

    fun saveDrama(drama: Drama) {
        viewModelScope.launch {
            val existing = myDramas.value.find { it.id == drama.id }
            val now = System.currentTimeMillis()
            val newDrama = drama.copy(
                addedAt = if (existing != null && existing.addedAt > 0) existing.addedAt else now,
                updatedAt = now,
                completedAt = if (drama.status == "Done" && existing?.status != "Done") now else if (existing != null) existing.completedAt else 0L
            )
repository.saveDrama(newDrama)
            if (apiKeyManager.getSyncFrequency() == "On Every Change") {
                val currentList = myDramas.value.toMutableList()
                val idx = currentList.indexOfFirst { it.id == newDrama.id }
                if (idx != -1) currentList[idx] = newDrama else currentList.add(newDrama)
                cloudSyncRepository.syncToCloud(currentList)
            }
        }
    }

    suspend fun getDramaExtraDetails(id: Int): com.skech.moglog.domain.model.DramaExtraDetails? {
        return repository.getDramaExtraDetails(id)
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
            repository.saveDrama(drama.copy(isDeleted = true))
        }
    }

    fun restoreDrama(drama: Drama) {
        viewModelScope.launch {
            repository.saveDrama(drama.copy(isDeleted = false))
        }
    }

    fun exportDramas(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val currentDramas = myDramas.value
                val json = com.google.gson.Gson().toJson(currentDramas)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importDramas(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    val type = object : com.google.gson.reflect.TypeToken<List<Drama>>() {}.type
                    val importedDramas: List<Drama> = com.google.gson.Gson().fromJson(json, type)
                    importedDramas.forEach { drama ->
                        repository.saveDrama(drama)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
