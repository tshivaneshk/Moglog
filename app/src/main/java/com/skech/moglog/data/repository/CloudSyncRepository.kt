package com.skech.moglog.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

        val data = hashMapOf<String, Any>(
            "dramas" to dramas,
            "lastSyncedAt" to System.currentTimeMillis(),
            // Explicitly delete any legacy plaintext tmdbApiKey stored in Firestore
            "tmdbApiKey" to FieldValue.delete()
        )

        try {
            firestore.collection("users")
                .document(user.uid)
                .set(data, SetOptions.merge())
                .await()
        } catch (_: Exception) {
        }
    }

    suspend fun fetchFromCloud(): CloudData? {
        val user = auth.currentUser ?: return null

        return try {
            val docRef = firestore.collection("users").document(user.uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                // If legacy doc still has tmdbApiKey in cloud, purge it from Firestore immediately
                if (snapshot.contains("tmdbApiKey")) {
                    try {
                        docRef.update(mapOf("tmdbApiKey" to FieldValue.delete())).await()
                    } catch (_: Exception) {
                    }
                }

                @Suppress("UNCHECKED_CAST")
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
                CloudData(dramas = dramas)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

data class CloudData(
    val dramas: List<Drama>
)
