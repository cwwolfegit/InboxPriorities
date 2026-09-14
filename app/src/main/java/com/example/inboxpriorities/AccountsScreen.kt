package     com.chuckwolfe.inboxpriorities

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
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    accounts: List<EmailAccount>,
    onAddAccount: (email: String, nickname: String, provider: AccountProvider) -> Unit,
    onUpdateAccount: (oldAccount: EmailAccount, updatedAccount: EmailAccount) -> Unit,
    onRemoveAccountClick: (String) -> Unit
) {
    val context = LocalContext.current
    val authHelper = remember { GoogleAuthHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<EmailAccount?>(null) }
    var accountToDelete by remember { mutableStateOf<EmailAccount?>(null) }

    val maxAccounts = 3
    val isLimitReached = accounts.size >= maxAccounts

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

                coroutineScope.launch {
                    try {
                        var targetEmail = email
                        var targetNickname = nickname

                        if (provider == AccountProvider.GMAIL) {
                            android.util.Log.d("Debug_Auth", "Attempting to create credential request for Gmail...")
                            val request = authHelper.createCredentialRequest()

                            android.util.Log.d("Debug_Auth", "Calling getCredential...")
                            val result = authHelper.manager.getCredential(
                                context = context,
                                request = request
                            )
                            val credential = result.credential

                            if (credential is androidx.credentials.CustomCredential &&
                                credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                                targetEmail = googleIdTokenCredential.id
                                val userDisplayName = googleIdTokenCredential.displayName ?: "Gmail Account"
                                if (targetNickname.isBlank()) {
                                    targetNickname = userDisplayName
                                }

                                android.util.Log.d("Debug_Auth", "Successfully retrieved Google account for: $targetEmail")

                                // Request the OAuth Access Token using GoogleAuthUtil on an IO dispatcher
                                android.util.Log.d("Debug_Auth", "Successfully retrieved Google account for: $targetEmail")

                                // Request the OAuth Access Token via AuthorizationClient (which handles consent prompts automatically)
                                val authRequest = authHelper.createAuthorizationRequest(targetEmail)
                                authHelper.authClient.authorize(authRequest)
                                    .addOnSuccessListener { authorizationResult ->
                                        android.util.Log.d("Debug_Auth", "Authorization result received successfully!")
                                        val accessToken = authorizationResult.accessToken

                                        if (!accessToken.isNullOrEmpty()) {
                                            android.util.Log.d("Debug_Auth", "Successfully obtained Gmail OAuth Access Token")
                                            viewModel.cacheGoogleToken(targetEmail, accessToken)

                                            val finalNickname = targetNickname.ifBlank { targetEmail }
                                            onAddAccount(targetEmail, finalNickname, provider)
                                            viewModel.loadAllDefinedAccounts(context)
                                        } else {
                                            android.util.Log.e("Debug_Auth", "Authorization succeeded but access token was null/empty")
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        android.util.Log.e("Debug_Auth", "Failed to obtain Gmail authorization token", e)
                                    }
                            } else {
                                android.util.Log.d("Debug_Auth", "Credential type mismatch or not a Google ID token credential.")
                            }
                        }

                        if (targetEmail.isNotBlank()) {
                            val finalNickname = targetNickname.ifBlank { targetEmail }
                            onAddAccount(targetEmail, finalNickname, provider)
                            viewModel.loadAllDefinedAccounts(context)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.util.Log.e("Debug_Auth", "Credential Manager failed with exception: ${e.localizedMessage}", e)

                        if (email.isNotBlank()) {
                            val finalNickname = nickname.ifBlank { email }
                            onAddAccount(email, finalNickname, provider)
                            viewModel.cacheSession(email, "fallback_token")
                        }
                    }
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