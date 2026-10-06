package com.chuckwolfe.inboxpriorities

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    billingViewModel: BillingViewModel,
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
    var searchQuery by remember { mutableStateOf("") }

    // Temporary storage for pending authorization account details across the consent flow
    var pendingEmail by remember { mutableStateOf("") }
    var pendingNickname by remember { mutableStateOf("") }
    var pendingProvider by remember { mutableStateOf(AccountProvider.GMAIL) }

    // Collect Pro status and price from the BillingViewModel
    val isPro by billingViewModel.isProUnlocked.collectAsState()
    val price by billingViewModel.productPrice.collectAsState()
    val activity = LocalContext.current as? Activity

    // Activity result launcher for handling Google OAuth Consent Resolution
    val resolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            android.util.Log.d("Debug_Auth", "Consent resolution flow returned RESULT_OK. Extracting authorization result...")
            try {
                val authorizationResult = authHelper.authClient.getAuthorizationResultFromIntent(result.data!!)
                val accessToken = authorizationResult.accessToken

                if (!accessToken.isNullOrEmpty()) {
                    android.util.Log.d("Debug_Auth", "Successfully obtained Gmail OAuth Access Token post-resolution")
                    viewModel.cacheGoogleToken(pendingEmail, accessToken)

                    val finalNickname = pendingNickname.ifBlank { pendingEmail }
                    onAddAccount(pendingEmail, finalNickname, pendingProvider)
                    viewModel.loadAllDefinedAccounts(context)
                } else {
                    android.util.Log.e("Debug_Auth", "Post-resolution authorization result still has null/empty access token")
                }
            } catch (e: Exception) {
                android.util.Log.e("Debug_Auth", "Failed to retrieve authorization result from intent after resolution", e)
            }
        } else {
            android.util.Log.d("Debug_Auth", "User cancelled or failed consent resolution flow.")
        }
    }

    val maxAccounts = 1
    val isLimitReached = !isPro && accounts.size >= maxAccounts

    // Filter accounts based on searchQuery matching nickname or email address
    val filteredAccounts = remember(accounts, searchQuery) {
        if (searchQuery.isBlank()) {
            accounts
        } else {
            accounts.filter { account ->
                account.nickname.contains(searchQuery, ignoreCase = true) ||
                        account.emailAddress.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Header Section: Line 1 = Tier Badge, Line 2 = Accounts Title
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Line 1: Interactive Tier Badge Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isPro) {
                        Color(0xFFE8F5E9)
                    } else {
                        Color(0xFFFFCDD2)
                    },
                    modifier = Modifier
                        .then(
                            if (!isPro && activity != null) {
                                Modifier.clickable { billingViewModel.buyPro(activity) }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Text(
                        text = if (isPro) "PRO Tier" else "Free Tier (Unlock Pro - $price)",
                        color = if (isPro) {
                            Color(0xFF2E7D32)
                        } else {
                            Color(0xFFB71C1C)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Line 2: Accounts Title
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-header Row (Connected Count Badge + Add Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isPro) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "${accounts.size} Connected",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isPro) Color(0xFF2E7D32) else Color(0xFF8E0000),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    enabled = !isLimitReached
                ) {
                    Text("+ Add Account")
                }
            }

            // Free tier limit warning displayed cleanly underneath if reached
            if (isLimitReached) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Free tier limit of $maxAccounts account reached. Unlock Pro to add more.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar Component
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                placeholder = { Text("Search accounts...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
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
                            text = "No email accounts.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Add Account' above to add one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (filteredAccounts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No accounts match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredAccounts, key = { it.id }) { account ->
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

                                // Store pending variables in case consent resolution is required
                                pendingEmail = targetEmail
                                pendingNickname = targetNickname
                                pendingProvider = provider

                                val authRequest = authHelper.createAuthorizationRequest(targetEmail)
                                authHelper.authClient.authorize(authRequest)
                                    .addOnSuccessListener { authorizationResult ->
                                        android.util.Log.d("Debug_Auth", "Authorization result received successfully!")

                                        if (authorizationResult.hasResolution() && authorizationResult.pendingIntent != null) {
                                            android.util.Log.d("Debug_Auth", "Consent resolution required. Launching resolution intent...")
                                            try {
                                                val intentSenderRequest = IntentSenderRequest.Builder(
                                                    authorizationResult.pendingIntent!!.intentSender
                                                ).build()
                                                resolutionLauncher.launch(intentSenderRequest)
                                            } catch (e: Exception) {
                                                android.util.Log.e("Debug_Auth", "Failed to launch resolution intent", e)
                                            }
                                        } else {
                                            val accessToken = authorizationResult.accessToken
                                            if (!accessToken.isNullOrEmpty()) {
                                                android.util.Log.d("Debug_Auth", "Successfully obtained Gmail OAuth Access Token directly")
                                                viewModel.cacheGoogleToken(targetEmail, accessToken)

                                                val finalNickname = targetNickname.ifBlank { targetEmail }
                                                onAddAccount(targetEmail, finalNickname, provider)
                                                viewModel.loadAllDefinedAccounts(context)
                                            } else {
                                                android.util.Log.e("Debug_Auth", "Authorization succeeded but access token was null/empty and no resolution was provided")
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        android.util.Log.e("Debug_Auth", "Failed to obtain Gmail authorization token", e)
                                    }
                            } else {
                                android.util.Log.d("Debug_Auth", "Credential type mismatch or not a Google ID token credential.")
                            }
                        } else {
                            // Non-Gmail providers
                            if (targetEmail.isNotBlank()) {
                                val finalNickname = targetNickname.ifBlank { targetEmail }
                                onAddAccount(targetEmail, finalNickname, provider)
                                viewModel.loadAllDefinedAccounts(context)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.util.Log.e("Debug_Auth", "Credential Manager failed with exception: ${e.localizedMessage}", e)
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
    val accountNameText = if (account.nickname.isNotBlank()) {
        "${account.nickname} ($providerLabel)"
    } else {
        providerLabel
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Line 1: Account Name / Nickname + Provider
            Text(
                text = accountNameText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Line 2: Email Address
            Text(
                text = account.emailAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Line 3: Edit and Delete Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("✏️", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("🗑️", fontSize = 18.sp)
                }
            }
        }
    }
}