package com.example.inboxpriorities

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
            email = "IBXPTest1@gmail.com",
            provider = "Gmail",
            unreadTodayCount = 4,
            unreadTotalCount = 68,
            priorityUnreadTodayCount = 1,
            priorityUnreadTotalCount = 15,
            emails = listOf(
                MockEmail(
                    id = "1",
                    account = "IBXPTest1@gmail.com",
                    sender = "billing@subscriptions.com",
                    subject = "Your monthly statement is ready for review",
                    snippet = "Your invoice #98231 for $49.99 has been successfully processed...",
                    timestamp = "9:15 AM",
                    isPriority = true
                ),
                MockEmail(
                    id = "2",
                    account = "IBXPTest1@gmail.com",
                    sender = "newsletter@dailyread.com",
                    subject = "Morning Digest: Top 10 Tech Trends Today",
                    snippet = "Catch up on the latest developments in mobile architecture...",
                    timestamp = "8:00 AM",
                    isPriority = false
                )
            )
        ),
        AccountDashboardData(
            email = "IBXPTest2@gmail.com",
            provider = "Gmail",
            unreadTodayCount = 2,
            unreadTotalCount = 45,
            priorityUnreadTodayCount = 0,
            priorityUnreadTotalCount = 8,
            emails = listOf(
                MockEmail(
                    id = "3",
                    account = "IBXPTest2@gmail.com",
                    sender = "updates@cloud service.io",
                    subject = "Scheduled maintenance window notification",
                    snippet = "We will be performing routine infrastructure upgrades...",
                    timestamp = "Yesterday",
                    isPriority = false
                )
            )
        ),
        AccountDashboardData(
            email = "IBXPTest3@outlook.com",
            provider = "Outlook",
            unreadTodayCount = 6,
            unreadTotalCount = 88,
            priorityUnreadTodayCount = 2,
            priorityUnreadTotalCount = 27,
            emails = listOf(
                MockEmail(
                    id = "4",
                    account = "IBXPTest3@outlook.com",
                    sender = "alert@servermonitor.net",
                    subject = "CRITICAL: High CPU utilization on Prod-01",
                    snippet = "Server CPU usage has exceeded 95% threshold for over 10 minutes...",
                    timestamp = "10:42 AM",
                    isPriority = true
                )
            )
        ),
        /*AccountDashboardData(
            email = "IBXPTest4@outlook.com",
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