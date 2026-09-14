package     com.chuckwolfe.inboxpriorities

interface EmailRepository {
    suspend fun fetchEmails(accountId: String): List<Email>
    suspend fun getUnreadInboxCount(): Pair<Int, Boolean>
    suspend fun getUnreadTodayCount(): Int
}