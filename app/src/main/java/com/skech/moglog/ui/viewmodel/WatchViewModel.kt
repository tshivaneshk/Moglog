package com.skech.moglog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skech.moglog.domain.model.Shortcut
import com.skech.moglog.domain.repository.ShortcutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchViewModel @Inject constructor(
    private val repository: ShortcutRepository
) : ViewModel() {

    val shortcuts: StateFlow<List<Shortcut>> = repository.getAllShortcuts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveShortcut(shortcut: Shortcut) {
        viewModelScope.launch {
            repository.saveShortcut(shortcut)
        }
    }

    fun deleteShortcut(shortcut: Shortcut) {
        viewModelScope.launch {
            repository.deleteShortcut(shortcut)
        }
    }
}
