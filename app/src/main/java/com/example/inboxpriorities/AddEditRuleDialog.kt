package com.example.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleDialog(
    ruleToEdit: PriorityRule? = null,
    onDismissRequest: () -> Unit,
    onSaveRule: (name: String, pattern: String, ruleType: RuleType, priorityLevel: PriorityLevel) -> Unit
) {
    var name by remember { mutableStateOf(ruleToEdit?.name ?: "") }
    var matchPattern by remember { mutableStateOf(ruleToEdit?.matchPattern ?: "") }
    var selectedRuleType by remember { mutableStateOf(ruleToEdit?.ruleType ?: RuleType.SENDER_CONTAINS) }
    var selectedPriority by remember { mutableStateOf(ruleToEdit?.priorityLevel ?: PriorityLevel.HIGH) }

    val isEditing = ruleToEdit != null

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = if (isEditing) "Edit Priority Rule" else "Add Priority Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = matchPattern,
                    onValueChange = { matchPattern = it },
                    label = { Text("Match Pattern (e.g. @client.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Rule Type",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedRuleType == RuleType.SENDER_CONTAINS,
                        onClick = { selectedRuleType = RuleType.SENDER_CONTAINS },
                        label = { Text("Sender") }
                    )
                    FilterChip(
                        selected = selectedRuleType == RuleType.SUBJECT_CONTAINS,
                        onClick = { selectedRuleType = RuleType.SUBJECT_CONTAINS },
                        label = { Text("Subject") }
                    )
                }

                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityLevel.entries.filter { it != PriorityLevel.NONE }.forEach { level ->
                        FilterChip(
                            selected = selectedPriority == level,
                            onClick = { selectedPriority = level },
                            label = { Text(level.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && matchPattern.isNotBlank(),
                onClick = {
                    onSaveRule(name, matchPattern, selectedRuleType, selectedPriority)
                }
            ) {
                Text(if (isEditing) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}