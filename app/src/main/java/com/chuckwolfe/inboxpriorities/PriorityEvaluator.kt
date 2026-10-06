package com.chuckwolfe.inboxpriorities

import android.util.Log

object PriorityEvaluator {

    private const val TAG = "PriorityEvaluator"

    /**
     * Evaluates incoming email details against active priority rules.
     * Supports standard rules (including NONE overrides) first,
     * followed by the @Contacts fallback rule (restricted to paid tier).
     */
    fun evaluatePriority(
        sender: String,
        subject: String,
        rules: List<PriorityRule> = emptyList(),
        isPro: Boolean = false,
        isContact: (String) -> Boolean = { false }
    ): Pair<PriorityLevel, String?> {

        Log.d(TAG, "Evaluating priority for sender: '$sender', subject: '$subject', isPro: $isPro, rules count: ${rules.size}")

        // 1. Evaluate all non-@Contacts rules first
        for (rule in rules) {
            if (!rule.isEnabled) continue
            if (rule.name == "@Contacts") continue

            if (ruleMatches(rule, sender, subject)) {
                Log.d(TAG, "Matched standard rule: '${rule.name}' with priority: ${rule.priorityLevel}")
                return Pair(rule.priorityLevel, rule.name)
            }
        }

        // 2. If no standard rule matched and user is on the paid tier, check for an active @Contacts rule
        if (isPro) {
            val contactsRule = rules.firstOrNull { it.isEnabled && it.priorityLevel != PriorityLevel.NONE && it.name.equals("@Contacts", ignoreCase = true) }
            if (contactsRule != null) {
                val cleanEmail = extractEmail(sender)
                val isMatchedContact = isContact(cleanEmail)
                Log.d(TAG, "Evaluating @Contacts rule for sender raw: '$sender' -> cleaned: '$cleanEmail'. Is contact match? $isMatchedContact")
                if (isMatchedContact) {
                    return Pair(contactsRule.priorityLevel, contactsRule.name)
                }
            } else {
                Log.d(TAG, "No active @Contacts rule found for paid user.")
            }
        } else if (rules.any { it.name == "@Contacts" && it.isEnabled }) {
            Log.d(TAG, "@Contacts rule exists, but user is not marked as paid tier (isPro = false).")
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