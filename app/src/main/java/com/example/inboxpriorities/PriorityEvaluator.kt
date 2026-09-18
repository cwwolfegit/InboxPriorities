package     com.chuckwolfe.inboxpriorities

object PriorityEvaluator {

    /**
     * Evaluates incoming email details against active priority rules.
     * Supports standard rules (including NONE overrides) first,
     * followed by the @Contacts fallback rule (restricted to paid tier).
     */
    fun evaluatePriority(
        sender: String,
        subject: String,
        rules: List<PriorityRule> = emptyList(),
        isPaidUser: Boolean = false,              // Check if user is on the paid tier
        isContact: (String) -> Boolean = { false } // Lookup function for @Contacts
    ): Pair<PriorityLevel, String?> {
        // 1. Evaluate all non-@Contacts rules first (allows specific rules/NONE overrides to take precedence)
        for (rule in rules) {
            if (!rule.isEnabled) continue
            if (rule.name == "@Contacts") continue // Skip @Contacts in the primary pass

            if (ruleMatches(rule, sender, subject)) {
                return Pair(rule.priorityLevel, rule.name)
            }
        }

        // 2. If no standard rule matched and user is on the paid tier, check for an active @Contacts rule
        if (isPaidUser) {
            val contactsRule = rules.firstOrNull { it.isEnabled && it.name == "@Contacts" }
            if (contactsRule != null) {
                if (isContact(sender)) {
                    return Pair(contactsRule.priorityLevel, contactsRule.name)
                }
            }
        }

        // 3. Fallback when no rule matches
        return Pair(PriorityLevel.NONE, null)
    }

    private fun ruleMatches(rule: PriorityRule, sender: String, subject: String): Boolean {
        return when (rule.ruleType) {
            RuleType.SENDER_CONTAINS -> sender.contains(rule.matchPattern, ignoreCase = true)
            RuleType.SUBJECT_CONTAINS -> subject.contains(rule.matchPattern, ignoreCase = true)
        }
    }
}