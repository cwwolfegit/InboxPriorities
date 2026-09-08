package com.example.inboxpriorities

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object MicrosoftAuthHelper {
    private const val CLIENT_ID = "6c306dd0-6ac1-4811-8af8-578861b2c5bb" // Replace with your Azure App Client ID
    private const val REDIRECT_URI = "inboxpriorities://oauth2redirect" // Match your Manifest intent filter

    // Generate a cryptographically secure random string for PKCE verifier
    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(32)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    // Hash the verifier with SHA-256 and encode it in Base64 URL-safe format for the challenge
    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    // Build the full authorization URL for Microsoft Entra ID
    fun createAuthorizationUrl(codeChallenge: String): String {
        val tenant = "common" // Or your specific tenant ID
        return "https://login.microsoftonline.com/$tenant/oauth2/v2.0/authorize?" +
                "client_id=$CLIENT_ID" +
                "&response_type=code" +
                "&redirect_uri=${java.net.URLEncoder.encode(REDIRECT_URI, "UTF-8")}" +
                "&response_mode=query" +
                "&scope=${java.net.URLEncoder.encode("https://graph.microsoft.com/Mail.Read offline_access", "UTF-8")}" +
                "&code_challenge=$codeChallenge" +
                "&code_challenge_method=S256"
    }
}