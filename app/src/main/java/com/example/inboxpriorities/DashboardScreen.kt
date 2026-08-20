package com.example.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen(
    emails: List<Email>,
    rules: List<PriorityRule>,
    accounts: List<EmailAccount>,
    unreadCounts: Map<String, String>,
    unreadTodayCounts: Map<String, String> // <--- Added this parameter
) {
    // 1. Evaluate all emails against active rules
    val allEvaluatedEmails = remember(rules, emails) {
        val activeRules = rules.filter { it.isEnabled }

        emails.map { email ->
            val calculatedPriority = PriorityEvaluator.evaluatePriority(
                sender = email.sender,
                subject = email.subject,
                rules = activeRules
            ).first

            email.copy(priorityLevel = calculatedPriority)
        }
    }

    // 2. Dynamic top summary calculations
    val priorityUnreadToday = allEvaluatedEmails.count {
        !it.isRead && it.isToday && it.priorityLevel != PriorityLevel.NONE
    }
    val priorityUnreadTotal = allEvaluatedEmails.count {
        !it.isRead && it.priorityLevel != PriorityLevel.NONE
    }

    // 3. Filtered & sorted list for the Priority Emails section
    val priorityEmails = remember(allEvaluatedEmails) {
        allEvaluatedEmails
            .filter { it.priorityLevel != PriorityLevel.NONE }
            .sortedByDescending { email ->
                when (email.priorityLevel) {
                    PriorityLevel.HIGH -> 3
                    PriorityLevel.MEDIUM -> 2
                    PriorityLevel.LOW -> 1
                    PriorityLevel.NONE -> 0
                }
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. TOP SUMMARY TOTALS ---
        item {
            Column {
                Text(
                    text = "INBOX PRIORITIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val totalUnreadToday = allEvaluatedEmails.count { !it.isRead && it.isToday }

                // Pull the global/primary account unread string, defaulting to "0" if not loaded yet
                val globalUnreadTotal = unreadCounts.values.firstOrNull() ?: "0"

                MetricCard(
                    title = "Total Unread",
                    mainValue = "$totalUnreadToday today",
                    subValue = "$globalUnreadTotal total",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Priority Unread",
                    mainValue = "$priorityUnreadToday today",
                    subValue = "$priorityUnreadTotal total",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 2. ACCOUNTS BREAKDOWN ---
        item {
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (accounts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "No connected accounts. Please add an email account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        } else {
            items(accounts, key = { it.id }) { account ->
                // Account-specific local counts for today's filter
                val accountEmails = allEvaluatedEmails.filter { email ->
                    email.recipientAccount.equals(account.emailAddress, ignoreCase = true)
                }
                val accountUnreadToday = unreadTodayCounts[account.emailAddress] ?: "0"

                // Pull formatted API count string for this specific account
                val trueAccountTotal = unreadCounts[account.emailAddress] ?: "0"

                val accountPriorityToday = accountEmails.count { !it.isRead && it.isToday && it.priorityLevel != PriorityLevel.NONE }
                val accountPriorityTotal = accountEmails.count { !it.isRead && it.priorityLevel != PriorityLevel.NONE }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val providerLabel = account.provider.name.lowercase().replaceFirstChar { it.uppercase() }
                        val titleText = if (account.nickname.isNotBlank()) {
                            "${account.nickname} ($providerLabel)"
                        } else {
                            account.emailAddress
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (account.nickname.isNotBlank()) {
                                Text(
                                    text = account.emailAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "$accountUnreadToday today · $trueAccountTotal total unread",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Dynamic priority badge state
                        val hasPriority = accountPriorityTotal > 0
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (hasPriority) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        ) {
                            val badgeText = when {
                                !hasPriority -> "No priority"
                                accountPriorityToday == accountPriorityTotal -> "$accountPriorityToday priority"
                                else -> "$accountPriorityToday today · $accountPriorityTotal total priority"
                            }

                            Text(
                                text = badgeText,
                                color = if (hasPriority) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. DETAIL EMAIL LIST ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Priority Emails",
                style = MaterialTheme.typography.titleLarge
            )
        }

        val hasActiveRules = rules.any { it.isEnabled }

        if (!hasActiveRules) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "No active priority rules. Please add or enable priority rule(s).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        } else {
            items(priorityEmails, key = { it.id }) { email ->
                EmailCard(email = email)
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    mainValue: String,
    subValue: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = mainValue,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun EmailCard(email: Email) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = email.sender,
                    style = MaterialTheme.typography.titleMedium
                )
                PriorityBadge(priorityLevel = email.priorityLevel)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = email.subject,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = email.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}