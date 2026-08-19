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
            // Query exactly 200 unread threads; if there's a nextPageToken, it's > 200
            val response = gmailService.users().threads().list("me")
                .setQ("is:unread in:inbox")
                .setMaxResults(200)
                .execute()

            val count = response.threads?.size ?: 0
            val isExceeded = !response.nextPageToken.isNullOrEmpty()

            return@withContext Pair(count, isExceeded)
        } catch (e: Exception) {
            return@withContext Pair(0, false)
        }
    }
}