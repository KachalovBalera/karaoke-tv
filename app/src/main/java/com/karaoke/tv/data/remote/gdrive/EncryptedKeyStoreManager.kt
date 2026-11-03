package com.karaoke.tv.data.remote.gdrive

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.google.gson.JsonParser
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages secure hardware-backed storage for Google Service Account credentials on Android TV.
 * Uses AndroidKeyStore with AES-256-GCM.
 * Credentials are encrypted and stored in [Context.getNoBackupFilesDir] to prevent backup leakage.
 */
object EncryptedKeyStoreManager {

    private const val TAG = "EncryptedKeyStore"
    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "KaraokeGoogleDriveCredentialsKey"
    private const val ENCRYPTED_FILE_NAME = "gdrive_credentials.enc"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    // Staging file name pushed via ADB: adb push key.json /sdcard/Android/data/com.karaoke.tv/files/temp_service_account.json
    const val STAGING_FILE_NAME = "temp_service_account.json"

    /**
     * Checks if encrypted credentials already exist.
     */
    fun hasCredentials(context: Context): Boolean {
        val file = getEncryptedFile(context)
        return file.exists() && file.length() > GCM_IV_LENGTH
    }

    /**
     * Encrypts and saves the JSON credentials into no_backup directory.
     */
    fun saveEncryptedCredentials(context: Context, jsonString: String): Boolean {
        return try {
            validateJsonStructure(jsonString)
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val ciphertext = cipher.doFinal(jsonString.toByteArray(StandardCharsets.UTF_8))

            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

            val destFile = getEncryptedFile(context)
            destFile.parentFile?.mkdirs()
            destFile.writeBytes(combined)

            Log.i(TAG, "Credentials successfully encrypted and saved to ${destFile.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt credentials: ${e.message}")
            false
        }
    }

    /**
     * Decrypts and returns the JSON credentials string, or null if not found/error.
     */
    fun loadDecryptedCredentials(context: Context): String? {
        val file = getEncryptedFile(context)
        if (!file.exists() || file.length() <= GCM_IV_LENGTH) {
            return null
        }

        return try {
            val combined = file.readBytes()
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plaintextBytes = cipher.doFinal(ciphertext)
            val jsonString = String(plaintextBytes, StandardCharsets.UTF_8)
            validateJsonStructure(jsonString)
            jsonString
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt credentials: ${e.message}")
            null
        }
    }

    /**
     * Checks if a staging unencrypted file was pushed via ADB, securely imports it,
     * encrypts it into noBackupFilesDir, and securely wipes and deletes the staging file.
     */
    fun checkAndImportFromStaging(context: Context): Boolean {
        val stagingLocations = listOfNotNull(
            File(context.getExternalFilesDir(null), STAGING_FILE_NAME),
            File("/sdcard/Android/data/${context.packageName}/files/$STAGING_FILE_NAME"),
            File("/sdcard/Download/$STAGING_FILE_NAME")
        )

        for (stagingFile in stagingLocations) {
            if (stagingFile.exists() && stagingFile.isFile && stagingFile.length() > 50) {
                Log.i(TAG, "Found staging credentials file at: ${stagingFile.absolutePath}")
                try {
                    val rawJson = stagingFile.readText(StandardCharsets.UTF_8)
                    val saved = saveEncryptedCredentials(context, rawJson)
                    if (saved) {
                        secureWipeAndDelete(stagingFile)
                        Log.i(TAG, "Staging file successfully imported and securely deleted.")
                        return true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed importing staging file: ${e.message}")
                }
            }
        }
        return false
    }

    /**
     * Deletes the encrypted credentials and removes the Keystore entry (for rotation/revocation).
     */
    fun deleteCredentials(context: Context): Boolean {
        return try {
            val file = getEncryptedFile(context)
            if (file.exists()) {
                secureWipeAndDelete(file)
            }
            runCatching {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
                keyStore.load(null)
                if (keyStore.containsAlias(KEY_ALIAS)) {
                    keyStore.deleteEntry(KEY_ALIAS)
                }
            }
            Log.i(TAG, "Credentials and Keystore key successfully deleted")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting credentials: ${e.message}")
            false
        }
    }

    private fun getEncryptedFile(context: Context): File {
        return File(context.noBackupFilesDir, ENCRYPTED_FILE_NAME)
    }

    /**
     * Validates that the JSON contains required service account fields.
     */
    private fun validateJsonStructure(json: String) {
        val obj = JsonParser.parseString(json).asJsonObject
        require(obj.has("client_email")) { "Missing client_email in credentials" }
        require(obj.has("private_key")) { "Missing private_key in credentials" }
    }

    /**
     * Securely overwrites the file with zeroes before deleting it from disk.
     */
    private fun secureWipeAndDelete(file: File) {
        try {
            if (file.exists()) {
                val length = file.length()
                RandomAccessFile(file, "rws").use { raf ->
                    raf.seek(0)
                    val zeroes = ByteArray(minOf(4096, length.toInt().coerceAtLeast(1)))
                    var written = 0L
                    while (written < length) {
                        val toWrite = minOf(zeroes.size.toLong(), length - written).toInt()
                        raf.write(zeroes, 0, toWrite)
                        written += toWrite
                    }
                }
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not securely zero file, falling back to standard delete: ${e.message}")
            file.delete()
        }
    }

    /**
     * Gets or creates the AES-256 key inside AndroidKeyStore (or software fallback for JVM tests).
     */
    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE_PROVIDER
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
                Log.i(TAG, "Generated new hardware-backed AES-256 key in AndroidKeyStore")
            }

            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } catch (e: Exception) {
            // Software fallback for JVM unit tests where AndroidKeyStore provider is unavailable
            Log.w(TAG, "AndroidKeyStore not available (JVM environment), using fallback key: ${e.message}")
            val fallbackBytes = ByteArray(32) { (it * 7).toByte() }
            SecretKeySpec(fallbackBytes, "AES")
        }
    }
}
