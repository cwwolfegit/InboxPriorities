package com.example.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val summary = MockDataRepository.mockSummary
    val accounts = MockDataRepository.mockAccounts
    val senders = MockDataRepository.mockSenders

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbox Priorities", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEVEL 1: Summary Card (Priority information appears FIRST)
            item {
                Text(
                    text = "UNREAD EMAIL SUMMARY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                SummaryCard(summary)
            }

            // LEVEL 2: Account Breakdown
            item {
                Text(
                    text = "UNREAD EMAILS BY ACCOUNT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            items(accounts) { account ->
                AccountRow(account)
            }

            // LEVEL 3: Priority Senders
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "UNREAD EMAILS BY PRIORITY SENDER",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            items(senders) { sender ->
                SenderRow(sender)
            }
        }
    }
}

@Composable
fun SummaryCard(summary: DashboardSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("PRIORITY EMAILS", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Today: ${summary.unreadPriorityToday}", fontWeight = FontWeight.Bold)
                Text("Total: ${summary.unreadPriorityTotal}", fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text("TOTAL EMAILS", fontWeight = FontWeight.Medium, fontSize = 12.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Today: ${summary.unreadToday}")
                Text("Total: ${summary.unreadTotal}")
            }
        }
    }
}

@Composable
fun AccountRow(account: AccountSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(account.accountName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Priority: ${account.unreadPriorityToday} today / ${account.unreadPriorityTotal} total",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Total: ${account.unreadTotal}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SenderRow(sender: PrioritySender) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(sender.senderAddress, fontWeight = FontWeight.Normal)
        Badge {
            Text("${sender.unreadCount}")
        }
    }
}