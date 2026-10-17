package com.skech.moglog.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.util.Base64
import javax.crypto.AEADBadTagException

class BackupCryptoEnvelopeTest {

    private val testIterations = 20_000 // Use fast iteration count in unit tests while testing full PBKDF2+AES-GCM pipeline

    @Test
    fun `encrypt and decrypt round-trip preserves exact UTF-8 JSON payload`() {
        val sampleJson = """[{"id":1399,"title":"Game of Thrones","myRating":9.5,"notes":"Winter is coming ❄️"}]"""
        val passphrase = "correct-horse-battery-staple"
        val uid = "user_abc_123"

        val envelope = BackupCryptoEnvelope.encrypt(
            plaintextJson = sampleJson,
            passphrase = passphrase,
            uid = uid,
            iterations = testIterations
        )

        val serialized = BackupCryptoEnvelope.serializeEnvelope(envelope)
        val parsed = BackupCryptoEnvelope.parseEnvelopeOrNull(serialized)
        assertNotNull(parsed)

        val decrypted = BackupCryptoEnvelope.decrypt(
            envelope = parsed!!,
            passphrase = passphrase,
            expectedUid = uid
        )
        assertEquals(sampleJson, decrypted)
    }

    @Test
    fun `encrypt generates unique salt and IV on every invocation`() {
        val payload = """[{"id":1,"title":"Test"}]"""
        val pass = "same-passphrase-123"

        val env1 = BackupCryptoEnvelope.encrypt(payload, pass, iterations = testIterations)
        val env2 = BackupCryptoEnvelope.encrypt(payload, pass, iterations = testIterations)

        assertNotEquals(env1.salt, env2.salt)
        assertNotEquals(env1.iv, env2.iv)
        assertNotEquals(env1.ciphertext, env2.ciphertext)
    }

    @Test(expected = AEADBadTagException::class)
    fun `decrypt with wrong passphrase throws AEADBadTagException`() {
        val envelope = BackupCryptoEnvelope.encrypt(
            plaintextJson = """[{"id":42,"title":"Severance"}]""",
            passphrase = "right-passphrase",
            uid = "u1",
            iterations = testIterations
        )

        BackupCryptoEnvelope.decrypt(
            envelope = envelope,
            passphrase = "wrong-passphrase",
            expectedUid = "u1"
        )
    }

    @Test
    fun `decrypt with tampered ciphertext fails authentication`() {
        val envelope = BackupCryptoEnvelope.encrypt(
            plaintextJson = """[{"id":100,"title":"Shogun"}]""",
            passphrase = "secret-passphrase",
            uid = "u1",
            iterations = testIterations
        )

        val rawBytes = Base64.getDecoder().decode(envelope.ciphertext)
        rawBytes[0] = (rawBytes[0].toInt() xor 0xFF).toByte()
        val tampered = envelope.copy(ciphertext = Base64.getEncoder().encodeToString(rawBytes))

        try {
            BackupCryptoEnvelope.decrypt(tampered, "secret-passphrase", expectedUid = "u1")
            fail("Expected AEADBadTagException when ciphertext is tampered")
        } catch (_: AEADBadTagException) {
            // Expected
        }
    }

    @Test(expected = AEADBadTagException::class)
    fun `decrypt with mismatched AAD uid fails authentication`() {
        val envelope = BackupCryptoEnvelope.encrypt(
            plaintextJson = """[{"id":7,"title":"Arcane"}]""",
            passphrase = "secret-passphrase",
            uid = "owner_uid_1",
            iterations = testIterations
        )

        BackupCryptoEnvelope.decrypt(
            envelope = envelope,
            passphrase = "secret-passphrase",
            expectedUid = "attacker_uid_2"
        )
    }

    @Test
    fun `parseEnvelopeOrNull returns null for plain JSON array`() {
        val plainArray = """[{"id":1,"title":"Plain JSON"}]"""
        assertNull(BackupCryptoEnvelope.parseEnvelopeOrNull(plainArray))
    }
}
