package com.skech.moglog.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.skech.moglog.data.local.ApiKeyManager
import com.skech.moglog.domain.model.Drama
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val apiKeyManager: ApiKeyManager
) {
    suspend fun syncToCloud(dramas: List<Drama>) {
        val user = auth.currentUser ?: return
        val apiKey = apiKeyManager.getApiKey() ?: ""
        
        val data = hashMapOf(
            "tmdbApiKey" to apiKey,
            "dramas" to dramas,
            "lastSyncedAt" to System.currentTimeMillis()
        )
        
        try {
            firestore.collection("users")
                .document(user.uid)
                .set(data)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchFromCloud(): CloudData? {
        val user = auth.currentUser ?: return null
        
        return try {
            val snapshot = firestore.collection("users")
                .document(user.uid)
                .get()
                .await()
                
            if (snapshot.exists()) {
                val apiKey = snapshot.getString("tmdbApiKey")
                
                // Firestore arrays of objects come back as Lists of HashMaps, we need to map them back
                val dramasRaw = snapshot.get("dramas") as? List<Map<String, Any>> ?: emptyList()
                val dramas = dramasRaw.map { map ->
                    Drama(
                        id = (map["id"] as? Number)?.toInt() ?: 0,
                        title = map["title"] as? String ?: "",
                        overview = map["overview"] as? String ?: "",
                        rating = (map["rating"] as? Number)?.toDouble() ?: 0.0,
                        posterPath = map["posterPath"] as? String ?: "",
                        status = map["status"] as? String ?: "Planned",
                        episodesWatched = (map["episodesWatched"] as? Number)?.toInt() ?: 0,
                        totalEpisodes = (map["totalEpisodes"] as? Number)?.toInt() ?: 0,
                        isDeleted = (map["isDeleted"] as? Boolean) ?: (map["deleted"] as? Boolean) ?: false,
                        addedAt = (map["addedAt"] as? Number)?.toLong() ?: 0L,
                        updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                        completedAt = (map["completedAt"] as? Number)?.toLong() ?: 0L
                    )
                }
                CloudData(apiKey, dramas)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class CloudData(
    val apiKey: String?,
    val dramas: List<Drama>
)
