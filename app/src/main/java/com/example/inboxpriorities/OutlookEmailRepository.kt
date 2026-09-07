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
                    val senderName = emailAddressObj?.get("name")?.asString
                        ?: emailAddressObj?.get("address")?.asString
                        ?: "Unknown Sender"

                    val receivedDateTime = messageObj.get("receivedDateTime")?.asString
                    val timestamp = parseIsoTimestamp(receivedDateTime)

                    emailList.add(
                        Email(
                            id = id,
                            sender = senderName,
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

    private fun parseIsoTimestamp(isoStr: String?): Long {
        if (isoStr.isNullOrEmpty()) return 0L
        return try {
            Instant.parse(isoStr).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}