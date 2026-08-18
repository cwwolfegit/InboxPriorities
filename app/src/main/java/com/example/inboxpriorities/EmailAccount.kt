package com.example.inboxpriorities

import kotlinx.serialization.Serializable
import java.util.UUID

enum class AccountProvider {
    GMAIL,
    OUTLOOK,
    OTHER
}
@Serializable
data class EmailAccount(
    val id: String = UUID.randomUUID().toString(),
    val emailAddress: String,
    val nickname: String = "", // e.g., "Work", "Personal"
    val provider: AccountProvider = AccountProvider.GMAIL,
    val isConnected: Boolean = true,
    val unreadToday: Int = 0,
    val unreadTotal: Int = 0,
    val priorityToday: Int = 0,
    val priorityTotal: Int = 0

)
