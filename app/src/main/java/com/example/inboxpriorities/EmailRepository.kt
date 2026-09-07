package com.example.inboxpriorities

interface EmailRepository {
    suspend fun fetchEmails(accountId: String): List<Email>
}