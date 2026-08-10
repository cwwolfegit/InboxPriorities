package com.example.inboxpriorities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ============================================================================
// MAIN DASHBOARD SCREEN
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(emails: List<Email>) {
    // 1. Calculate Priority Metrics
    val highCount = emails.count { it.priorityLevel == PriorityLevel.HIGH }
    val medCount = emails.count { it.priorityLevel == PriorityLevel.MEDIUM }
    val lowCount = emails.count { it.priorityLevel == PriorityLevel.LOW }

    // 2. Group emails for the feed sections (excluding NONE)
    val priorityGroups = emails
        .filter { it.priorityLevel != PriorityLevel.NONE }
        .groupBy { it.priorityLevel }

    val displayOrder = listOf(PriorityLevel.HIGH, PriorityLevel.MEDIUM, PriorityLevel.LOW)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbox Priorities") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: Top Summary Card (Provided by SummaryCard.kt)
            item {
                OptionASummaryCard(
                    totalUnread = emails.size,
                    highCount = highCount,
                    medCount = medCount,
                    lowCount = lowCount
                )
            }

            // SECTION 2: Unread Emails by Account
            item {
                AccountSummaryCard(emails = emails)
            }

            // SECTION 3: Prioritized Inbox Feed Header
            item {
                Text(
                    text = "Prioritized Inbox Feed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Loop through HIGH -> MEDIUM -> LOW priority levels
            displayOrder.forEach { level ->
                val listForLevel = priorityGroups[level] ?: emptyList()
                if (listForLevel.isNotEmpty()) {
                    item {
                        PrioritySectionHeader(level = level, count = listForLevel.size)
                    }
                    items(listForLevel) { email ->
                        EmailRow(email = email)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

// ============================================================================
// HELPER COMPOSABLES FOR DASHBOARD SCREEN
// ============================================================================

@Composable
private fun AccountSummaryCard(emails: List<Email>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Unread by Account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Primary Account",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${emails.size} unread",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PrioritySectionHeader(level: PriorityLevel, count: Int) {
    val (title, color) = when (level) {
        PriorityLevel.HIGH -> "High Priority" to Color(0xFFD32F2F)
        PriorityLevel.MEDIUM -> "Medium Priority" to Color(0xFFE65100)
        PriorityLevel.LOW -> "Low Priority" to Color(0xFF2E7D32)
        PriorityLevel.NONE -> "Other" to Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "$count emails",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun EmailRow(email: Email) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = email.sender,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = email.subject,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = email.body,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}