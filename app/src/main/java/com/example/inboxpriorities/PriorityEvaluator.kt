package com.example.inboxpriorities

object PriorityEvaluator {

    /**
     * Evaluates incoming email details against active priority rules.
     * Returns the matching PriorityLevel, or LOW if no active rule matches.
     */
    fun evaluatePriority(
        senderEmail: String,
        subject: String,
        rules: List<PriorityRule> = MockDataRepository.mockRules
    ): PriorityLevel {

        // 1. Filter to only evaluate rules that are enabled
        val activeRules = rules.filter { it.isEnabled }

        // 2. Iterate through rules to find the first matching condition
        for (rule in activeRules) {
            when (rule.ruleType) {
                RuleType.SENDER_CONTAINS -> {
                    if (senderEmail.contains(rule.matchPattern, ignoreCase = true)) {
                        return rule.priorityLevel
                    }
                }
                RuleType.SUBJECT_CONTAINS -> {
                    if (subject.contains(rule.matchPattern, ignoreCase = true)) {
                        return rule.priorityLevel
                    }
                }
             }
        }

        // Default priority if no rules match
        return PriorityLevel.NONE
    }
}