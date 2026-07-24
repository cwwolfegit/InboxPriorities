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
}