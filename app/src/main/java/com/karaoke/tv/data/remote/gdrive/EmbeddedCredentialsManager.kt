package com.karaoke.tv.data.remote.gdrive

import android.content.Context
import android.util.Log
import com.google.gson.JsonParser
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages encrypted embedded credentials for Google Drive.
 * Allows the APK to work out-of-the-box on new devices without requiring ADB.
 * Credentials payload is stored as AES-256-CBC encrypted binary in assets.
 */
object EmbeddedCredentialsManager {

    private const val TAG = "EmbeddedCredentials"
    private const val ASSET_NAME = "embedded_gdrive.bin"
    private const val IV_SIZE = 16

    // Obfuscated compile-time key pieces (32 bytes total for AES-256)
    private val P1 = byteArrayOf(0x4b, 0x61, 0x72, 0x61, 0x6f, 0x6b, 0x65, 0x54) // "KaraokeT"
    private val P2 = byteArrayOf(0x56, 0x47, 0x44, 0x72, 0x69, 0x76, 0x65, 0x32) // "VGDrive2"
    private val P3 = byteArrayOf(0x30, 0x32, 0x36, 0x53, 0x65, 0x63, 0x75, 0x72) // "026Secur"
    private val P4 = byteArrayOf(0x65, 0x45, 0x6d, 0x62, 0x65, 0x64, 0x39, 0x39) // "eEmbed99"

    /**
     * Checks if the embedded encrypted credentials asset exists.
     */
    fun hasEmbeddedAsset(context: Context): Boolean {
        return try {
            val list = context.assets.list("") ?: return false
            list.contains(ASSET_NAME)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Decrypts and returns the embedded JSON credentials string, or null if error/missing.
     */
    fun loadEmbeddedCredentials(context: Context): String? {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.assets.open(ASSET_NAME)
            val allBytes = inputStream.readBytes()
            if (allBytes.size <= IV_SIZE) {
                Log.w(TAG, "Embedded asset too small: ${allBytes.size} bytes")
                return null
            }

            val iv = allBytes.copyOfRange(0, IV_SIZE)
            val ciphertext = allBytes.copyOfRange(IV_SIZE, allBytes.size)

            val fullKey = ByteArray(32)
            System.arraycopy(P1, 0, fullKey, 0, 8)
            System.arraycopy(P2, 0, fullKey, 8, 8)
            System.arraycopy(P3, 0, fullKey, 16, 8)
            System.arraycopy(P4, 0, fullKey, 24, 8)

            val keySpec = SecretKeySpec(fullKey, "AES")
            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            val jsonString = String(decryptedBytes, StandardCharsets.UTF_8)

            // Validate that decrypted payload is a valid Service Account JSON
            val root = JsonParser.parseString(jsonString).asJsonObject
            val type = root.get("type")?.asString
            val privateKey = root.get("private_key")?.asString
            val clientEmail = root.get("client_email")?.asString

            if (type == "service_account" && !privateKey.isNullOrBlank() && !clientEmail.isNullOrBlank()) {
                Log.i(TAG, "Successfully unpacked and verified embedded Google Drive credentials")
                jsonString
            } else {
                Log.e(TAG, "Embedded credentials verification failed: invalid schema")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load embedded credentials: ${e.message}")
            null
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
        }
    }
}
