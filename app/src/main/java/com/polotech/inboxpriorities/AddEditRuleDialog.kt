package com.polotech.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

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
                    PriorityLevel.entries.chunked(2).forEach { rowLevels ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowLevels.forEach { level ->
                                val textColor = when (level) {
                                    PriorityLevel.HIGH -> Color(0xFFC62828)
                                    PriorityLevel.MEDIUM -> Color(0xFFEF6C00)
                                    PriorityLevel.LOW -> Color(0xFF1976D2) // Blue
                                    PriorityLevel.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                val containerColor = when (level) {
                                    PriorityLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
                                    PriorityLevel.MEDIUM -> Color(0xFFFFFE0B2)
                                    PriorityLevel.LOW -> Color(0xFFE3F2FD) // Light blue container
                                    PriorityLevel.NONE -> MaterialTheme.colorScheme.surfaceVariant
                                }

                                FilterChip(
                                    selected = selectedPriority == level,
                                    onClick = { selectedPriority = level },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = containerColor,
                                        selectedLabelColor = textColor,
                                        selectedLeadingIconColor = textColor
                                    ),
                                    label = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = level.name,
                                                color = if (selectedPriority == level) textColor else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
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