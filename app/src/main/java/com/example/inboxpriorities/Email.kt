package com.example.inboxpriorities

data class Email(
    val id: String,
    val sender: String,
    val subject: String,
    val body: String,
    val recipientAccount: String = "",
    val isRead: Boolean = false,
    val isToday: Boolean = true,
    val priorityLevel: PriorityLevel = PriorityLevel.NONE,
    val timestamp: Long = 0L)