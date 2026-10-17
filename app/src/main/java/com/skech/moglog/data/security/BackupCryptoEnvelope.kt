package com.skech.moglog.data.security

import com.google.gson.Gson
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Versioned client-side encryption envelope for Cloud Backup and `.moglog` file exports.
 *
 * Cryptography parameters:
 * - KDF: PBKDF2WithHmacSHA256, 600,000 iterations, 16-byte random salt, 256-bit derived key
 * - Cipher: AES/GCM/NoPadding, 12-byte random IV, 128-bit authentication tag
 * - AAD: "moglog-backup:v<schemaVersion>:uid=<uid>" bound into GCM authentication tag
 */
data class BackupEnvelopePayload(
    val format: String = FORMAT_MAGIC,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val kdf: String = KDF_ALGO,
    val kdfIterations: Int = DEFAULT_ITERATIONS,
    val salt: String,
    val iv: String,
    val ciphertext: String,
    val uid: String = "local",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val FORMAT_MAGIC = "MOGLOG_ENC_V1"
        const val CURRENT_SCHEMA_VERSION = 1
        const val KDF_ALGO = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 600_000
    }
}

object BackupCryptoEnvelope {
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

    private val gson = Gson()
    private val secureRandom = SecureRandom()

    fun encrypt(
        plaintextJson: String,
        passphrase: String,
        uid: String = "local",
        schemaVersion: Int = BackupEnvelopePayload.CURRENT_SCHEMA_VERSION,
        iterations: Int = BackupEnvelopePayload.DEFAULT_ITERATIONS
    ): BackupEnvelopePayload {
        require(passphrase.isNotBlank()) { "Passphrase must not be blank" }

        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { secureRandom.nextBytes(it) }

        val keyBytes = deriveKey(passphrase, salt, iterations)
        return try {
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(buildAad(uid, schemaVersion))

            val ciphertextBytes = cipher.doFinal(plaintextJson.toByteArray(StandardCharsets.UTF_8))

            BackupEnvelopePayload(
                schemaVersion = schemaVersion,
                kdfIterations = iterations,
                salt = Base64.getEncoder().encodeToString(salt),
                iv = Base64.getEncoder().encodeToString(iv),
                ciphertext = Base64.getEncoder().encodeToString(ciphertextBytes),
                uid = uid
            )
        } finally {
            keyBytes.fill(0)
        }
    }

    fun decrypt(
        envelope: BackupEnvelopePayload,
        passphrase: String,
        expectedUid: String? = null
    ): String {
        require(passphrase.isNotBlank()) { "Passphrase required to decrypt backup" }
        val uidForAad = expectedUid ?: envelope.uid

        val salt = Base64.getDecoder().decode(envelope.salt)
        val iv = Base64.getDecoder().decode(envelope.iv)
        val ciphertextBytes = Base64.getDecoder().decode(envelope.ciphertext)

        val iterations = envelope.kdfIterations.coerceIn(10_000, 2_000_000)
        val keyBytes = deriveKey(passphrase, salt, iterations)
        return try {
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(buildAad(uidForAad, envelope.schemaVersion))

            val decrypted = cipher.doFinal(ciphertextBytes)
            String(decrypted, StandardCharsets.UTF_8)
        } finally {
            keyBytes.fill(0)
        }
    }

    fun serializeEnvelope(envelope: BackupEnvelopePayload): String {
        return gson.toJson(envelope)
    }

    fun parseEnvelopeOrNull(rawJson: String): BackupEnvelopePayload? {
        if (!rawJson.trimStart().startsWith("{")) return null
        return try {
            val parsed = gson.fromJson(rawJson, BackupEnvelopePayload::class.java)
            if (parsed?.format == BackupEnvelopePayload.FORMAT_MAGIC &&
                parsed.salt.isNotBlank() &&
                parsed.iv.isNotBlank() &&
                parsed.ciphertext.isNotBlank()
            ) {
                parsed
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int): ByteArray {
        val chars = passphrase.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, KEY_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(BackupEnvelopePayload.KDF_ALGO)
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }

    private fun buildAad(uid: String, schemaVersion: Int): ByteArray {
        return "moglog-backup:v$schemaVersion:uid=$uid".toByteArray(StandardCharsets.UTF_8)
    }
}
