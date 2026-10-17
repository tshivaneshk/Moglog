package com.skech.moglog.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.skech.moglog.data.auth.GoogleAuthClient
import com.skech.moglog.data.auth.SignInResult
import com.skech.moglog.data.local.ApiKeyManager
import com.skech.moglog.data.repository.CloudData
import com.skech.moglog.data.repository.CloudSyncRepository
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.domain.repository.DramaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val authClient: GoogleAuthClient,
    private val cloudSyncRepository: CloudSyncRepository,
    private val dramaRepository: DramaRepository,
    private val apiKeyManager: ApiKeyManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authClient.getSignedInUser())
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    fun signIn(result: SignInResult) {
        _currentUser.value = result.data
        if (result.data != null) {
            viewModelScope.launch {
                fetchFromCloud()
            }
        }
    }

    fun createAccountAndSync(result: SignInResult, dramas: List<Drama>, onComplete: ((Boolean) -> Unit)? = null) {
        _currentUser.value = result.data
        if (result.data != null) {
            viewModelScope.launch {
                val success = syncToCloudInternal(dramas)
                onComplete?.invoke(success)
            }
        } else {
            onComplete?.invoke(false)
        }
    }

    fun signOut() {
        authClient.signOut()
        _currentUser.value = null
        // Do NOT wipe the user's local TMDB API key on sign-out
    }

    suspend fun checkAccountExists(uid: String): Boolean {
        return try {
            val doc = cloudSyncRepository.firestore.collection("users").document(uid).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    fun deleteAccount(deleteLocalData: Boolean = false, onComplete: (Boolean, String?) -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            onComplete(false, "Not signed in")
            return
        }
        viewModelScope.launch {
            try {
                // Delete backup subcollection chunks if present
                val backupDocs = cloudSyncRepository.firestore
                    .collection("users")
                    .document(user.uid)
                    .collection("backup")
                    .get()
                    .await()
                for (doc in backupDocs.documents) {
                    doc.reference.delete().await()
                }
                cloudSyncRepository.firestore.collection("users").document(user.uid).delete().await()
                user.delete().await()
                if (deleteLocalData) {
                    dramaRepository.clearAllDramas()
                }
                signOut()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Re-authentication required to delete account.")
            }
        }
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun syncToCloudInternal(dramas: List<Drama>): Boolean {
        if (!isOnline()) {
            Toast.makeText(context, "Offline: Cannot sync to cloud.", Toast.LENGTH_SHORT).show()
            return false
        }
        _isSyncing.value = true
        var success = false
        try {
            withTimeout(15000) {
                cloudSyncRepository.syncToCloud(dramas)
                success = true
            }
        } catch (e: TimeoutCancellationException) {
            android.util.Log.e("MoglogAuth", "Cloud sync timed out", e)
            Toast.makeText(context, "Sync timed out. Connection is too slow.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("MoglogAuth", "Cloud sync failed", e)
            Toast.makeText(context, "Sync failed: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        } finally {
            _isSyncing.value = false
        }
        return success
    }

    fun syncToCloud(dramas: List<Drama>) {
        viewModelScope.launch {
            syncToCloudInternal(dramas)
        }
    }

    suspend fun fetchFromCloud(): CloudData? {
        if (!isOnline()) {
            return null
        }
        _isSyncing.value = true
        var data: CloudData? = null
        try {
            withTimeout(8000) {
                data = cloudSyncRepository.fetchFromCloud()
                val fetchedDramas = data?.dramas
                if (!fetchedDramas.isNullOrEmpty()) {
                    val localMap = dramaRepository.getAllDramas().first().associateBy { it.id }
                    fetchedDramas.forEach { remoteDrama ->
                        val local = localMap[remoteDrama.id]
                        if (local == null || remoteDrama.updatedAt >= local.updatedAt) {
                            dramaRepository.saveDrama(remoteDrama)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        _isSyncing.value = false
        return data
    }
}
