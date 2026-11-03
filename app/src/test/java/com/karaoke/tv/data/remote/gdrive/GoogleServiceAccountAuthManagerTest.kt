package com.karaoke.tv.data.remote.gdrive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class GoogleServiceAccountAuthManagerTest {

    @Test
    fun testJwtCreationAndSignatureVerification() {
        // Generate an ephemeral 2048-bit RSA key pair for testing
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()

        val privateKeyBytes = keyPair.private.encoded
        val pemPrivateKey = buildString {
            append("-----BEGIN PRIVATE KEY-----\n")
            append(Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(privateKeyBytes))
            append("\n-----END PRIVATE KEY-----\n")
        }

        val jsonCredentials = """
            {
              "type": "service_account",
              "project_id": "test-project",
              "client_email": "test-account@test-project.iam.gserviceaccount.com",
              "private_key": "$pemPrivateKey"
            }
        """.trimIndent()

        val authManager = GoogleServiceAccountAuthManager()
        val creds = authManager.loadCredentialsFromJson(jsonCredentials)

        assertEquals("test-account@test-project.iam.gserviceaccount.com", creds.clientEmail)
        assertEquals("test-project", creds.projectId)
        assertNotNull(creds.privateKey)

        val nowSec = 1700000000L
        val jwt = authManager.createSignedJwt(creds, nowSec)

        // Verify JWT format: header.payload.signature
        val parts = jwt.split(".")
        assertEquals(3, parts.size)

        val headerJson = String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)
        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
        val signatureBytes = Base64.getUrlDecoder().decode(parts[2])

        assertTrue(headerJson.contains("\"alg\":\"RS256\""))
        assertTrue(payloadJson.contains("test-account@test-project.iam.gserviceaccount.com"))
        assertTrue(payloadJson.contains("drive.readonly"))

        // Verify cryptographic signature using public key
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(keyPair.public)
        val signedData = "${parts[0]}.${parts[1]}".toByteArray(StandardCharsets.UTF_8)
        verifier.update(signedData)

        assertTrue("JWT signature must be mathematically valid for the public key", verifier.verify(signatureBytes))
    }
}
