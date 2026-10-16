package     com.polotech.inboxpriorities.diagnostics

import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.GsonBuilder

class GmailDiagnostic {

    companion object {
        private const val TAG = "DEBUG_GmailDiagnostic"
        private const val TARGET_EMAIL = "cwwfun123@gmail.com"
    }

    suspend fun runDiagnostic(liveAccessToken: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "GMAIL_DIAG: Starting Gmail API diagnostic for $TARGET_EMAIL...")

                if (liveAccessToken.isBlank()) {
                    Log.e(TAG, "GMAIL_DIAG: Diagnostic failed: Provided access token is empty.")
                    return@withContext
                }

                // Use a clean HttpRequestInitializer to inject the raw Bearer token directly
                val requestInitializer = HttpRequestInitializer { request ->
                    request.headers.authorization = "Bearer $liveAccessToken"
                }

                // Build the Gmail API client service without GoogleCredentials
                val transport = NetHttpTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()

                val gmailService = Gmail.Builder(
                    transport,
                    jsonFactory,
                    requestInitializer
                )
                    .setApplicationName("Inbox Priorities Diagnostic")
                    .build()

                // Execute a raw test query
                val userId = "me"
                val query = "label:unread category:primary"

                Log.d(TAG, "GMAIL_DIAG: Executing API request for user: $userId with query: '$query'")

                val response = gmailService.users().threads()
                    .list(userId)
                    .setQ(query)
                    .setMaxResults(10L)
                    .execute()

                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonPayload = gson.toJson(response)
                Log.d("API_RESPONSE", "Thread List Payload:\n$jsonPayload")

                val threads = response.threads
                val resultCount = threads?.size ?: 0

                Log.d(TAG, "GMAIL_DIAG: SUCCESS! Connected to Gmail API for $TARGET_EMAIL.")
                Log.d(TAG, "GMAIL_DIAG: Retrieved $resultCount unread primary threads.")

                if (threads != null) {
                    for ((index, thread) in threads.withIndex()) {
                        Log.d(TAG, "GMAIL_DIAG: Thread [${index + 1}] ID: ${thread.id} | Snippet: ${thread.snippet}")
                    }
                } else {
                    Log.d(TAG, "GMAIL_DIAG: No unread threads found matching the query.")
                }

            } catch (e: GoogleAuthIOException) {
                Log.e(TAG, "GMAIL_DIAG: Google Auth Error during diagnostic: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "GMAIL_DIAG: Unexpected error during Gmail diagnostic execution: ${e.message}", e)
            }
        }
    }
}