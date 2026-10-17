package com.skech.moglog.data.security

import android.content.Context
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.io.RandomAccessFile

/**
 * Crash-safe helper that migrates an existing unencrypted Room SQLite database (`drama_db`)
 * to a 256-bit SQLCipher encrypted database on first upgrade, and provides the
 * [SupportOpenHelperFactory] for Room.
 */
object SqlCipherMigrationHelper {

    private val SQLITE_PLAINTEXT_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun createEncryptedOpenHelperFactory(
        context: Context,
        dbName: String,
        secureStore: SecureStore
    ): SupportOpenHelperFactory {
        try {
            System.loadLibrary("sqlcipher")
        } catch (_: Throwable) {
        }

        val passphraseBytes = secureStore.getOrCreateDatabaseKey()
        try {
            migratePlaintextDbIfNeeded(context, dbName, passphraseBytes)
        } catch (e: Exception) {
            android.util.Log.e("SqlCipherMigration", "Database migration to SQLCipher failed: ${e.message}", e)
        }
        return SupportOpenHelperFactory(passphraseBytes)
    }

    private fun migratePlaintextDbIfNeeded(
        context: Context,
        dbName: String,
        passphraseBytes: ByteArray
    ) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists() || dbFile.length() < SQLITE_PLAINTEXT_HEADER.size) {
            return
        }

        if (!isPlaintextSqlite(dbFile)) {
            return
        }

        val tempEncryptedFile = File(dbFile.parentFile, "${dbName}_sqlcipher_tmp.db")
        if (tempEncryptedFile.exists()) {
            tempEncryptedFile.delete()
        }

        // Open/create the target encrypted database using SQLCipher and the generated key
        val encryptedDb = SQLiteDatabase.openOrCreateDatabase(
            tempEncryptedFile.absolutePath,
            passphraseBytes,
            null,
            null
        )

        try {
            // Attach the existing plaintext database with an empty key ('')
            encryptedDb.rawExecSQL(
                "ATTACH DATABASE '${dbFile.absolutePath}' AS plaintext KEY '';"
            )
            // Query source user_version before exporting
            val cursor = encryptedDb.rawQuery("PRAGMA plaintext.user_version;", null)
            val sourceVersion = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            cursor.close()

            // Export schema, tables, indices, and data from plaintext INTO main (the open encrypted database)
            encryptedDb.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext');")

            // Preserve the Room user_version on the newly created encrypted database so Room runs migrations
            encryptedDb.rawExecSQL("PRAGMA main.user_version = $sourceVersion;")
            encryptedDb.rawExecSQL("DETACH DATABASE plaintext;")
        } finally {
            encryptedDb.close()
        }

        if (tempEncryptedFile.exists() && tempEncryptedFile.length() > 0L) {
            // Remove old plaintext WAL/SHM sidecars and atomically replace with encrypted DB
            File("${dbFile.absolutePath}-wal").delete()
            File("${dbFile.absolutePath}-shm").delete()
            File("${dbFile.absolutePath}-journal").delete()

            val backupOld = File(dbFile.parentFile, "${dbName}_plaintext_bak")
            if (backupOld.exists()) backupOld.delete()

            if (dbFile.renameTo(backupOld)) {
                if (tempEncryptedFile.renameTo(dbFile)) {
                    backupOld.delete()
                } else {
                    backupOld.renameTo(dbFile)
                }
            }
        }
    }

    private fun isPlaintextSqlite(file: File): Boolean {
        return try {
            val header = ByteArray(SQLITE_PLAINTEXT_HEADER.size)
            RandomAccessFile(file, "r").use { raf ->
                val read = raf.read(header)
                read == SQLITE_PLAINTEXT_HEADER.size && header.contentEquals(SQLITE_PLAINTEXT_HEADER)
            }
        } catch (_: Exception) {
            false
        }
    }
}
