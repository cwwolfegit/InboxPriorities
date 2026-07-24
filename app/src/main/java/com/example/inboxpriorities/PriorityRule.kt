package com.example.inboxpriorities

import java.util.UUID

/**
 * Defines what email attribute to evaluate for priority matching.
 */
enum class RuleType {
    SENDER_CONTAINS,  // Matches substring anywhere in sender email address
    SUBJECT_CONTAINS, // Matches substring in subject line

}


/**
 * Defines the priority severity assigned when a rule matches.
 */
enum class PriorityLevel {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Data model representing an individual priority match rule.
 */
data class PriorityRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ruleType: RuleType,
    val matchPattern: String,
    val priorityLevel: PriorityLevel = PriorityLevel.HIGH,
    val targetAccountId: String? = null, // null means applies across ALL connected accounts
    val isEnabled: Boolean = true
)