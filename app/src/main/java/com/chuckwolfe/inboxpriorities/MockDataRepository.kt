package     com.chuckwolfe.inboxpriorities

data class AccountDashboardData(
    val email: String,
    val provider: String,
    val unreadTodayCount: Int,
    val unreadTotalCount: Int,
    val priorityUnreadTodayCount: Int,
    val priorityUnreadTotalCount: Int,
    val emails: List<MockEmail>
)

object MockDataRepository {
    val accountDashboards = listOf(
        AccountDashboardData(
            email = "NBXP1@gmail.com",
            provider = "Gmail",
            unreadTodayCount = 4,
            unreadTotalCount = 68,
            priorityUnreadTodayCount = 1,
            priorityUnreadTotalCount = 1,
            emails = listOf(
                MockEmail(
                    id = "1",
                    account = "NBXP1@gmail.com",
                    sender = "billing@subscriptions.com",
                    subject = "Your monthly invoice is ready for review",
                    snippet = "Your invoice #98231 for $49.99 has been successfully processed...",
                    timestamp = "9:15 AM",
                    isPriority = true
                ),
            )
        ),
        AccountDashboardData(
            email = "NBXP2@gmail.com",
            provider = "Gmail",
            unreadTodayCount = 2,
            unreadTotalCount = 45,
            priorityUnreadTodayCount = 0,
            priorityUnreadTotalCount = 1,
            emails = listOf(
                MockEmail(
                    id = "3",
                    account = "NBXP2@gmail.com",
                    sender = "updates@cloud service.io",
                    subject = "Scheduled updates maintenance window notification",
                    snippet = "We will be performing routine infrastructure upgrades...",
                    timestamp = "Yesterday",
                    isPriority = true
                )
            )
        ),
        AccountDashboardData(
            email = "NBXP3@outlook.com",
            provider = "Outlook",
            unreadTodayCount = 6,
            unreadTotalCount = 88,
            priorityUnreadTodayCount = 1,
            priorityUnreadTotalCount = 1,
            emails = listOf(
                MockEmail(
                    id = "4",
                    account = "NBXP3@outlook.com",
                    sender = "client@example.com",
                    subject = "CRITICAL: High CPU utilization on Prod-01",
                    snippet = "Server CPU usage has exceeded 95% threshold for over 10 minutes...",
                    timestamp = "10:42 AM",
                    isPriority = true
                )
            )
        ),
        /*AccountDashboardData(
            email = "NBXPTest4@outlook.com",
            provider = "Outlook",
            unreadTodayCount = 0,
            unreadTotalCount = 0,
            priorityUnreadTodayCount = 0,
            priorityUnreadTotalCount = 0,
            emails = emptyList()
        )*/
    )
}

data class MockEmail(
    val id: String,
    val account: String,
    val sender: String,
    val subject: String,
    val snippet: String,
    val timestamp: String,
    val isPriority: Boolean
)