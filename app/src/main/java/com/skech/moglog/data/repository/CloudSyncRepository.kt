package com.skech.moglog.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skech.moglog.data.local.ApiKeyManager
import com.skech.moglog.data.local.SettingsStore
import com.skech.moglog.data.security.BackupCryptoEnvelope
import com.skech.moglog.data.security.BackupEnvelopePayload
import com.skech.moglog.data.security.SecureStore
import com.skech.moglog.domain.model.Drama
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val apiKeyManager: ApiKeyManager,
    private val secureStore: SecureStore,
    private val settingsStore: SettingsStore
) {
    private val gson = Gson()

    suspend fun syncToCloud(dramas: List<Drama>) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val now = System.currentTimeMillis()
        val settings = settingsStore.settingsState.value
        val passphrase = secureStore.getBackupPassphrase()
        val useE2e = settings.encryptedCloudBackupEnabled && passphrase.isNotBlank()

        try {
            val userDocRef = firestore.collection("users").document(uid)
            val backupColl = userDocRef.collection("backup")

            if (useE2e) {
                val plaintextJson = gson.toJson(dramas)
                val envelope = BackupCryptoEnvelope.encrypt(
                    plaintextJson = plaintextJson,
                    passphrase = passphrase,
                    uid = uid
                )

                // Split ciphertext into safe chunks (max 450 KB per document) so large libraries never hit 1 MB limit
                val chunks = envelope.ciphertext.chunked(MAX_CHUNK_CHARS)
                for ((idx, chunkText) in chunks.withIndex()) {
                    val chunkDoc = hashMapOf<String, Any>(
                        "index" to idx,
                        "totalChunks" to chunks.size,
                        "encrypted" to true,
                        "format" to envelope.format,
                        "schemaVersion" to envelope.schemaVersion,
                        "kdf" to envelope.kdf,
                        "kdfIterations" to envelope.kdfIterations,
                        "salt" to envelope.salt,
                        "iv" to envelope.iv,
                        "ciphertextChunk" to chunkText,
                        "updatedAt" to now
                    )
                    backupColl.document("chunk_$idx").set(chunkDoc).await()
                }

                // Remove any stale extra chunks from previous larger backups
                val existingChunks = backupColl.get().await()
                for (doc in existingChunks.documents) {
                    val idx = (doc.get("index") as? Number)?.toInt() ?: -1
                    if (idx >= chunks.size) {
                        doc.reference.delete().await()
                    }
                }

                // Store only metadata in root user doc and wipe plaintext dramas + legacy tmdbApiKey
                val rootMeta = hashMapOf<String, Any>(
                    "encryptedBackup" to true,
                    "backupChunks" to chunks.size,
                    "lastSyncedAt" to now,
                    "dramas" to FieldValue.delete(),
                    "tmdbApiKey" to FieldValue.delete()
                )
                userDocRef.set(rootMeta, SetOptions.merge()).await()
            } else {
                // Chunked JSON backup in users/{uid}/backup/{chunkId} + root metadata
                val json = gson.toJson(dramas)
                val chunks = json.chunked(MAX_CHUNK_CHARS)
                for ((idx, chunkText) in chunks.withIndex()) {
                    val chunkDoc = hashMapOf<String, Any>(
                        "index" to idx,
                        "totalChunks" to chunks.size,
                        "encrypted" to false,
                        "payloadChunk" to chunkText,
                        "updatedAt" to now
                    )
                    backupColl.document("chunk_$idx").set(chunkDoc).await()
                }

                val rootData = hashMapOf<String, Any>(
                    "encryptedBackup" to false,
                    "backupChunks" to chunks.size,
                    "dramas" to dramas,
                    "lastSyncedAt" to now,
                    // Explicitly delete any legacy plaintext tmdbApiKey stored in Firestore
                    "tmdbApiKey" to FieldValue.delete()
                )
                userDocRef.set(rootData, SetOptions.merge()).await()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun fetchFromCloud(explicitPassphrase: String? = null): CloudData? {
        val user = auth.currentUser ?: return null
        val uid = user.uid

        return try {
            val docRef = firestore.collection("users").document(uid)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) return null

            // If legacy doc still has tmdbApiKey in cloud, purge it from Firestore immediately
            if (snapshot.contains("tmdbApiKey")) {
                try {
                    docRef.update(mapOf("tmdbApiKey" to FieldValue.delete())).await()
                } catch (_: Exception) {
                }
            }

            // 1. Check subcollection `users/{uid}/backup` first
            val backupDocs = docRef.collection("backup").get().await().documents
                .sortedBy { (it.get("index") as? Number)?.toInt() ?: 0 }

            if (backupDocs.isNotEmpty()) {
                val firstDoc = backupDocs.first()
                val isEncrypted = firstDoc.getBoolean("encrypted") == true
                if (isEncrypted) {
                    val pass = explicitPassphrase?.takeIf { it.isNotBlank() }
                        ?: secureStore.getBackupPassphrase()
                    if (pass.isBlank()) {
                        return null
                    }
                    val fullCiphertext = backupDocs.joinToString("") {
                        it.getString("ciphertextChunk").orEmpty()
                    }
                    val envelope = BackupEnvelopePayload(
                        format = firstDoc.getString("format") ?: BackupEnvelopePayload.FORMAT_MAGIC,
                        schemaVersion = (firstDoc.get("schemaVersion") as? Number)?.toInt()
                            ?: BackupEnvelopePayload.CURRENT_SCHEMA_VERSION,
                        kdf = firstDoc.getString("kdf") ?: BackupEnvelopePayload.KDF_ALGO,
                        kdfIterations = (firstDoc.get("kdfIterations") as? Number)?.toInt()
                            ?: BackupEnvelopePayload.DEFAULT_ITERATIONS,
                        salt = firstDoc.getString("salt").orEmpty(),
                        iv = firstDoc.getString("iv").orEmpty(),
                        ciphertext = fullCiphertext,
                        uid = uid
                    )
                    val decryptedJson = BackupCryptoEnvelope.decrypt(
                        envelope = envelope,
                        passphrase = pass,
                        expectedUid = uid
                    )
                    val listType = object : TypeToken<List<Drama>>() {}.type
                    val dramas: List<Drama> = gson.fromJson(decryptedJson, listType) ?: emptyList()
                    return CloudData(dramas = dramas)
                } else {
                    val fullJson = backupDocs.joinToString("") {
                        it.getString("payloadChunk").orEmpty()
                    }
                    if (fullJson.isNotBlank()) {
                        val listType = object : TypeToken<List<Drama>>() {}.type
                        val dramas: List<Drama> = gson.fromJson(fullJson, listType) ?: emptyList()
                        return CloudData(dramas = dramas)
                    }
                }
            }

            // 2. Fallback to legacy root `dramas` array
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
                    completedAt = (map["completedAt"] as? Number)?.toLong() ?: 0L,
                    myRating = (map["myRating"] as? Number)?.toDouble() ?: 0.0,
                    notes = map["notes"] as? String ?: "",
                    tags = map["tags"] as? String ?: "",
                    genres = map["genres"] as? String ?: "",
                    releaseYear = (map["releaseYear"] as? Number)?.toInt() ?: 0,
                    totalSeasons = (map["totalSeasons"] as? Number)?.toInt() ?: 0,
                    currentSeason = (map["currentSeason"] as? Number)?.toInt() ?: 1,
                    currentEpisode = (map["currentEpisode"] as? Number)?.toInt() ?: 0,
                    deletedAt = (map["deletedAt"] as? Number)?.toLong() ?: 0L
                )
            }
            CloudData(dramas = dramas)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val MAX_CHUNK_CHARS = 450_000
    }
}

data class CloudData(
    val dramas: List<Drama>
)
