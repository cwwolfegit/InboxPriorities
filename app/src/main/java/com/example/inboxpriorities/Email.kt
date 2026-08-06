package com.example.inboxpriorities

data class Email(
    val id: String,
    val sender: String,
    val subject: String,
    val body: String,
    val priorityLevel: PriorityLevel = PriorityLevel.NONE
)