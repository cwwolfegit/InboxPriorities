package com.example.inboxpriorities

data class Email(
    val id: String,
    val sender: String,
    val senderAddress: String = "",
    val subject: String,
    val body: String,
    val timestamp: String = "",
    val priorityLevel: PriorityLevel = PriorityLevel.LOW,
    val matchedRuleName: String? = null
)