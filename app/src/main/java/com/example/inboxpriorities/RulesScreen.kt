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
    modifier: Modifier = Modifier,
    isProUser: Boolean = false,
    freeRuleLimit: Int = 10
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val activeRulesCount = rules.count { it.isEnabled }
    val totalRulesCount = rules.size
    val isLimitReached = !isProUser && totalRulesCount >= freeRuleLimit

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Priority Rules") },
                actions = {
                    Surface(
                        color = if (isLimitReached) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = if (isProUser) "Pro (Unlimited)" else "$activeRulesCount / $totalRulesCount Active",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (isLimitReached) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (!isLimitReached) {
                        showAddDialog = true
                    }
                },
                icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
                text = { Text(if (isLimitReached) "Upgrade to Add More" else "Add Rule") },
                containerColor = if (isLimitReached) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No rules created yet.\nTap '+' below to add one!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                items(rules) { rule ->
                    RuleItemCard(
                        rule = rule,
                        onToggleEnable = { isChecked -> onToggleRule(rule, isChecked) }
                    )
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
    }
}

@Preview(showBackground = true)
@Composable
private fun RulesScreenPreview() {
    val sampleRules = listOf(
        PriorityRule(
            name = "Client Emails",
            ruleType = RuleType.SENDER_CONTAINS,
            matchPattern = "@client.com",
            priorityLevel = PriorityLevel.HIGH,
            isEnabled = true
        )
    )

    MaterialTheme {
        RulesScreen(
            rules = sampleRules,
            onToggleRule = { _, _ -> },
            onAddRule = { _, _, _, _ -> },
            isProUser = false,
            freeRuleLimit = 10
        )
    }
}