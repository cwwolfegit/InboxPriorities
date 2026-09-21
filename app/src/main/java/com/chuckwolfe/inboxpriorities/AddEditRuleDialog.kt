package com.chuckwolfe.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleDialog(
    ruleToEdit: PriorityRule? = null,
    errorMessage: String? = null,
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
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.labelLarge
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Split PriorityLevel entries into rows of 2
                    PriorityLevel.entries.chunked(2).forEach { rowLevels ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowLevels.forEach { level ->
                                FilterChip(
                                    selected = selectedPriority == level,
                                    onClick = { selectedPriority = level },
                                    label = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = level.name)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // If a row has an odd number of items, add an empty spacer to keep grid balanced
                            if (rowLevels.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
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