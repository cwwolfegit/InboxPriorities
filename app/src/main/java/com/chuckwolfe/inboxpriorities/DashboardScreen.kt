package com.chuckwolfe.inboxpriorities

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat

import com.chuckwolfe.inboxpriorities.utils.openEmailInbox

import android.app.Activity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

enum class EmailFilterTab { TODAY, TOTAL }

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    billingViewModel: BillingViewModel = viewModel(),
    emails: List<Email>,
    rules: List<PriorityRule>,
    accounts: List<EmailAccount>,
    unreadCounts: Map<String, String>,
    unreadTodayCounts: Map<String, String>,
    contactPriority: String = "NONE"
) {
    val context = LocalContext.current
    val googleAuthHelper = remember { GoogleAuthHelper(context) }
    val isLoading by viewModel.isLoading.collectAsState()
    val accountErrors by viewModel.accountErrors.collectAsState()

    val priorityUnreadMap by viewModel.priorityUnreadCounts.collectAsState()
    val priorityTodayMap by viewModel.priorityTodayCounts.collectAsState()
    val priorityEmailsByAccount by viewModel.todayPriorityEmailsByAccount.collectAsState()
    var expandedAccounts by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }

    // Collect Pro status and price from the BillingViewModel
    val isPro by billingViewModel.isProUnlocked.collectAsState()
    val price by billingViewModel.productPrice.collectAsState()
    val activity = LocalContext.current as? Activity

    // Runtime Permission Launcher for reading contacts
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("Permissions", "READ_CONTACTS permission granted")
        } else {
            Log.w("Permissions", "READ_CONTACTS permission denied")
        }
    }

    // Check and request READ_CONTACTS permission when the Dashboard loads
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    // Unified start of today timestamp helper
    val startOfTodayMillis = remember {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        calendar.timeInMillis
    }

    // This will trigger whenever the accounts list changes
    LaunchedEffect(key1 = accounts) {
        if (accounts.isNotEmpty()) {
            viewModel.loadAllDefinedAccounts(context)
        }
    }

    // 1. Filter accounts based on searchQuery matching nickname or email address
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

    // 2. Dynamic top summary calculations
    val hasActiveRules = rules.any { it.isEnabled }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. TOP HEADER SECTION ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Line 1: "INBOX PRIORITIES" inside a styled Surface matching priority counts
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "INBOX PRIORITIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Line 2: "Free Tier" badge
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

                // Line 3: "Dashboard" on left, Refresh icon on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { viewModel.loadAllDefinedAccounts(context, force = true) },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Dashboard",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // --- 2. ACCOUNTS BREAKDOWN & INLINE ACCORDIONS ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
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
        }

        if (accounts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFCE4EC)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "No connected accounts. Please add an email account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF880E4F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        } else if (filteredAccounts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
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
                val accountEmails = priorityEmailsByAccount[account.emailAddress] ?: emptyList()
                val isExpanded = expandedAccounts.contains(account.emailAddress)
                val accountUnreadToday = unreadTodayCounts[account.emailAddress] ?: "0"
                val trueAccountTotal = unreadCounts[account.emailAddress] ?: "0"

                val accountPriorityToday = priorityTodayMap[account.emailAddress] ?: "0"
                val accountPriorityTotal = priorityUnreadMap[account.emailAddress] ?: "0"
                val numericPriorityTotal = accountPriorityTotal.toIntOrNull() ?: 0
                val hasPriority = numericPriorityTotal > 0

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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val providerLabel = account.provider.name.lowercase().replaceFirstChar { it.uppercase() }
                        val titleText = if (account.nickname.isNotBlank()) {
                            "${account.nickname} ($providerLabel)"
                        } else {
                            account.emailAddress
                        }
                        val errorText = accountErrors[account.emailAddress]

                        if (errorText != null) {
                            Surface(
                                color = Color(0xFFFFCDD2),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (account.provider == AccountProvider.OUTLOOK) {
                                            MicrosoftAuthHelper.startLogin(context)
                                        } else {
                                            val authRequest = googleAuthHelper.createAuthorizationRequest(account.emailAddress)
                                            googleAuthHelper.authClient.authorize(authRequest)
                                                .addOnSuccessListener { authorizationResult ->
                                                    if (authorizationResult.accessToken != null) {
                                                        val newToken = authorizationResult.accessToken!!
                                                        viewModel.retryAccountAuth(account.emailAddress, newToken, context)
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    android.util.Log.e("Debug_Dashboard", "Failed to re-authorize Google account: ${account.emailAddress}", e)
                                                }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tap to Sign-in!",
                                        color = Color(0xFFB71C1C),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry Sign-in",
                                        tint = Color(0xFFB71C1C),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (hasPriority) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier.then(
                                    if (hasPriority) {
                                        Modifier.clickable {
                                            expandedAccounts = if (isExpanded) {
                                                expandedAccounts - account.emailAddress
                                            } else {
                                                expandedAccounts + account.emailAddress
                                            }
                                        }
                                    } else Modifier
                                )
                            ) {
                                val badgeText = when {
                                    !hasPriority -> "No priorities"
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

                            if (hasPriority) {
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            expandedAccounts = if (isExpanded) {
                                                expandedAccounts - account.emailAddress
                                            } else {
                                                expandedAccounts + account.emailAddress
                                            }
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isExpanded) "▲" else "▼",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$accountUnreadToday today ·",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "$trueAccountTotal total unread",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { openEmailInbox(context, account.emailAddress) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Open Email Inbox",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isExpanded && accountEmails.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            var selectedTab by remember(accountEmails) {
                                val todayCount = accountEmails.count { it.timestamp >= startOfTodayMillis }
                                if (todayCount == 0 && accountEmails.isNotEmpty()) {
                                    mutableStateOf(EmailFilterTab.TOTAL)
                                } else {
                                    mutableStateOf(EmailFilterTab.TODAY)
                                }
                            }

                            val displayedEmails = remember(accountEmails, selectedTab) {
                                when (selectedTab) {
                                    EmailFilterTab.TODAY -> accountEmails.filter { it.timestamp >= startOfTodayMillis }
                                    EmailFilterTab.TOTAL -> accountEmails
                                }
                            }

                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                SegmentedButton(
                                    selected = selectedTab == EmailFilterTab.TODAY,
                                    onClick = { selectedTab = EmailFilterTab.TODAY },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    Text("Today ($accountPriorityToday)")
                                }
                                SegmentedButton(
                                    selected = selectedTab == EmailFilterTab.TOTAL,
                                    onClick = { selectedTab = EmailFilterTab.TOTAL },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Text("Total (${accountEmails.size})")
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                displayedEmails.forEach { email ->
                                    EmailCard(email = email)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!hasActiveRules && contactPriority.uppercase() == "NONE") {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFCE4EC)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "No active priority rules. Please add or enable priority rule(s).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF880E4F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmailCard(email: Email) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Sender & Subject occupy the top area cleanly without side-clutter
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = email.sender,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = email.subject,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Email body preview snippet
            Text(
                text = email.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Last line / Footer row: Email icon on the left, Priority Badge on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email Icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                if (email.priorityLevel != PriorityLevel.NONE) {
                    PriorityBadge(priorityLevel = email.priorityLevel)
                }
            }
        }
    }
}