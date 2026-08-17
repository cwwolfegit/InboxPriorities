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

@Composable
fun AccountsScreen(
    accounts: List<EmailAccount>,
    onAddAccountClick: () -> Unit,
    onRemoveAccountClick: (String) -> Unit
) {
    val maxAccounts = 3
    val isLimitReached = accounts.size >= maxAccounts

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Sub-header Row (Badge + Button) matching Rules layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "${accounts.size} / $maxAccounts Connected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Button(
                    onClick = onAddAccountClick,
                    enabled = !isLimitReached
                ) {
                    Text("+ Add Account")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(accounts, key = { it.id }) { account ->
            AccountManagementCard(
                account = account,
                onRemove = { onRemoveAccountClick(account.id) }
            )
        }
    }
}

@Composable
fun AccountManagementCard(
    account: EmailAccount,
    onRemove: () -> Unit
) {
    val providerLabel = account.provider.name.lowercase().replaceFirstChar { it.uppercase() }
    val titleText = if (account.nickname.isNotBlank()) {
        "${account.nickname} ($providerLabel)"
    } else {
        account.emailAddress
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            }

            IconButton(onClick = onRemove) {
                Text("🗑️", fontSize = 18.sp)
            }
        }
    }
}