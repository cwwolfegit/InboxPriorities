package com.chuckwolfe.inboxpriorities

import android.app.Activity
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    viewModel: MainViewModel,
    billingViewModel: BillingViewModel,
    rules: List<PriorityRule>,
    ruleError: String?,
    onClearError: () -> Unit,
    onToggleRule: (PriorityRule, Boolean) -> Unit,
    onAddRule: (name: String, pattern: String, ruleType: RuleType, priorityLevel: PriorityLevel) -> Boolean,
    onUpdateRule: (oldRule: PriorityRule, newRule: PriorityRule) -> Boolean,
    onDeleteRule: (PriorityRule) -> Unit,
    modifier: Modifier = Modifier,
    freeRuleLimit: Int = 1
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var showAddDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<PriorityRule?>(null) }
    var ruleToDelete by remember { mutableStateOf<PriorityRule?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Collect Pro status and price from the BillingViewModel
    val isPro by billingViewModel.isProUnlocked.collectAsState()
    val price by billingViewModel.productPrice.collectAsState()

    val activeRulesCount = rules.count { it.isEnabled }
    val totalRulesCount = rules.size
    val isLimitReached = !isPro && totalRulesCount >= freeRuleLimit

    // Dynamic colors for the main tier badge matching Accounts and Dashboard
    val badgeBgColor = if (isPro) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer
    val badgeTextColor = if (isPro) Color(0xFF2E7D32) else Color(0xFF8E0000)

    // Filter rules based on searchQuery matching rule name or match pattern
    val filteredRules = remember(rules, searchQuery) {
        if (searchQuery.isBlank()) {
            rules
        } else {
            rules.filter { rule ->
                rule.name.contains(searchQuery, ignoreCase = true) ||
                        rule.matchPattern.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Rules",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Interactive Tier Badge Button next to Title
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = badgeBgColor,
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
                                    color = badgeTextColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sub-header Row (Active Count Badge + Stacked Warning Text + Add Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Count Badge
                Surface(
                    color = if (isPro) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "$activeRulesCount / $totalRulesCount Active",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPro) Color(0xFF2E7D32) else Color(0xFF8E0000),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Two-line limit warning stacked in the middle to prevent button layout shifts
                if (isLimitReached) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Free tier limit of $freeRuleLimit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "rules reached.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // High-visibility "+ Add Rule" Button (locked in position)
                Button(
                    onClick = {
                        if (!isLimitReached) {
                            showAddDialog = true
                        }
                    },
                    enabled = !isLimitReached,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "+ Add Rule",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Search Bar Component
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search rules...") },
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

            if (rules.isEmpty()) {
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
                            text = "No priority rules.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Add Rule' above to add one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredRules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No rules match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Sort filtered rules: Enabled first, then HIGH -> MEDIUM -> LOW -> NONE
                val sortedRules = remember(filteredRules) {
                    filteredRules.sortedWith(
                        compareByDescending<PriorityRule> { it.isEnabled }
                            .thenByDescending { rule ->
                                when (rule.priorityLevel) {
                                    PriorityLevel.HIGH -> 3
                                    PriorityLevel.MEDIUM -> 2
                                    PriorityLevel.LOW -> 1
                                    PriorityLevel.NONE -> 0
                                }
                            }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    items(
                        items = sortedRules,
                        key = { rule -> rule.id }
                    ) { rule ->
                        RuleItemCard(
                            rule = rule,
                            onToggleEnable = { isChecked -> onToggleRule(rule, isChecked) },
                            onEditClick = { ruleToEdit = rule },
                            onDeleteClick = { ruleToDelete = rule }
                        )
                    }
                }
            }
        }

        // Show Dialog when user taps "+ Add Rule"
        if (showAddDialog) {
            AddEditRuleDialog(
                errorMessage = ruleError,
                onDismissRequest = {
                    onClearError()
                    showAddDialog = false
                },
                onSaveRule = { name, pattern, ruleType, priority ->
                    val isSuccess = onAddRule(name, pattern, ruleType, priority)
                    if (isSuccess) {
                        showAddDialog = false
                    }
                }
            )
        }

        // Show Dialog when user taps Edit (✏️) on a card
        ruleToEdit?.let { targetRule ->
            AddEditRuleDialog(
                ruleToEdit = targetRule,
                errorMessage = ruleError,
                onDismissRequest = {
                    onClearError()
                    ruleToEdit = null
                },
                onSaveRule = { name, pattern, ruleType, priority ->
                    val updatedRule = targetRule.copy(
                        name = name,
                        matchPattern = pattern,
                        ruleType = ruleType,
                        priorityLevel = priority
                    )
                    val isSuccess = onUpdateRule(targetRule, updatedRule)
                    if (isSuccess) {
                        ruleToEdit = null
                    }
                }
            )
        }
    }

    // Shows the dialog whenever ruleToDelete is non-null
    ruleToDelete?.let { rule ->
        DeleteRuleConfirmationDialog(
            rule = rule,
            onConfirmDelete = {
                onDeleteRule(rule)
                ruleToDelete = null
            },
            onDismiss = {
                ruleToDelete = null
            }
        )
    }
}

@Composable
fun DeleteRuleConfirmationDialog(
    rule: PriorityRule,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Priority Rule",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"${rule.name}\"? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmDelete()
                    onDismiss()
                }
            ) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}