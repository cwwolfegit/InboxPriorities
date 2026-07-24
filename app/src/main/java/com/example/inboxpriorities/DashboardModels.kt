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
