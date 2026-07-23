package com.example.inboxpriorities

// Overall high-level summary (Top Card)
data class DashboardSummary(
    val unreadPriorityToday: Int,
    val unreadPriorityTotal: Int,
    val unreadToday: Int,
    val unreadTotal: Int
)

// Per-account summary (Middle Section)
data class AccountSummary(
    val id: String,
    val accountName: String,
    val unreadPriorityToday: Int,
    val unreadPriorityTotal: Int,
    val unreadToday: Int,
    val unreadTotal: Int
)

// Sender drill-down data (Bottom Section / Level 3)
data class PrioritySender(
    val senderAddress: String,
    val unreadCount: Int
)

// Object supplying fake/mock data for Phase 1 UI
object MockDataRepository {
    val mockSummary = DashboardSummary(
        unreadPriorityToday = 5,
        unreadPriorityTotal = 13,
        unreadToday = 16,
        unreadTotal = 183
    )

    val mockAccounts = listOf(
        AccountSummary(
            id = "1",
            accountName = "Personal Gmail",
            unreadPriorityToday = 3,
            unreadPriorityTotal = 8,
            unreadToday = 12,
            unreadTotal = 145
        ),
        AccountSummary(
            id = "2",
            accountName = "Work Gmail",
            unreadPriorityToday = 2,
            unreadPriorityTotal = 5,
            unreadToday = 4,
            unreadTotal = 38
        )
    )

    val mockSenders = listOf(
        PrioritySender("boss@company.com", 1),
        PrioritySender("customer1.com", 2),
        PrioritySender("amazon.com", 5)
    )
}