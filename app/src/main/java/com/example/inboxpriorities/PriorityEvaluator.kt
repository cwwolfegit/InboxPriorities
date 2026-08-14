package com.example.inboxpriorities

object PriorityEvaluator {

    /**
     * Evaluates incoming email details against active priority rules.
     * Returns a Pair containing the PriorityLevel and the matched rule's name (or null if no match).
     */
    fun evaluatePriority(
        sender: String,
        subject: String,
        rules: List<PriorityRule> = emptyList()
    ): Pair<PriorityLevel, String?> {
        for (rule in rules) {
            // 1. Skip disabled rules
            if (!rule.isEnabled) continue

            // 2. Check match based on ruleType
            if (ruleMatches(rule, sender, subject)) {
                return Pair(rule.priorityLevel, rule.name)
            }
        }

        // 3. Fallback when no active rule matches
        return Pair(PriorityLevel.NONE, null)
    }

    private fun ruleMatches(rule: PriorityRule, sender: String, subject: String): Boolean {
        return when (rule.ruleType) {
            RuleType.SENDER_CONTAINS -> sender.contains(rule.matchPattern, ignoreCase = true)
            RuleType.SUBJECT_CONTAINS -> subject.contains(rule.matchPattern, ignoreCase = true)
        }
    }
}