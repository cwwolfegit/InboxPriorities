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
import com.chuckwolfe.inboxpriorities.ContactCache.isContact

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
    unreadTodayCounts: Map<String, String>
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

    // This will trigger whenever the accounts list changes (e.g., when Room finishes loading them)
    LaunchedEffect(key1 = accounts) {
        if (accounts.isNotEmpty()) {
            viewModel.loadAllDefinedAccounts(context)
        }
    }

    // 1. Evaluate all emails against active rules
    val allEvaluatedEmails = remember(rules, emails, accounts) {
        val activeRules = rules.filter { it.isEnabled }

        Log.d("isPaidTier_Dashboard", "Evaluating with isPaidTier: $isPro")
        emails.map { email ->
            val defaultAccountEmail = accounts.firstOrNull()?.emailAddress ?: ""
            val calculatedPriority = PriorityEvaluator.evaluatePriority(
                sender = email.sender,
                subject = email.subject,
                rules = activeRules,
                isPaidUser = isPro,
                isContact = { cleanedEmail ->
                    isContact(defaultAccountEmail, cleanedEmail)
                }
            ).first

            email.copy(priorityLevel = calculatedPriority)
        }
    }

    // 2. Filter accounts based on searchQuery matching nickname or email address
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

    // 3. Dynamic top summary calculations
    val hasActiveRules = rules.any { it.isEnabled }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. TOP SUMMARY TOTALS & TIER INDICATOR ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "INBOX PRIORITIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1A73E8),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Interactive Tier Badge Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isPro) {
                                Color(0xFFE8F5E9)
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            modifier = Modifier
                                .then(
                                    if (!isPro && activity != null) {
                                        Modifier.clickable { billingViewModel.buyPro(activity) }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isPro) "PRO Tier" else "Free Tier (Unlock Pro - $price)",
                                color = if (isPro) {
                                    Color(0xFF2E7D32)
                                } else {
                                    Color(0xFF8E0000)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

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
            Spacer(modifier = Modifier.height(12.dp))
        }

        // --- 2. ACCOUNTS BREAKDOWN & INLINE ACCORDIONS ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.titleLarge
                )

                // Search Bar Component
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
                        .padding(top = 8.dp)
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
                val accountEmails = priorityEmailsByAccount[account.emailAddress] ?: emptyList()
                val isExpanded = expandedAccounts.contains(account.emailAddress)
                val accountUnreadToday = unreadTodayCounts[account.emailAddress] ?: "0"
                val trueAccountTotal = unreadCounts[account.emailAddress] ?: "0"

                val accountPriorityToday = priorityTodayMap[account.emailAddress] ?: "0"
                val accountPriorityTotal = priorityUnreadMap[account.emailAddress] ?: "0"
                val numericPriorityTotal = accountPriorityTotal.toIntOrNull() ?: 0
                val hasPriority = numericPriorityTotal > 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            openEmailInbox(context, account.emailAddress)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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

                        // --- LINE 1: Sign-in Error Banner with Refresh Icon (Only if needed) ---
                        if (errorText != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
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
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry Sign-in",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // --- LINE 2: Account Name & Provider Only ---
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // --- LINE 3: Email Address Only ---
                        if (account.nickname.isNotBlank()) {
                            Text(
                                text = account.emailAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // --- LINE 4: Priority Badge & Expand Arrow (If priority exists) ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
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

                            if (hasPriority) {
                                Text(
                                    text = if (isExpanded) "▲" else "▼",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // --- LINE 5: Non-Priority Unread Counts & Email Icon (If no priority) ---
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

                            if (!hasPriority) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Open Email Inbox",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Nested Expandable Email List inside the Account Card
                        if (isExpanded && accountEmails.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            var selectedTab by remember(accountEmails) {
                                val todayCount = accountEmails.count { it.timestamp >= startOfTodayMillis }
                                val initialTab = if (todayCount == 0 && accountEmails.isNotEmpty()) {
                                    EmailFilterTab.TOTAL
                                } else {
                                    EmailFilterTab.TODAY
                                }
                                mutableStateOf(initialTab)
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

                            displayedEmails.forEach { email ->
                                EmailCard(email = email)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- 3. ACTIVE RULES WARNING ---
        if (!hasActiveRules) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFCE4EC)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = email.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}