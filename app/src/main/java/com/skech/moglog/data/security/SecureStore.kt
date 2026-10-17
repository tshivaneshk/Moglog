package com.skech.moglog.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.skech.moglog.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware-backed encrypted storage for application secrets using Google Tink AEAD (AES256_GCM).
 * Replaces deprecated androidx.security:security-crypto and plaintext SharedPreferences.
 */
@Singleton
class SecureStore @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    private val aead: Aead? by lazy {
        try {
            AeadConfig.register()
            val keysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
            keysetHandle.getPrimitive(Aead::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private val _hasApiKeyFlow = MutableStateFlow(false)
    val hasApiKeyFlow: StateFlow<Boolean> = _hasApiKeyFlow.asStateFlow()

    init {
        migrateLegacyPlaintextApiKeyIfNeeded()
        _hasApiKeyFlow.value = getTmdbCredential().isNotBlank()
    }

    private fun migrateLegacyPlaintextApiKeyIfNeeded() {
        try {
            val legacyPrefs = context.getSharedPreferences("moglog_prefs", Context.MODE_PRIVATE)
            if (legacyPrefs.contains("tmdb_api_key")) {
                val legacyKey = legacyPrefs.getString("tmdb_api_key", "")?.trim().orEmpty()
                // Do not migrate the compromised hardcoded key
                if (legacyKey.isNotBlank() && legacyKey != COMPROMISED_LEGACY_KEY && getTmdbCredential().isBlank()) {
                    setTmdbCredential(legacyKey)
                }
                legacyPrefs.edit().remove("tmdb_api_key").apply()
            }
        } catch (_: Exception) {
        }
    }

    fun getTmdbCredential(): String {
        val stored = decryptString(KEY_TMDB_CREDENTIAL, AAD_TMDB_CREDENTIAL)
        if (!stored.isNullOrBlank()) return stored
        return BuildConfig.DEV_TMDB_API_KEY.trim()
    }

    fun getUserTmdbCredentialOrEmpty(): String {
        return decryptString(KEY_TMDB_CREDENTIAL, AAD_TMDB_CREDENTIAL).orEmpty()
    }

    fun setTmdbCredential(credential: String) {
        val clean = credential.trim()
        if (clean.isEmpty()) {
            prefs.edit().remove(KEY_TMDB_CREDENTIAL).apply()
            _hasApiKeyFlow.value = BuildConfig.DEV_TMDB_API_KEY.isNotBlank()
        } else {
            encryptAndPutString(KEY_TMDB_CREDENTIAL, clean, AAD_TMDB_CREDENTIAL)
            _hasApiKeyFlow.value = true
        }
    }

    /**
     * Validates either a TMDB v3 API key (32 hex chars) or a TMDB v4 Read Access Token (JWT).
     */
    fun isValidTmdbCredentialFormat(input: String): Boolean {
        val clean = input.trim()
        val isV3Hex = clean.matches(Regex("^[a-fA-F0-9]{32}$"))
        val isV4Jwt = clean.startsWith("eyJ") && clean.count { it == '.' } == 2 && clean.length >= 60
        return isV3Hex || isV4Jwt
    }

    fun isBearerToken(credential: String = getTmdbCredential()): Boolean {
        val clean = credential.trim()
        return clean.startsWith("eyJ") && clean.count { it == '.' } == 2
    }

    /**
     * Returns or generates the 256-bit random passphrase used to encrypt the SQLCipher Room database.
     */
    @Synchronized
    fun getOrCreateDatabaseKey(): ByteArray {
        val existingBase64 = decryptString(KEY_DB_PASSPHRASE, AAD_DB_PASSPHRASE)
        if (!existingBase64.isNullOrBlank()) {
            return Base64.decode(existingBase64, Base64.NO_WRAP)
        }
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val encoded = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
        encryptAndPutString(KEY_DB_PASSPHRASE, encoded, AAD_DB_PASSPHRASE)
        return randomBytes
    }

    /**
     * Optional Keystore-wrapped cache for the user's cloud backup passphrase / derived key.
     */
    fun getCachedBackupPassphrase(): String? {
        return decryptString(KEY_BACKUP_PASSPHRASE, AAD_BACKUP_PASSPHRASE)?.takeIf { it.isNotBlank() }
    }

    fun setCachedBackupPassphrase(passphrase: String?) {
        if (passphrase.isNullOrBlank()) {
            prefs.edit().remove(KEY_BACKUP_PASSPHRASE).apply()
        } else {
            encryptAndPutString(KEY_BACKUP_PASSPHRASE, passphrase, AAD_BACKUP_PASSPHRASE)
        }
    }

    private fun encryptAndPutString(key: String, plaintext: String, aad: ByteArray) {
        val primitive = aead
        if (primitive != null) {
            val ciphertext = primitive.encrypt(plaintext.toByteArray(StandardCharsets.UTF_8), aad)
            val encoded = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            prefs.edit().putString(key, encoded).apply()
        } else {
            // Fallback for JVM unit tests where Android Keystore is unavailable
            val encoded = Base64.encodeToString(plaintext.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            prefs.edit().putString("${key}_fallback", encoded).apply()
        }
    }

    private fun decryptString(key: String, aad: ByteArray): String? {
        val primitive = aead
        if (primitive != null) {
            val encoded = prefs.getString(key, null) ?: return null
            return try {
                val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
                val decrypted = primitive.decrypt(ciphertext, aad)
                String(decrypted, StandardCharsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        } else {
            val encoded = prefs.getString("${key}_fallback", null) ?: return null
            return try {
                String(Base64.decode(encoded, Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }
    }

    companion object {
        private const val PREFS_FILE_NAME = "moglog_secure_store"
        private const val KEYSET_PREFS_NAME = "moglog_tink_keyset_prefs"
        private const val KEYSET_NAME = "moglog_master_keyset"
        private const val MASTER_KEY_URI = "android-keystore://moglog_tink_master_key"

        private const val KEY_TMDB_CREDENTIAL = "enc_tmdb_credential"
        private const val KEY_DB_PASSPHRASE = "enc_db_passphrase"
        private const val KEY_BACKUP_PASSPHRASE = "enc_backup_passphrase"

        private val AAD_TMDB_CREDENTIAL = "moglog:tmdb_credential:v1".toByteArray(StandardCharsets.UTF_8)
        private val AAD_DB_PASSPHRASE = "moglog:db_passphrase:v1".toByteArray(StandardCharsets.UTF_8)
        private val AAD_BACKUP_PASSPHRASE = "moglog:backup_passphrase:v1".toByteArray(StandardCharsets.UTF_8)

        private const val COMPROMISED_LEGACY_KEY = "b30d871ab7f51c18054f0e6d0921da16"
    }
}
