package com.example.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    rules: List<PriorityRule>,
    onToggleRule: (PriorityRule, Boolean) -> Unit,
    onAddRule: (name: String, pattern: String, ruleType: RuleType, priorityLevel: PriorityLevel) -> Unit,
    onUpdateRule: (oldRule: PriorityRule, newRule: PriorityRule) -> Unit,
    onDeleteRule: (PriorityRule) -> Unit,
    modifier: Modifier = Modifier,
    isProUser: Boolean = false,
    freeRuleLimit: Int = 3
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<PriorityRule?>(null) }

    val activeRulesCount = rules.count { it.isEnabled }
    val totalRulesCount = rules.size
    val isLimitReached = !isProUser && totalRulesCount >= freeRuleLimit

    // State to hold the rule pending deletion (null when dialog is closed)
    var ruleToDelete by remember { mutableStateOf<PriorityRule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rules",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
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
            // Prominent Action Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Count Badge
                Surface(
                    color = if (isLimitReached) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (isProUser) "Pro (Unlimited)" else "$activeRulesCount / $totalRulesCount Active",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = if (isLimitReached) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // High-visibility "+ Add Rule" Button
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
                        text = if (isLimitReached) "Limit Reached" else "+ Add Rule",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isLimitReached) {
                    Text(
                        text = "Free tier limit of $freeRuleLimit rules reached.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

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
                            text = "No priority rules created yet.",
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
            } else {
                // Sort rules: Enabled first, then HIGH -> MEDIUM -> LOW -> NONE
                val sortedRules = remember(rules) {
                    rules.sortedWith(
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
                        .weight(1f) // <-- Fixes scroll bounds inside Column
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
                onDismissRequest = { showAddDialog = false },
                onSaveRule = { name, pattern, ruleType, priority ->
                    onAddRule(name, pattern, ruleType, priority)
                    showAddDialog = false
                }
            )
        }

        // Show Dialog when user taps Edit (✏️) on a card
        ruleToEdit?.let { targetRule ->
            AddEditRuleDialog(
                ruleToEdit = targetRule,
                onDismissRequest = { ruleToEdit = null },
                onSaveRule = { name, pattern, ruleType, priority ->
                    val updatedRule = targetRule.copy(
                        name = name,
                        matchPattern = pattern,
                        ruleType = ruleType,
                        priorityLevel = priority
                    )
                    onUpdateRule(targetRule, updatedRule)
                    ruleToEdit = null
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
                    color = MaterialTheme.colorScheme.error, // Red text for destructive action
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