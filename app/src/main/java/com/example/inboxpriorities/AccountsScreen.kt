package com.example.inboxpriorities

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    accounts: List<EmailAccount>,
    onAddAccount: (email: String, nickname: String, provider: AccountProvider) -> Unit,
    onUpdateAccount: (oldAccount: EmailAccount, updatedAccount: EmailAccount) -> Unit,
    onRemoveAccountClick: (String) -> Unit
) {
    val context = LocalContext.current
    val authHelper = remember { GmailAuthHelper(context) }

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<EmailAccount?>(null) }
    var accountToDelete by remember { mutableStateOf<EmailAccount?>(null) }

    // Declare pendingAccountData BEFORE signInLauncher so it is in scope
    var pendingAccountData by remember { mutableStateOf<Pair<String, String>?>(null) }

    val maxAccounts = 3
    val isLimitReached = accounts.size >= maxAccounts

    // Activity launcher for Google Sign-In OAuth flow
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val googleAccount = task.getResult(ApiException::class.java)
            if (googleAccount != null) {
                val email = googleAccount.email.orEmpty()
                // Use the nickname from the dialog if provided
                val nickname = pendingAccountData?.second.takeIf { !it.isNullOrBlank() }
                    ?: googleAccount.displayName.orEmpty().ifBlank { "Gmail Account" }

                if (email.isNotBlank()) {
                    onAddAccount(email, nickname, AccountProvider.GMAIL)
                    viewModel.loadGmailMessages(context, googleAccount)
                }
            }
        } catch (e: ApiException) {
            println("Google Sign-In failed with status code: ${e.statusCode}")
        } finally {
            pendingAccountData = null
        }
    }

    // Dynamic colors for the badge based on limit state
    val badgeBgColor = if (isLimitReached) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val badgeTextColor = if (isLimitReached) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

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

            // Sub-header Row (Badge + Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeBgColor,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "${accounts.size} / $maxAccounts Connected",
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeTextColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        enabled = !isLimitReached
                    ) {
                        Text(if (isLimitReached) "Limit Reached" else "+ Add Account")
                    }
                    if (isLimitReached) {
                        Text(
                            text = "Free tier limit of $maxAccounts accounts reached.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (accounts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No email accounts connected yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Add Account' above to connect your account!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(accounts, key = { it.id }) { account ->
                AccountManagementCard(
                    account = account,
                    onEdit = { accountToEdit = account },
                    onRemove = { accountToDelete = account }
                )
            }
        }
    }

    // --- Add Account Dialog ---
    if (showAddDialog) {
        AddEditAccountDialog(
            accountToEdit = null,
            onDismissRequest = { showAddDialog = false },
            onSaveAccount = { email, nickname, provider ->
                showAddDialog = false
                if (provider == AccountProvider.GMAIL) {
                    pendingAccountData = Pair(email, nickname)

                    // Forcefully revoke access and sign out to clear cached tokens
                    // and guarantee the account picker UI opens
                    authHelper.signInClient.revokeAccess().addOnCompleteListener {
                        authHelper.signInClient.signOut().addOnCompleteListener {
                            signInLauncher.launch(authHelper.signInClient.signInIntent)
                        }
                    }
                } else {
                    onAddAccount(email, nickname, provider)
                }
            }
        )
    }

    // --- Edit Account Dialog ---
    accountToEdit?.let { targetAccount ->
        AddEditAccountDialog(
            accountToEdit = targetAccount,
            onDismissRequest = { accountToEdit = null },
            onSaveAccount = { email, nickname, provider ->
                val updatedAccount = targetAccount.copy(
                    emailAddress = email,
                    nickname = nickname,
                    provider = provider
                )
                onUpdateAccount(targetAccount, updatedAccount)
                accountToEdit = null
            }
        )
    }

    // --- Delete Account Dialog ---
    accountToDelete?.let { account ->
        val displayName = account.nickname.ifBlank { account.emailAddress }

        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = {
                Text(
                    text = "Delete Account",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete '$displayName'? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveAccountClick(account.id)
                        accountToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AccountManagementCard(
    account: EmailAccount,
    onEdit: () -> Unit,
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

            Row {
                IconButton(onClick = onEdit) {
                    Text("✏️", fontSize = 18.sp)
                }
                IconButton(onClick = onRemove) {
                    Text("🗑️", fontSize = 18.sp)
                }
            }
        }
    }
}