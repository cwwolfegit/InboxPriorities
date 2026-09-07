package com.example.inboxpriorities

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.time.Instant

class OutlookEmailRepository(
    private val emailAddress: String,
    private val tokenProvider: suspend () -> String?
) : EmailRepository {

    companion object {
        private const val TAG = "Debug_OutlookRepo"
        // Requesting standard fields matching Microsoft Graph API schema
        private const val GRAPH_API_URL = "https://graph.microsoft.com/v1.0/me/messages?\$select=id,subject,bodyPreview,from,receivedDateTime&\$top=10"
    }

    private val client = OkHttpClient()

    override suspend fun fetchEmails(accountId: String): List<Email> = withContext(Dispatchers.IO) {
        val emailList = mutableListOf<Email>()
        val token = tokenProvider()

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Token provider returned a null or empty token for Outlook account: $emailAddress")
            return@withContext emptyList()
        }

        try {
            val request = Request.Builder()
                .url(GRAPH_API_URL)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Microsoft Graph API error: ${response.code} - ${response.message} for $emailAddress")
                    return@withContext emptyList()
                }

                val responseBody = response.body?.string() ?: return@withContext emptyList()
                val gson = GsonBuilder().create()
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val valueArray = jsonObject.getAsJsonArray("value") ?: return@withContext emptyList()

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

                    Log.d(TAG, "FromObj: $fromObj.")
                    Log.d(TAG, "emailAddressObj: $emailAddressObj.")
                    Log.d(TAG, "SenderName: $senderName.")
                    Log.d(TAG, "senderAddress: $senderAddress.")
                    Log.d(TAG, "Subject: $subject.")

                    // Format as "Name <email@domain.com>" or fallback to address so substring rules match correctly
                    val sender = if (senderAddress.isNotEmpty()) {
                        if (senderName.isNotEmpty()) "$senderName <$senderAddress>" else senderAddress
                    } else {
                        senderName.ifEmpty { "Unknown Sender" }
                    }

                    Log.d(TAG, "Final Sender: $sender.")

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
                Log.d(TAG, "Successfully parsed ${emailList.size} Outlook emails for $emailAddress.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in fetchEmails for Outlook account $emailAddress: ${e.message}", e)
            e.printStackTrace()
        }

        return@withContext emailList
    }

    override suspend fun getUnreadInboxCount(): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        val token = tokenProvider()
        if (token.isNullOrEmpty()) return@withContext Pair(0, false)

        try {
            val url = "https://graph.microsoft.com/v1.0/me/mailFolders/inbox/messages?\$filter=isRead eq false&\$count=true&\$top=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("ConsistencyLevel", "eventual")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Pair(0, false)
                val bodyString = response.body?.string() ?: return@withContext Pair(0, false)

                val gson = GsonBuilder().create()
                val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
                val totalCount = if (jsonObject.has("@odata.count")) jsonObject.get("@odata.count").asInt else 0
                val hasMore = jsonObject.has("@odata.nextLink")

                Pair(totalCount, hasMore)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getUnreadInboxCount for $emailAddress: ${e.message}", e)
            Pair(0, false)
        }
    }

    override suspend fun getUnreadTodayCount(): Int = withContext(Dispatchers.IO) {
        val token = tokenProvider()
        if (token.isNullOrEmpty()) return@withContext 0

        try {
            val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val isoToday = Instant.ofEpochMilli(calendar.timeInMillis).toString()

            val url = "https://graph.microsoft.com/v1.0/me/mailFolders/inbox/messages?\$filter=isRead eq false and receivedDateTime ge $isoToday&\$count=true"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("ConsistencyLevel", "eventual")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext 0
                val bodyString = response.body?.string() ?: return@withContext 0

                val gson = GsonBuilder().create()
                val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
                if (jsonObject.has("@odata.count")) jsonObject.get("@odata.count").asInt else 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getUnreadTodayCount for $emailAddress: ${e.message}", e)
            0
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