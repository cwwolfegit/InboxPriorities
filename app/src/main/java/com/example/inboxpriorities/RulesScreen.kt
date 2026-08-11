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
    freeRuleLimit: Int = 10
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<PriorityRule?>(null) }

    val activeRulesCount = rules.count { it.isEnabled }
    val totalRulesCount = rules.size
    val isLimitReached = !isProUser && totalRulesCount >= freeRuleLimit

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Priority Rules") }
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
            }

            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No rules created yet.\nTap '+ Add Rule' above to add one!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // <-- Fixes scroll bounds inside Column
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    items(rules) { rule ->
                        RuleItemCard(
                            rule = rule,
                            onToggleEnable = { isChecked -> onToggleRule(rule, isChecked) },
                            onEditClick = { ruleToEdit = rule },
                            onDeleteClick = { onDeleteRule(rule) }
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
}