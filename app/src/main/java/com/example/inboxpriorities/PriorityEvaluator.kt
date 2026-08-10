package com.example.inboxpriorities

object PriorityEvaluator {

    /**
     * Evaluates incoming email details against active priority rules.
     * Returns the matching PriorityLevel, or LOW if no active rule matches.
     */
    fun evaluatePriority(
        sender: String,
        subject: String,
        rules: List<PriorityRule> = MockDataRepository.mockRules

    ): PriorityLevel {
        // Iterate only through the rules passed into this function
        for (rule in rules) {
            if (ruleMatches(rule, sender, subject)) { // Or your specific rule-matching logic
                return rule.priorityLevel
            }
        }
        return PriorityLevel.NONE
    }

    private fun ruleMatches(rule: PriorityRule, sender: String, subject: String): Boolean {
        // Your matching logic (e.g., checking rule matchPattern against sender/subject)
        return sender.contains(rule.matchPattern, ignoreCase = true) ||
                subject.contains(rule.matchPattern, ignoreCase = true)
    }
}