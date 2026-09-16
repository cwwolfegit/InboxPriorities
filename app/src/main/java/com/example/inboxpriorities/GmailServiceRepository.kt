package     com.chuckwolfe.inboxpriorities

import android.content.Context
import android.util.Log
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.google.gson.GsonBuilder


class GmailServiceRepository(
    private val context: Context,
    private val emailAddress: String,
    private val tokenProvider: suspend () -> String?
) : EmailRepository {

    override suspend fun fetchEmails(accountId: String): List<Email> {
        return fetchRecentEmails(50)
    }
    companion object {
        private const val TAG = "Debug_GmailRepo"
    }

    private suspend fun getGmailService(): Gmail? {
        val currentToken = tokenProvider()
        if (currentToken.isNullOrEmpty()) {
            Log.e(TAG, "Token provider returned a null or empty token for $emailAddress")
            return null
        }

        val credential = Credential(
            BearerToken.authorizationHeaderAccessMethod()
        ).setAccessToken(currentToken)

        return Gmail.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Inbox Priorities")
            .build()
    }

    suspend fun fetchRecentEmails(maxResults: Long = 10): List<Email> = withContext(Dispatchers.IO) {
        val emailList = mutableListOf<Email>()
        Log.d(TAG, "fetchRecentEmails called for $emailAddress")

        val service = getGmailService() ?: return@withContext emptyList()

        try {
            val response = service.users().messages().list("me")
                .setQ("label:unread category:primary")
                .setMaxResults(maxResults)
                .execute()

            val messages = response.messages ?: emptyList()
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonPayload = gson.toJson(response)

            Log.d(TAG, "Fetched message list size: ${messages.size}")

            for (messageItem in messages) {
                val fullMessage = service.users().messages()
                    .get("me", messageItem.id)
                    .setFormat("metadata")
                    .setMetadataHeaders(listOf("From", "Subject", "Date"))
                    .execute()

                val headers = fullMessage.payload?.headers ?: emptyList()
                val sender = headers.firstOrNull { it.name.equals("From", ignoreCase = true) }?.value ?: "Unknown Sender"
                val subject = headers.firstOrNull { it.name.equals("Subject", ignoreCase = true) }?.value ?: "No Subject"
                val snippet = fullMessage.snippet ?: ""
                val internalDate = fullMessage.internalDate ?: 0L

                emailList.add(
                    Email(
                        id = fullMessage.id,
                        sender = sender,
                        subject = subject,
                        body = snippet,
                        recipientAccount = emailAddress,
                        timestamp = internalDate
                    )
                )
            }
            Log.d(TAG, "Successfully parsed ${emailList.size} emails.")
        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401) {
                Log.e(TAG, "Authentication expired (401) for $emailAddress in fetchRecentEmails")
                // Handle 401 specifically (e.g., return empty or a distinct error marker)
            } else {
                Log.e(TAG, "Google JSON exception in fetchRecentEmails for $emailAddress: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in fetchRecentEmails for $emailAddress: ${e.message}", e)
            e.printStackTrace()
        }

        return@withContext emailList
    }

    override suspend fun getUnreadInboxCount(): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getUnreadInboxCount called for $emailAddress")
        val service = getGmailService() ?: return@withContext Pair(0, false)

        try {
            val response = service.users().threads().list("me")
                .setQ("label:unread category:primary")
                .setMaxResults(200)
                .execute()

            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonPayload = gson.toJson(response)
            Log.d("API_RESPONSE", "Thread List Payload:\n$jsonPayload")

            val messages = response.threads ?: emptyList()
            val hasNextPage = !response.nextPageToken.isNullOrEmpty()
            val resultEstimate = response.resultSizeEstimate

            Log.d(TAG, "Threads response - resultSizeEstimate: $resultEstimate, threads size: ${messages.size}, hasNextPage: $hasNextPage")

            if (hasNextPage) {
                return@withContext Pair(200, true)
            }

            val count = resultEstimate?.toInt() ?: messages.size
            Log.d(TAG, "Calculated unread count for $emailAddress: $count")
            return@withContext Pair(count, false)
        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401) {
                Log.e(TAG, "Authentication expired (401) for $emailAddress")
                // You can return a special indicator or throw a custom AuthException
                // so your ViewModel knows to display a "Sign-in expired" warning.
                return@withContext Pair(-1, false) // Using -1 as an error flag for count
            } else {
                Log.e(TAG, "Google JSON exception in getUnreadInboxCount for $emailAddress: ${e.message}", e)
                return@withContext Pair(0, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getUnreadInboxCount for $emailAddress: ${e.message}", e)
            e.printStackTrace()
            return@withContext Pair(0, false)
        }
    }

    override suspend fun getUnreadTodayCount(): Int = withContext(Dispatchers.IO) {
        Log.d(TAG, "getUnreadTodayCount called for $emailAddress")
        val service = getGmailService() ?: return@withContext 0

        try {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))

            val response = service.users().threads().list("me")
                .setQ("label:unread category:primary after:$todayStr")
                .execute()

            val count = response.resultSizeEstimate?.toInt() ?: (response.threads?.size ?: 0)
            Log.d(TAG, "Calculated unread today count for $emailAddress: $count")
            count
        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401) {
                Log.e(TAG, "Authentication expired (401) for $emailAddress in getUnreadTodayCount")
                return@withContext -1
            } else {
                Log.e(TAG, "Google JSON exception in getUnreadTodayCount for $emailAddress: ${e.message}", e)
                return@withContext 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getUnreadTodayCount for $emailAddress: ${e.message}", e)
            e.printStackTrace()
            return@withContext 0
        }
    }
}