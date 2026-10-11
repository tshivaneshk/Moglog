package com.skech.moglog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.skech.moglog.data.auth.GoogleAuthClient
import com.skech.moglog.data.repository.CloudSyncRepository
import com.skech.moglog.domain.model.Drama
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    val authClient: GoogleAuthClient,
    private val cloudSyncRepository: CloudSyncRepository,
    private val dramaRepository: com.skech.moglog.domain.repository.DramaRepository,
    private val apiKeyManager: com.skech.moglog.data.local.ApiKeyManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authClient.getSignedInUser())
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    fun signIn(result: com.skech.moglog.data.auth.SignInResult) {
        _currentUser.value = result.data
        if (result.data != null) {
            viewModelScope.launch {
                fetchFromCloud()
            }
        }
    }

    fun signOut() {
        authClient.signOut()
        _currentUser.value = null
        apiKeyManager.setApiKey("")
    }

    suspend fun checkAccountExists(uid: String): Boolean {
        return try {
            val doc = cloudSyncRepository.firestore.collection("users").document(uid).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun deleteAccount(onComplete: (Boolean) -> Unit) {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                try {
                    cloudSyncRepository.firestore.collection("users").document(user.uid).delete().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    user.delete().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                signOut()
                onComplete(true)
            }
        } else {
            onComplete(false)
        }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    fun syncToCloud(dramas: List<Drama>) {
        viewModelScope.launch {
            if (!isOnline()) {
                android.widget.Toast.makeText(context, "Offline: Cannot sync to cloud.", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            _isSyncing.value = true
            try {
                kotlinx.coroutines.withTimeout(8000) {
                    cloudSyncRepository.syncToCloud(dramas)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.widget.Toast.makeText(context, "Sync timed out. Connection is too slow.", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Sync failed.", android.widget.Toast.LENGTH_SHORT).show()
            }
            _isSyncing.value = false
        }
    }

    suspend fun fetchFromCloud(): com.skech.moglog.data.repository.CloudData? {
        if (!isOnline()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(context, "Offline: Using local data.", android.widget.Toast.LENGTH_SHORT).show()
            }
            return null
        }
        _isSyncing.value = true
        var data: com.skech.moglog.data.repository.CloudData? = null
        try {
            kotlinx.coroutines.withTimeout(8000) {
                data = cloudSyncRepository.fetchFromCloud()
                if (data != null) {
                    if (!data!!.apiKey.isNullOrEmpty()) {
                        apiKeyManager.setApiKey(data!!.apiKey)
                    }
                    data!!.dramas.forEach { drama ->
                        dramaRepository.saveDrama(drama)
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(context, "Fetch timed out.", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
        }
        _isSyncing.value = false
        return data
    }
}
