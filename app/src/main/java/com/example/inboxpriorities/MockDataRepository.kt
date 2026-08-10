package com.example.inboxpriorities

object MockDataRepository {

    // --- Dashboard Mock Data ---

    val mockSummary = DashboardSummary(
        unreadPriorityToday = 8,
        unreadPriorityTotal = 24,
        unreadToday = 42,
        unreadTotal = 156
    )

    val mockAccounts = listOf(
        AccountSummary("acc_1", "Work (Gmail)", 5, 18, 18, unreadTotal = 110),
        AccountSummary("acc_2", "Personal (Outlook)", 3, 6, 6, unreadTotal = 46)
    )

    val mockSenders = listOf(
        PrioritySender("boss@company.com", 3),
        PrioritySender("alerts@clientcorp.com", 2),
        PrioritySender("support@vendor.com", 3)
    )

    // --- Priority Rules Mock Data (Phase 2) ---

    val mockRules = listOf(
        PriorityRule(
            id = "rule_1",
            name = "Direct Manager",
            ruleType = RuleType.SENDER_CONTAINS,
            matchPattern = "boss@company.com",
            priorityLevel = PriorityLevel.HIGH,
            isEnabled = true
        ),
        PriorityRule(
            id = "rule_2",
            name = "Key Client Domain",
            ruleType = RuleType.SENDER_CONTAINS,
            matchPattern = "clientcorp.com",
            priorityLevel = PriorityLevel.HIGH,
            isEnabled = true
        ),
        PriorityRule(
            id = "rule_3",
            name = "Urgent Subject Line",
            ruleType = RuleType.SUBJECT_CONTAINS,
            matchPattern = "URGENT",
            priorityLevel = PriorityLevel.HIGH,
            isEnabled = true
        ),
        PriorityRule(
            id = "rule_4",
            name = "Invoices & Billing",
            ruleType = RuleType.SUBJECT_CONTAINS,
            matchPattern = "Invoice",
            priorityLevel = PriorityLevel.MEDIUM,
            isEnabled = true
        ),
        PriorityRule(
            id = "rule_5",
            name = "Old Vendor Alert (Disabled)",
            ruleType = RuleType.SENDER_CONTAINS,
            matchPattern = "support@oldvendor.com",
            priorityLevel = PriorityLevel.LOW,
            isEnabled = false
        )
    )

    // --- Raw Email Mock Data ---

    private val rawEmails = listOf(
        Email(
            id = "1",
            sender = "boss@company.com",
            subject = "Project Status",
            body = "Need an update on the build.",
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "2",
            sender = "news@clientcorp.com",
            subject = "Quarterly Report",
            body = "Attached is the Q3 overview.",
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "3",
            sender = "friend@gmail.com",
            subject = "URGENT: Lunch plans",
            body = "Are we still meeting today?",
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "4",
            sender = "billing@service.com",
            subject = "Monthly Invoice",
            body = "Your invoice is ready for review.",
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "5",
            sender = "support@oldvendor.com",
            subject = "System Maintenance",
            body = "Scheduled downtime tonight.",
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "6",
            sender = "newsletter@random.com",
            subject = "Weekly Digest",
            body = "Here are the top stories of the week.",
            priorityLevel = PriorityLevel.NONE
        )
    )

    // --- Dynamic Priority Evaluation ---

    /**
     * Filters active rules and calculates the priority for each raw email
     * on demand using the PriorityEvaluator logic.
     */
    fun getEvaluatedEmails(): List<Email> {
        // 1. Keep only enabled rules
        val activeRules = mockRules.filter { it.isEnabled }

        // 2. Pass activeRules into the evaluator for each email
        return rawEmails.map { email ->
            val calculatedPriority = PriorityEvaluator.evaluatePriority(
                sender = email.sender,
                subject = email.subject,
                rules = activeRules
            )
            email.copy(priorityLevel = calculatedPriority)
        }
    }
}