package com.example.inboxpriorities

import android.util.Log

class OutlookEmailRepository : EmailRepository {
    override suspend fun fetchEmails(accountId: String): List<Email> {
        // TODO: Implement Microsoft Graph REST API call using accountId
        // 1. Retrieve OAuth2 token for the Outlook account
        // 2. Make HTTP request to https://graph.microsoft.com/v1.0/me/messages
        // 3. Map Microsoft Graph JSON message payload to your common Email data model

        Log.d("OutlookEmailRepository", "Fetching emails for Outlook account: $accountId")
        return emptyList()
    }
}