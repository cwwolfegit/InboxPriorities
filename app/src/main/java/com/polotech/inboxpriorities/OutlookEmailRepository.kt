package com.polotech.inboxpriorities

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant

class OutlookEmailRepository(
    private val emailAddress: String,
    private val tokenProvider: suspend () -> String?
) : EmailRepository {

    companion object {
        private const val TAG = "Debug_OutlookRepo"

        // Requesting standard fields matching Microsoft Graph API schema
        private const val GRAPH_API_URL =
            "https://graph.microsoft.com/v1.0/me/mailFolders/inbox/messages?\$filter=isRead eq false&\$select=id,subject,bodyPreview,from,receivedDateTime&\$top=50"
    }

    private val client = OkHttpClient()

    private fun captureTechnicalDetail(
        operation: String,
        detailMessage: String,
        e: Throwable? = null
    ): String {
        val type = e?.let { it::class.java.simpleName.ifBlank { "Exception" } } ?: "GraphApiError"
        val causeInfo = e?.cause?.let { c ->
            " | Cause: ${c::class.java.simpleName}:${c.message ?: "No message"}"
        }.orEmpty()
        val detail = "$operation ->$type: $detailMessage$causeInfo"
        DiagnosticLogger.record(
            account = emailAddress,
            operation = operation,
            errorType = type,
            message = "$detailMessage$causeInfo"
        )
        return detail
    }

    private fun classifyGraphError(
        operation: String,
        statusCode: Int,
        responseMessage: String,
        responseBody: String?
    ): AuthExpiredException {
        if (statusCode == 401) {
            return AuthExpiredException(
                message = "Your sign-in session for $emailAddress expired ($operation)."
            )
        }

        val summary = buildString {
            append("HTTP $statusCode")
            if (responseMessage.isNotBlank()) append(" ($responseMessage)")
            if (!responseBody.isNullOrBlank()) append(": ${responseBody.take(300)}")
        }
        val detail = captureTechnicalDetail(operation, summary)

        return if (statusCode == 403) {
            AccountAuthException(
                title = "Outlook Permission Denied",
                reason = "Microsoft blocked Outlook mail access for $emailAddress (HTTP 403).",
                howToFix = "Reconnect $emailAddress and approve Mail.Read permission.",
                technicalDetail = detail
            )
        } else {
            AccountAuthException(
                title = "Outlook API Error ($statusCode)",
                reason = "Microsoft Graph rejected the Outlook request for $emailAddress (HTTP$statusCode).",
                howToFix = "Check your account connection and try reconnecting $emailAddress.",
                technicalDetail = detail
            )
        }
    }

    override suspend fun fetchEmails(accountId: String): List<Email> = withContext(Dispatchers.IO) {
        val emailList = mutableListOf<Email>()
        val token = tokenProvider()

        if (token.isNullOrEmpty()) {
            Log.e(
                TAG,
                "Token provider returned a null or empty token for Outlook account: $emailAddress"
            )
            throw AuthExpiredException("No active Outlook sign-in token for $emailAddress.")
        }

        try {
            val request = Request.Builder()
                .url(GRAPH_API_URL)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()
                    Log.e(
                        TAG,
                        "Microsoft Graph API error: ${response.code} - ${response.message} for$emailAddress"
                    )
                    throw classifyGraphError(
                        "fetchEmails",
                        response.code,
                        response.message,
                        errBody
                    )
                }

                val responseBody = response.body?.string() ?: return@withContext emptyList()
                val gson = GsonBuilder().create()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val valueArray =
                    jsonObject.getAsJsonArray("value") ?: return@withContext emptyList()

                for (item in valueArray) {
                    val messageObj = item.asJsonObject
                    val id = messageObj.get("id")?.asString ?: ""
                    val subject = messageObj.get("subject")?.asString ?: "No Subject"
                    val bodyPreview = messageObj.get("bodyPreview")?.asString ?: ""

                    // Microsoft Graph structures sender information under from -> emailAddress -> name/address
                    val fromObj = messageObj.getAsJsonObject("from")
                    val emailAddressObj = fromObj?.getAsJsonObject("emailAddress")
                    val senderName = emailAddressObj?.get("name")?.asString ?: ""
                    val senderAddress = emailAddressObj?.get("address")?.asString ?: ""

                    // Format as "Name <email@domain.com>" or fallback to address so substring rules match correctly
                    val sender = if (senderAddress.isNotEmpty()) {
                        if (senderName.isNotEmpty()) "$senderName <$senderAddress>" else senderAddress
                    } else {
                        senderName.ifEmpty { "Unknown Sender" }
                    }

                    val receivedDateTime = messageObj.get("receivedDateTime")?.asString
                    val timestamp = parseIsoTimestamp(receivedDateTime)

                    emailList.add(
                        Email(
                            id = id,
                            sender = sender,
                            subject = subject,
                            body = bodyPreview,
                            recipientAccount = emailAddress,
                            timestamp = timestamp
                        )
                    )
                }
                Log.d(TAG, "Successfully parsed ${emailList.size} Outlook emails for$emailAddress.")
            }
        } catch (e: AuthExpiredException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception in fetchEmails for Outlook account $emailAddress:${e.message}", e)
            val detail = captureTechnicalDetail("fetchEmails", e.message ?: "Unexpected error", e)
            throw AccountAuthException(
                title = "Outlook Sync Error",
                reason = e.localizedMessage ?: "Failed to load recent emails for $emailAddress.",
                howToFix = "Check your network connection and reconnect $emailAddress if the problem persists.",
                technicalDetail = detail,
                cause = e
            )
        }

        return@withContext emailList
    }

    override suspend fun getUnreadInboxCount(): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        val token = tokenProvider()
        if (token.isNullOrEmpty()) {
            throw AuthExpiredException("No active Outlook sign-in token for $emailAddress.")
        }

        try {
            val url =
                "https://graph.microsoft.com/v1.0/me/mailFolders/inbox/messages?\$filter=isRead eq false&\$count=true&\$top=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("ConsistencyLevel", "eventual")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()
                    Log.e(
                        TAG,
                        "Microsoft Graph unread count error: ${response.code} - ${response.message} for$emailAddress"
                    )
                    throw classifyGraphError(
                        "getUnreadInboxCount",
                        response.code,
                        response.message,
                        errBody
                    )
                }
                val bodyString = response.body?.string() ?: return@withContext Pair(0, false)

                val gson = GsonBuilder().create()
                val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
                val totalCount =
                    if (jsonObject.has("@odata.count")) jsonObject.get("@odata.count").asInt else 0

                // Only show '+' if it exceeds a large threshold (e.g., 200, matching Gmail behavior)
                val hasMore = totalCount > 200

                Pair(totalCount, hasMore)
            }
        } catch (e: AuthExpiredException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getUnreadInboxCount for $emailAddress:${e.message}", e)
            val detail =
                captureTechnicalDetail("getUnreadInboxCount", e.message ?: "Unexpected error", e)
            throw AccountAuthException(
                title = "Outlook Sync Error",
                reason = e.localizedMessage
                    ?: "Unable to fetch unread inbox count for $emailAddress.",
                howToFix = "Check your network connection and reconnect $emailAddress if the problem persists.",
                technicalDetail = detail,
                cause = e
            )
        }
    }

    override suspend fun getUnreadTodayCount(): Int = withContext(Dispatchers.IO) {
        val token = tokenProvider()
        if (token.isNullOrEmpty()) {
            throw AuthExpiredException("No active Outlook sign-in token for $emailAddress.")
        }

        try {
            val calendar =
                java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
            val isoToday = Instant.ofEpochMilli(calendar.timeInMillis).toString()

            val url =
                "https://graph.microsoft.com/v1.0/me/mailFolders/inbox/messages?\$filter=isRead eq false and receivedDateTime ge $isoToday&\$count=true"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("ConsistencyLevel", "eventual")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()
                    Log.e(
                        TAG,
                        "Microsoft Graph unread today error: ${response.code} - ${response.message} for$emailAddress"
                    )
                    throw classifyGraphError(
                        "getUnreadTodayCount",
                        response.code,
                        response.message,
                        errBody
                    )
                }
                val bodyString = response.body?.string() ?: return@withContext 0

                val gson = GsonBuilder().create()
                val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
                if (jsonObject.has("@odata.count")) jsonObject.get("@odata.count").asInt else 0
            }
        } catch (e: AuthExpiredException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getUnreadTodayCount for $emailAddress:${e.message}", e)
            val detail =
                captureTechnicalDetail("getUnreadTodayCount", e.message ?: "Unexpected error", e)
            throw AccountAuthException(
                title = "Outlook Sync Error",
                reason = e.localizedMessage
                    ?: "Unable to fetch today's unread count for $emailAddress.",
                howToFix = "Check your network connection and reconnect $emailAddress if the problem persists.",
                technicalDetail = detail,
                cause = e
            )
        }
    }

    private fun parseIsoTimestamp(isoStr: String?): Long {
        if (isoStr.isNullOrEmpty()) return 0L
        return try {
            Instant.parse(isoStr).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}