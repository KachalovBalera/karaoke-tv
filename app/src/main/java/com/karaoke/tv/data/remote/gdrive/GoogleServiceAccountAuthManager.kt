package com.karaoke.tv.data.remote.gdrive

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Manages Google Cloud Service Account authentication using OAuth 2.0 JWT Bearer flow.
 * RFC 7523: JSON Web Token (JWT) Profile for OAuth 2.0 Client Authentication and Authorization Grants.
 */
class GoogleServiceAccountAuthManager(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val tokenEndpoint: String = "https://oauth2.googleapis.com/token"
) {

    companion object {
        private const val TAG = "GServiceAccountAuth"
        private const val DRIVE_READONLY_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
        private const val TOKEN_EXPIRY_BUFFER_SEC = 300L // 5 minutes safety margin
    }

    data class Credentials(
        val clientEmail: String,
        val privateKey: PrivateKey,
        val projectId: String = ""
    )

    private val mutex = Mutex()
    private var cachedCredentials: Credentials? = null
    private var cachedAccessToken: String? = null
    private var tokenExpiresAtSec: Long = 0L

    /**
     * Loads credentials from a JSON string.
     */
    fun loadCredentialsFromJson(jsonString: String): Credentials {
        val root = JsonParser.parseString(jsonString).asJsonObject
        val clientEmail = root.get("client_email").asString
        val privateKeyPem = root.get("private_key").asString
        val projectId = root.get("project_id")?.asString ?: ""

        val privateKey = parsePrivateKey(privateKeyPem)
        val creds = Credentials(
            clientEmail = clientEmail,
            privateKey = privateKey,
            projectId = projectId
        )
        cachedCredentials = creds
        // Invalidate old token on new credentials
        cachedAccessToken = null
        tokenExpiresAtSec = 0L
        runCatching { Log.i(TAG, "Loaded service account credentials for: $clientEmail (project: $projectId)") }
        return creds
    }

    /**
     * Loads credentials from a File.
     */
    fun loadCredentialsFromFile(file: File): Credentials {
        require(file.exists()) { "Credentials file not found: ${file.absolutePath}" }
        val content = file.readText(StandardCharsets.UTF_8)
        return loadCredentialsFromJson(content)
    }

    /**
     * Attempts to find and load credentials safely:
     * 1. Hardware-encrypted Keystore storage (no_backup/gdrive_credentials.enc)
     * 2. Staging file pushed via ADB (/sdcard/Android/data/.../temp_service_account.json)
     * 3. External secrets file (for local PC tests: C:/Users/Admin/.karaoke_secrets/service_account.json)
     */
    fun tryAutoLoadCredentials(context: Context): Boolean {
        // 1. Hardware Keystore encrypted credentials (production path)
        val encryptedJson = EncryptedKeyStoreManager.loadDecryptedCredentials(context)
        if (!encryptedJson.isNullOrBlank()) {
            return runCatching {
                loadCredentialsFromJson(encryptedJson)
                Log.i(TAG, "Successfully loaded credentials from hardware Keystore")
                true
            }.getOrDefault(false)
        }

        // 2. Check staging file pushed via ADB
        val imported = EncryptedKeyStoreManager.checkAndImportFromStaging(context)
        if (imported) {
            val newlyEncryptedJson = EncryptedKeyStoreManager.loadDecryptedCredentials(context)
            if (!newlyEncryptedJson.isNullOrBlank()) {
                return runCatching {
                    loadCredentialsFromJson(newlyEncryptedJson)
                    Log.i(TAG, "Successfully imported, encrypted and loaded credentials from staging")
                    true
                }.getOrDefault(false)
            }
        }

        // 3. Fallback for local PC / test environment
        val localSecrets = listOf(
            File("C:/Users/Admin/.karaoke_secrets/service_account.json"),
            File(context.filesDir, "service_account.json")
        )
        for (f in localSecrets) {
            if (f.exists()) {
                return runCatching {
                    loadCredentialsFromFile(f)
                    // Auto-encrypt into Keystore so future boots use Keystore
                    EncryptedKeyStoreManager.saveEncryptedCredentials(context, f.readText(StandardCharsets.UTF_8))
                    true
                }.getOrDefault(false)
            }
        }

        return false
    }

    /**
     * Gets a valid Bearer access token, automatically refreshing if expired or requested.
     * Implements exponential backoff retry on HTTP 429 and 5xx errors.
     */
    suspend fun getAccessToken(forceRefresh: Boolean = false): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val creds = cachedCredentials
                ?: throw IllegalStateException("Service account credentials not loaded. Call loadCredentialsFromJson/File first.")

            val nowSec = System.currentTimeMillis() / 1000L
            if (!forceRefresh && cachedAccessToken != null && nowSec < (tokenExpiresAtSec - TOKEN_EXPIRY_BUFFER_SEC)) {
                return@withLock cachedAccessToken!!
            }

            var attempt = 0
            val maxAttempts = 3
            var delayMs = 1000L

            while (attempt < maxAttempts) {
                attempt++
                try {
                    runCatching { Log.d(TAG, "Requesting fresh access token from $tokenEndpoint for ${creds.clientEmail} (attempt $attempt)...") }
                    val jwtAssertion = createSignedJwt(creds, nowSec)

                    val formBody = FormBody.Builder()
                        .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        .add("assertion", jwtAssertion)
                        .build()

                    val request = Request.Builder()
                        .url(tokenEndpoint)
                        .post(formBody)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    val responseBody = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        val json = JsonParser.parseString(responseBody).asJsonObject
                        val accessToken = json.get("access_token").asString
                        val expiresIn = json.get("expires_in")?.asLong ?: 3600L

                        cachedAccessToken = accessToken
                        tokenExpiresAtSec = nowSec + expiresIn

                        runCatching { Log.i(TAG, "Successfully acquired Google access token (valid for ${expiresIn}s)") }
                        return@withLock accessToken
                    }

                    val code = response.code
                    runCatching { Log.e(TAG, "Token request failed [HTTP $code]: $responseBody") }

                    if (code == 429 || code >= 500) {
                        // Rate limit or server error: retry with exponential backoff
                        if (attempt < maxAttempts) {
                            kotlinx.coroutines.delay(delayMs)
                            delayMs *= 2
                            continue
                        }
                    }

                    throw RuntimeException("Failed to obtain Google access token: HTTP $code - $responseBody")
                } catch (e: Exception) {
                    if (attempt >= maxAttempts) throw e
                    kotlinx.coroutines.delay(delayMs)
                    delayMs *= 2
                }
            }

            throw RuntimeException("Failed to acquire access token after $maxAttempts attempts")
        }
    }

    /**
     * Returns true if credentials are loaded.
     */
    val isConfigured: Boolean
        get() = cachedCredentials != null

    fun hasCredentials(): Boolean = isConfigured

    /**
     * Returns the client email of loaded credentials, or null.
     */
    val clientEmail: String?
        get() = cachedCredentials?.clientEmail

    /**
     * Creates an RS256 signed JWT assertion for the OAuth 2.0 token endpoint.
     */
    internal fun createSignedJwt(creds: Credentials, nowSec: Long): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()

        // 1. Header
        val headerJson = JsonObject().apply {
            addProperty("alg", "RS256")
            addProperty("typ", "JWT")
        }.toString()
        val headerBase64 = encoder.encodeToString(headerJson.toByteArray(StandardCharsets.UTF_8))

        // 2. Claims (Payload)
        val claimsJson = JsonObject().apply {
            addProperty("iss", creds.clientEmail)
            addProperty("scope", DRIVE_READONLY_SCOPE)
            addProperty("aud", tokenEndpoint)
            addProperty("exp", nowSec + 3600L)
            addProperty("iat", nowSec)
        }.toString()
        val claimsBase64 = encoder.encodeToString(claimsJson.toByteArray(StandardCharsets.UTF_8))

        // 3. Signature
        val dataToSign = "$headerBase64.$claimsBase64"
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(creds.privateKey)
            update(dataToSign.toByteArray(StandardCharsets.UTF_8))
        }.sign()
        val signatureBase64 = encoder.encodeToString(signature)

        return "$dataToSign.$signatureBase64"
    }

    /**
     * Parses a PEM-formatted RSA PKCS#8 private key.
     */
    private fun parsePrivateKey(pem: String): PrivateKey {
        val cleanKey = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")

        val keyBytes = Base64.getDecoder().decode(cleanKey)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(keySpec)
    }
}
