package com.example.inboxpriorities

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GmailServiceRepository(private val context: Context, private val account: GoogleSignInAccount) {

    private val credential = GoogleAccountCredential.usingOAuth2(
        context, listOf(GmailScopes.GMAIL_READONLY)
    ).apply {
        selectedAccount = account.account
    }

    private val gmailService: Gmail = Gmail.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        credential
    )
        .setApplicationName("Inbox Priorities")
        .build()

    suspend fun fetchRecentEmails(maxResults: Long = 10): List<Email> = withContext(Dispatchers.IO) {
        val emailList = mutableListOf<Email>()

        try {
            // Step 1: Fetch list of message IDs from user's inbox
            val response = gmailService.users().messages().list("me")
                .setQ("label:INBOX")
                .setMaxResults(maxResults)
                .execute()

            val messages = response.messages ?: emptyList()

            // Step 2: Fetch full details for each message and parse headers
            for (messageItem in messages) {
                val fullMessage = gmailService.users().messages()
                    .get("me", messageItem.id)
                    .setFormat("full")
                    .execute()

                val headers = fullMessage.payload?.headers ?: emptyList()

                val sender = headers.firstOrNull { it.name.equals("From", ignoreCase = true) }?.value ?: "Unknown Sender"
                val subject = headers.firstOrNull { it.name.equals("Subject", ignoreCase = true) }?.value ?: "No Subject"
                val snippet = fullMessage.snippet ?: ""

                emailList.add(
                    Email(
                        id = fullMessage.id,
                        sender = sender,
                        subject = subject,
                        body = snippet,
                        recipientAccount = account.email ?: ""
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext emailList
    }

    suspend fun getUnreadInboxCount(): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        try {
            // Query unread messages in the primary category
            val response = gmailService.users().threads().list("me")
                .setQ("label:unread category:primary")
                .setMaxResults(200) // Fetch up to 200 to check for pagination
                .execute()

            val messages = response.threads ?: emptyList()
            val hasNextPage = !response.nextPageToken.isNullOrEmpty()

            // If there is a nextPageToken, it's 200+
            if (hasNextPage) {
                return@withContext Pair(200, true)
            }

            // Otherwise, use resultSizeEstimate if available, or fall back to the actual messages list size
            val count = response.resultSizeEstimate?.toInt() ?: messages.size

            return@withContext Pair(count, false)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Pair(0, false)
        }
    }
    suspend fun getUnreadTodayCount(): Int = withContext(Dispatchers.IO) {
        try {
            // Format today's date as YYYY/MM/DD (e.g., "2026/08/20")
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))

            // Query unread messages in the primary category received strictly from today onward
            val response = gmailService.users().threads().list("me")
                .setQ("label:unread category:primary after:$todayStr")
                .execute()

            response.resultSizeEstimate?.toInt() ?: (response.threads?.size ?: 0)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}