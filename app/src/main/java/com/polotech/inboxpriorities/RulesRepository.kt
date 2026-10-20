package com.polotech.inboxpriorities

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first

// Extension property to initialize DataStore instance
private val Context.dataStore by preferencesDataStore(name = "inbox_priorities_prefs")

class RulesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val RULES_KEY = stringPreferencesKey("saved_priority_rules")
    private val ACCOUNTS_KEY = stringPreferencesKey("saved_email_accounts")
    private val CONTACT_PRIORITY_KEY = stringPreferencesKey("contact_priority")

    // --- RULES ---

    // Continuous flow of rules from DataStore
    val rulesFlow: Flow<List<PriorityRule>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[RULES_KEY]
            if (!jsonString.isNullOrEmpty()) {
                try {
                    json.decodeFromString<List<PriorityRule>>(jsonString)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

    // Save full list of rules as a JSON string
    suspend fun saveRules(rules: List<PriorityRule>) {
        val jsonString = json.encodeToString(rules)
        context.dataStore.edit { preferences ->
            preferences[RULES_KEY] = jsonString
        }
    }

    // --- ACCOUNTS ---

    // Continuous flow of accounts from DataStore
    val accountsFlow: Flow<List<EmailAccount>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[ACCOUNTS_KEY]
            if (!jsonString.isNullOrEmpty()) {
                try {
                    json.decodeFromString<List<EmailAccount>>(jsonString)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

    // Save full list of accounts as a JSON string
    suspend fun saveAccounts(accounts: List<EmailAccount>) {
        val jsonString = json.encodeToString(accounts)
        context.dataStore.edit { preferences ->
            preferences[ACCOUNTS_KEY] = jsonString
        }
    }

    // --- CONTACT PRIORITY SETTING ---

    // Flow representing the current contact priority setting value (defaults to "NONE")
    val contactPriorityFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CONTACT_PRIORITY_KEY] ?: "NONE"
        }

    // Save the updated contact priority setting
    suspend fun saveContactPriority(priority: String) {
        context.dataStore.edit { preferences ->
            preferences[CONTACT_PRIORITY_KEY] = priority
        }
    }

    // Check if contact priority is active (i.e. not "NONE")
    suspend fun hasActiveContactPriority(): Boolean {
        val priority = contactPriorityFlow.first()
        return priority.uppercase() != "NONE"
    }

    // Default starter rules using named arguments to prevent positional mismatch
    suspend fun exchangeAuthorizationCode(authorizationCode: String): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                // Build form parameters
                val postData = buildString {
                    append("client_id=6c306dd0-6ac1-4811-8af8-578861b2c5bb")
                    append("&scope=https://graph.microsoft.com/Mail.Read offline_access")
                    append("&code=$authorizationCode")
                    append("&redirect_uri=msauth://com.polotech.inboxpriorities/T0f9SJ%2B5NRYcNFUbDsBC%2Fv0fUEk%3D")
                    append("&grant_type=authorization_code")

                    // Retrieve the code_verifier stored during the login request setup:
                    val codeVerifier = context.getSharedPreferences("AuthPrefs", android.content.Context.MODE_PRIVATE)
                        .getString("code_verifier", "")
                    if (!codeVerifier.isNullOrEmpty()) {
                        append("&code_verifier=$codeVerifier")
                    }
                }

                connection.outputStream.use { os ->
                    os.write(postData.toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }

                    // Parse JSON response and save tokens to AuthPrefs
                    val jsonResponse = org.json.JSONObject(responseString)
                    val accessToken = jsonResponse.getString("access_token")
                    val refreshToken = jsonResponse.optString("refresh_token")

                    val prefs = context.getSharedPreferences("AuthPrefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("microsoft_access_token", accessToken)
                        if (!refreshToken.isNullOrEmpty()) {
                            putString("microsoft_refresh_token", refreshToken)
                        }
                        apply()
                    }

                    true
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    android.util.Log.e("Repository", "Token exchange failed with code $responseCode: $errorStream")
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("Repository", "Exception during token exchange request", e)
                false
            }
        }
    }
}