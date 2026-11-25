package com.polotech.inboxpriorities

import android.content.Context
import android.util.Log
import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

open class AuthExpiredException(message: String, cause: Throwable? = null) : Exception(message, cause)

class AccountAuthException(
    val title: String,
    val reason: String,
    val howToFix: String,
    val technicalDetail: String? = null,
    cause: Throwable? = null
) : AuthExpiredException("$title: $reason", cause)

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

    private fun captureTechnicalDetail(operation: String, e: Throwable): String {
        val type = e::class.java.simpleName.ifBlank { "Exception" }
        val msg = e.message ?: e.localizedMessage ?: "No message"
        val causeInfo = e.cause?.let { c ->
            " | Cause: ${c::class.java.simpleName}: ${c.message ?: "No message"}"
        }.orEmpty()
        val detail = "$operation -> $type: $msg$causeInfo"
        DiagnosticLogger.record(
            account = emailAddress,
            operation = operation,
            errorType = type,
            message = "$msg$causeInfo"
        )
        return detail
    }

    private fun classifyHttpAuthException(e: GoogleJsonResponseException, operation: String): AuthExpiredException {
        val details = e.details?.message ?: e.message.orEmpty()
        return when (e.statusCode) {
            401 -> AuthExpiredException(
                message = "Your sign-in session for $emailAddress expired ($operation).",
                cause = e
            )
            403 -> {
                val detail = captureTechnicalDetail(operation, e)
                AccountAuthException(
                    title = "OAuth Permission Denied",
                    reason = if (details.isNotBlank()) {
                        "Google blocked Gmail access for $emailAddress: $details"
                    } else {
                        "Gmail read permission was not granted, was revoked, or OAuth access is restricted for $emailAddress."
                    },
                    howToFix = "Reconnect $emailAddress and approve Gmail read access. If testing, ensure this account is added as a test user and the Play Store SHA-1 is registered in Google Cloud Console.",
                    technicalDetail = detail,
                    cause = e
                )
            }
            else -> {
                val detail = captureTechnicalDetail(operation, e)
                AccountAuthException(
                    title = "Gmail API Error (${e.statusCode})",
                    reason = details.ifBlank { "Google rejected the Gmail request for $emailAddress (HTTP ${e.statusCode})." },
                    howToFix = "Check your account connection and try reconnecting $emailAddress.",
                    technicalDetail = detail,
                    cause = e
                )
            }
        }
    }

    private fun missingTokenException(operation: String): AccountAuthException {
        return AccountAuthException(
            title = "Not Signed In",
            reason = "No active sign-in token is available for $emailAddress ($operation).",
            howToFix = "Sign in to $emailAddress so Inbox Priorities can access your inbox."
        )
    }

    private suspend fun getGmailService(operation: String): Gmail {
        val currentToken = tokenProvider()
        if (currentToken.isNullOrBlank()) {
            Log.e(TAG, "Token provider returned a null or empty token for $emailAddress in $operation")
            throw missingTokenException(operation)
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

    suspend fun fetchContacts(): List<String> = withContext(Dispatchers.IO) {
        val contactEmails = mutableListOf<String>()
        Log.d(TAG, "fetchContacts called for $emailAddress")

        val currentToken = tokenProvider()
        if (currentToken.isNullOrEmpty()) {
            Log.e(TAG, "Token provider returned a null or empty token for $emailAddress")
            return@withContext emptyList()
        }

        val credential = Credential(
            BearerToken.authorizationHeaderAccessMethod()
        ).setAccessToken(currentToken)

        try {
            val requestFactory = NetHttpTransport().createRequestFactory(credential)
            var pageToken: String? = null

            do {
                var urlStr = "https://people.googleapis.com/v1/people/me/connections?personFields=emailAddresses&pageSize=100"
                if (!pageToken.isNullOrEmpty()) {
                    urlStr += "&pageToken=$pageToken"
                }

                val request = requestFactory.buildGetRequest(com.google.api.client.http.GenericUrl(urlStr))
                val jsonString = request.execute().parseAsString()

                val gson = com.google.gson.Gson()
                val map = gson.fromJson(jsonString, Map::class.java)

                @Suppress("UNCHECKED_CAST")
                val connections = map["connections"] as? List<Map<String, Any>> ?: emptyList()

                for (person in connections) {
                    @Suppress("UNCHECKED_CAST")
                    val emails = person["emailAddresses"] as? List<Map<String, Any>> ?: continue
                    for (emailObj in emails) {
                        val email = emailObj["value"] as? String
                        email?.let {
                            contactEmails.add(it.trim().lowercase())
                        }
                    }
                }

                pageToken = map["nextPageToken"] as? String
            } while (!pageToken.isNullOrEmpty())

            Log.d(TAG, "Successfully fetched ${contactEmails.size} contact emails.")
        } catch (e: Exception) {
            Log.e(TAG, "Exception in fetchContacts for $emailAddress: ${e.message}", e)
        }

        return@withContext contactEmails
    }

    suspend fun fetchRecentEmails(maxResults: Long = 10): List<Email> = withContext(Dispatchers.IO) {
        val emailList = mutableListOf<Email>()
        Log.d(TAG, "fetchRecentEmails called for $emailAddress")

        val service = getGmailService("fetchRecentEmails")

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
        } catch (e: AccountAuthException) {
            throw e
        } catch (e: GoogleJsonResponseException) {
            Log.e(TAG, "Google JSON exception (${e.statusCode}) in fetchRecentEmails for $emailAddress: ${e.message}", e)
            throw classifyHttpAuthException(e, "loading recent emails")
        } catch (e: Exception) {
            Log.e(TAG, "Exception in fetchRecentEmails for $emailAddress: ${e.message}", e)
            val detail = captureTechnicalDetail("fetchRecentEmails", e)
            throw AccountAuthException(
                title = "Gmail Sync Error",
                reason = e.localizedMessage ?: "Failed to load recent emails for $emailAddress.",
                howToFix = "Check your network connection and reconnect $emailAddress if the problem persists.",
                technicalDetail = detail,
                cause = e
            )
        }

        return@withContext emailList
    }

    override suspend fun getUnreadInboxCount(): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getUnreadInboxCount called for $emailAddress")
        val service = getGmailService("getUnreadInboxCount")

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
        } catch (e: AccountAuthException) {
            throw e
        } catch (e: GoogleJsonResponseException) {
            Log.e(TAG, "Google JSON exception (${e.statusCode}) in getUnreadInboxCount for $emailAddress: ${e.message}", e)
            throw classifyHttpAuthException(e, "checking unread inbox count")
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getUnreadInboxCount for $emailAddress: ${e.message}", e)
            val detail = captureTechnicalDetail("getUnreadInboxCount", e)
            throw AccountAuthException(
                title = "Gmail Sync Error",
                reason = e.localizedMessage ?: "Unable to fetch unread inbox count for $emailAddress.",
                howToFix = "Check your network connection and reconnect $emailAddress if the problem persists.",
                technicalDetail = detail,
                cause = e
            )
        }
    }

    override suspend fun getUnreadTodayCount(): Int = withContext(Dispatchers.IO) {
        Log.d(TAG, "getUnreadTodayCount called for $emailAddress")
        val service = getGmailService("getUnreadTodayCount")

        try {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))

            val response = service.users().threads().list("me")
                .setQ("label:unread category:primary after:$todayStr")
                .execute()

            val count = response.resultSizeEstimate?.toInt() ?: (response.threads?.size ?: 0)
            Log.d(TAG, "Calculated unread today count for $emailAddress: $count")
            count
        } catch (e: AccountAuthException) {
            throw e
        } catch (e: GoogleJsonResponseException) {
            Log.e(TAG, "Google JSON exception (${e.statusCode}) in getUnreadTodayCount for $emailAddress: ${e.message}", e)
            throw classifyHttpAuthException(e, "checking today's unread count")
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getUnreadTodayCount for $emailAddress: ${e.message}", e)
            val detail = captureTechnicalDetail("getUnreadTodayCount", e)
            throw AccountAuthException(
                title = "Gmail Sync Error",
                reason = e.localizedMessage ?: "Unable to fetch today's unread count for $emailAddress.",
                howToFix = "Check your network connection and reconnect $emailAddress if the problem persists.",
                technicalDetail = detail,
                cause = e
            )
        }
    }
}