package com.example.inboxpriorities

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import java.security.MessageDigest
import java.security.SecureRandom
import java.net.URLEncoder

object MicrosoftAuthHelper {
    private const val CLIENT_ID = "6c306dd0-6ac1-4811-8af8-578861b2c5bb"
    private const val REDIRECT_URI = "inboxpriorities://oauth2redirect"

    private var currentCodeVerifier: String? = null

    // Generate a cryptographically secure random string for PKCE verifier
    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(32)
        secureRandom.nextBytes(code)
        val verifier = Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        currentCodeVerifier = verifier
        return verifier
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
        val tenant = "common"
        return "https://login.microsoftonline.com/$tenant/oauth2/v2.0/authorize?" +
                "client_id=$CLIENT_ID" +
                "&response_type=code" +
                "&redirect_uri=${URLEncoder.encode(REDIRECT_URI, "UTF-8")}" +
                "&response_mode=query" +
                "&scope=${URLEncoder.encode("https://graph.microsoft.com/Mail.Read offline_access", "UTF-8")}" +
                "&code_challenge=$codeChallenge" +
                "&code_challenge_method=S256"
    }

    // Convenience method to initiate the Chrome Custom Tabs login flow
    fun startLogin(context: Context) {
        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)
        val authUrlString = createAuthorizationUrl(challenge)

        val uri = Uri.parse(authUrlString)
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, uri)
    }

    // Retrieve the active verifier needed during the token exchange step
    fun getSavedVerifier(): String? = currentCodeVerifier
}