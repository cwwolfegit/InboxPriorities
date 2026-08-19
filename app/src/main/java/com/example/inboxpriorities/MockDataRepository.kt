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

    // --- Raw Email Mock Data (Mapped to recipient accounts) ---

    val mockEmails = listOf(
        Email(
            id = "1",
            sender = "boss@company.com",
            subject = "Project Status",
            body = "Need an update on the build.",
            recipientAccount = "work@company.com",
            isRead = false,
            isToday = true,
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "2",
            sender = "news@clientcorp.com",
            subject = "Quarterly Report",
            body = "Attached is the Q3 overview.",
            recipientAccount = "work@company.com",
            isRead = false,
            isToday = true,
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "3",
            sender = "friend@gmail.com",
            subject = "URGENT: Lunch plans",
            body = "Are we still meeting today?",
            recipientAccount = "Personal@outlook.com",
            isRead = false,
            isToday = true,
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "4",
            sender = "billing@service.com",
            subject = "Monthly Invoice",
            body = "Your invoice is ready for review.",
            recipientAccount = "work@company.com",
            isRead = false,
            isToday = false,
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "5",
            sender = "support@oldvendor.com",
            subject = "System Maintenance",
            body = "Scheduled downtime tonight.",
            recipientAccount = "Personal@outlook.com",
            isRead = false,
            isToday = true,
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "6",
            sender = "newsletter@random.com",
            subject = "Weekly Digest",
            body = "Here are the top stories of the week.",
            recipientAccount = "Personal@outlook.com",
            isRead = true,
            isToday = false,
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "7",
            sender = "boss@company.com",
            subject = "Project Status",
            body = "Outlook test priority today",
            recipientAccount = "Personal@outlook.com",
            isRead = false,
            isToday = true,
            priorityLevel = PriorityLevel.NONE
        ),
        Email(
            id = "8",
            sender = "boss@company.com",
            subject = "Project Status",
            body = "Outlook test priority total",
            recipientAccount = "Personal@outlook.com",
            isRead = false,
            isToday = false,
            priorityLevel = PriorityLevel.NONE
        )
    )
}