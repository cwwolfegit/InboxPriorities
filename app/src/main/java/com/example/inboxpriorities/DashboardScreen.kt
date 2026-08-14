package com.example.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    emails: List<Email>,
    rules: List<PriorityRule>,
    summary: DashboardSummary = MockDataRepository.mockSummary,
    accounts: List<AccountSummary> = MockDataRepository.mockAccounts
) {
    // Live evaluation of raw emails against active DataStore rules
    val evaluatedEmails = remember(rules, emails) {
        val activeRules = rules.filter { it.isEnabled }

        emails.map { email ->
            val calculatedPriority = PriorityEvaluator.evaluatePriority(
                sender = email.sender,
                subject = email.subject,
                rules = activeRules
            ).first

            email.copy(priorityLevel = calculatedPriority)
        }
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
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Priority Unread",
                    mainValue = "${summary.unreadPriorityToday} today",
                    subValue = "${summary.unreadPriorityTotal} total",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Unread",
                    mainValue = "${summary.unreadToday} today",
                    subValue = "${summary.unreadTotal} total",
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

        items(accounts, key = { it.id }) { account ->
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
                    Column {
                        Text(
                            text = account.accountName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${account.unreadTotal} unread total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${account.unreadPriorityToday} priority",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
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

        items(evaluatedEmails, key = { it.id }) { email ->
            EmailCard(email = email)
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