package com.example.inboxpriorities

import java.util.UUID

enum class AccountProvider {
    GMAIL,
    OUTLOOK,
    OTHER
}

data class EmailAccount(
    val id: String = UUID.randomUUID().toString(),
    val emailAddress: String,
    val provider: AccountProvider = AccountProvider.GMAIL,
    val isConnected: Boolean = true,
    val unreadToday: Int = 0,
    val unreadTotal: Int = 0,
    val priorityToday: Int = 0,
    val priorityTotal: Int = 0

)
