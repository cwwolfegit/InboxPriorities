package     com.chuckwolfe.inboxpriorities

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

enum class EmailFilterTab { TODAY, TOTAL }

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
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

    // Calculate aggregate top-level priority counts from the ViewModel maps
    val totalPriorityTodaySum = priorityTodayMap.values.sumOf { it.toIntOrNull() ?: 0 }
    val totalPriorityTotalSum = priorityUnreadMap.values.sumOf { it.toIntOrNull() ?: 0 }

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
    val hasActiveRules = rules.any { it.isEnabled }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. TOP SUMMARY TOTALS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
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
                                ),
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

                                val errorText = accountErrors[account.emailAddress]

                                if (errorText != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
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
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Sign-in Expired (Tap to fix)",
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Retry Sign-in",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 8.dp)
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
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isExpanded) "▲" else "▼",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Nested Expandable Email List inside the Account Card
                        if (isExpanded && accountEmails.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // 1. Tab state & filtered list
                            val startOfTodayMillis = remember {
                                val calendar = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }
                                calendar.timeInMillis
                            }

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

                            // 2. Modern segmented button row
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
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

                            // 3. Loop through filtered items
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
                Text(
                    text = email.sender,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f) // Ensures sender text wraps nicely if long
                )
                if (email.priorityLevel != PriorityLevel.NONE) {
                    Spacer(modifier = Modifier.width(8.dp))
                    PriorityBadge(priorityLevel = email.priorityLevel)
                }
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
