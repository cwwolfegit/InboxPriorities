package com.chuckwolfe.inboxpriorities

import android.app.Activity
import android.widget.Toast
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
    modifier: Modifier = Modifier,
    billingViewModel: BillingViewModel,
    rules: List<PriorityRule>,
    onClearError: () -> Unit,
    onToggleRule: (PriorityRule, Boolean) -> Unit,
    onAddRule: (name: String, pattern: String, ruleType: RuleType, priorityLevel: PriorityLevel) -> Boolean,
    onUpdateRule: (oldRule: PriorityRule, newRule: PriorityRule) -> Boolean,
    onDeleteRule: (PriorityRule) -> Unit,
    currentContactPriority: String = "NONE",
    onContactPriorityChanged: (String) -> Unit = {},
    freeRuleLimit: Int = 1,
    ruleError: String? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var showAddDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<PriorityRule?>(null) }
    var ruleToDelete by remember { mutableStateOf<PriorityRule?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var contactDropdownExpanded by remember { mutableStateOf(false) }

    // Collect Pro status and price from the BillingViewModel
    val isPro by billingViewModel.isProUnlocked.collectAsState()
    val price by billingViewModel.productPrice.collectAsState()

    val activeRulesCount = rules.count { it.isEnabled }
    val totalRulesCount = rules.size
    val isLimitReached = !isPro && totalRulesCount >= freeRuleLimit

    val priorities = listOf("NONE", "LOW", "MEDIUM", "HIGH")

    // Color mapping for the global contact priority button
    val priorityColor = when (currentContactPriority.uppercase()) {
        "HIGH" -> Color(0xFFC62828)
        "MEDIUM" -> Color(0xFFEF6C00)
        "LOW" -> Color(0xFF1976D2)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val priorityContainerColor = when (currentContactPriority.uppercase()) {
        "HIGH" -> MaterialTheme.colorScheme.errorContainer
        "MEDIUM" -> Color(0xFFFFE0B2)
        "LOW" -> Color(0xFFE3F2FD)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

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

    // Sort filtered rules safely outside of any composable capture issues
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

    Scaffold(
        modifier = modifier,
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
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                // --- Global Android Contacts Priority Card ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Prioritize Emails Using Your Contacts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPro) "Applies fallback priority to incoming emails from your device contacts."
                                else "Requires Pro Tier to customize (Locked to NONE).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        if (isPro) {
                                            contactDropdownExpanded = true
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Upgrade to Pro to use your Contacts as priority.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = priorityContainerColor
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Priority Level: $currentContactPriority ${if (!isPro) "🔒" else ""}",
                                        color = priorityColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                DropdownMenu(
                                    expanded = contactDropdownExpanded,
                                    onDismissRequest = { contactDropdownExpanded = false }
                                ) {
                                    priorities.forEach { priority ->
                                        DropdownMenuItem(
                                            text = { Text(priority) },
                                            onClick = {
                                                android.util.Log.d("NBX_Debug_ContactPriority", "Dropdown item clicked: $priority")
                                                onContactPriorityChanged(priority)
                                                contactDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Custom Rules Header & Actions ---
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active Count Badge
                            Surface(
                                color = if (isPro) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Text(
                                    text = "$activeRulesCount / $totalRulesCount Active Rules",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPro) Color(0xFF2E7D32) else Color(0xFF8E0000),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
                                    text = "+ Add Rule",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Free tier limit warning on a separate line below
                        if (isLimitReached) {
                            Text(
                                text = "Free tier limit of $freeRuleLimit rule reached. Unlock Pro to add more.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // --- Search Bar Component ---
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
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
                }

                // --- Rules List Handling ---
                if (rules.isEmpty()) {
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
                                    text = "No custom priority rules.",
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
                    }
                } else if (filteredRules.isEmpty()) {
                    item {
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
                    }
                } else {
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
fun RuleItemCard(
    rule: PriorityRule,
    onToggleEnable: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val ruleTypeLabel = when (rule.ruleType) {
        RuleType.SENDER_CONTAINS -> "Sender contains"
        RuleType.SUBJECT_CONTAINS -> "Subject contains"
    }

    val priorityColor = when (rule.priorityLevel) {
        PriorityLevel.HIGH -> Color(0xFFC62828)
        PriorityLevel.MEDIUM -> Color(0xFFEF6C00)
        PriorityLevel.LOW -> Color(0xFF1976D2)
        PriorityLevel.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val priorityContainerColor = when (rule.priorityLevel) {
        PriorityLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
        PriorityLevel.MEDIUM -> Color(0xFFFFE0B2)
        PriorityLevel.LOW -> Color(0xFFE3F2FD)
        PriorityLevel.NONE -> MaterialTheme.colorScheme.surfaceVariant
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
            // Line 1: Rule Name
            Text(
                text = rule.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Line 2: Rule Type (e.g., "Sender contains")
            Text(
                text = ruleTypeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Line 3: Match Pattern (e.g., "cwwxyz123@gmail.com")
            Text(
                text = "\"${rule.matchPattern}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Line 4: Priority Level Badge (e.g., "HIGH")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = priorityContainerColor
            ) {
                Text(
                    text = rule.priorityLevel.name,
                    color = priorityColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Line 5: All Icons (Edit, Delete, Switch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("✏️", fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("🗑️", fontSize = 18.sp)
                    }
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleEnable
                )
            }
        }
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