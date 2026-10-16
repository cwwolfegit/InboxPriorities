package com.polotech.inboxpriorities

import android.util.Log

object PriorityEvaluator {

    private const val TAG = "NBX_Debug_PriorityEvaluator"

    /**
     * Evaluates incoming email details against active priority rules.
     * Supports standard rules first, followed by the global contact priority setting (restricted to pro tier).
     */
    fun evaluatePriority(
        sender: String,
        subject: String,
        rules: List<PriorityRule> = emptyList(),
        isPro: Boolean = false,
        contactPriority: String = "NONE",
        isContact: (String) -> Boolean = { false }
    ): Pair<PriorityLevel, String?> {

        Log.d(TAG, "Evaluating priority for sender: '$sender', subject: '$subject', isPro: $isPro, contactPriority: $contactPriority, rules count: ${rules.size}")

        // 1. Evaluate all priority rules first
        for (rule in rules) {
            if (!rule.isEnabled) continue

            if (ruleMatches(rule, sender, subject)) {
                Log.d(TAG, "Matched standard rule: '${rule.name}' with priority: ${rule.priorityLevel}")
                return Pair(rule.priorityLevel, rule.name)
            }
        }

        // 2. If no standard rule matched and user is on the paid tier, check the contact priority setting
        val parsedContactPriority = parsePriorityLevel(contactPriority)
        if (isPro && parsedContactPriority != PriorityLevel.NONE) {
            val cleanEmail = extractEmail(sender)
            val isMatchedContact = isContact(cleanEmail)
            Log.d(TAG, "Evaluating contact priority setting ($parsedContactPriority) for sender raw: '$sender' -> cleaned: '$cleanEmail'. Is contact match? $isMatchedContact")
            if (isMatchedContact) {
                return Pair(parsedContactPriority, "Contacts Setting")
            }
        } else if (!isPro && contactPriority.uppercase() != "NONE") {
            Log.d(TAG, "Contact priority setting exists, but user is not marked as Pro tier (isPro = false).")
        }

        // 3. Fallback when no rule matches
        Log.d(TAG, "No rules matched. Falling back to PriorityLevel.NONE")
        return Pair(PriorityLevel.NONE, null)
    }

    private fun ruleMatches(rule: PriorityRule, sender: String, subject: String): Boolean {
        return when (rule.ruleType) {
            RuleType.SENDER_CONTAINS -> sender.contains(rule.matchPattern, ignoreCase = true)
            RuleType.SUBJECT_CONTAINS -> subject.contains(rule.matchPattern, ignoreCase = true)
        }
    }

    private fun parsePriorityLevel(priorityStr: String): PriorityLevel {
        return try {
            PriorityLevel.valueOf(priorityStr.uppercase())
        } catch (e: IllegalArgumentException) {
            PriorityLevel.NONE
        }
    }

    fun extractEmail(sender: String): String {
        val startIndex = sender.indexOf('<')
        val endIndex = sender.indexOf('>')
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            sender.substring(startIndex + 1, endIndex).trim()
        } else {
            sender.trim()
        }
    }
}