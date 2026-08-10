package com.example.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleDialog(
    onDismissRequest: () -> Unit,
    onSaveRule: (name: String, pattern: String, ruleType: RuleType, priorityLevel: PriorityLevel) -> Unit,
    modifier: Modifier = Modifier,
    initialRule: PriorityRule? = null
) {
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var matchPattern by remember { mutableStateOf(initialRule?.matchPattern ?: "") }
    var selectedRuleType by remember { mutableStateOf(initialRule?.ruleType ?: RuleType.SENDER_CONTAINS) }
    var selectedPriority by remember { mutableStateOf(initialRule?.priorityLevel ?: PriorityLevel.HIGH) }

    val isInputValid = name.isNotBlank() && matchPattern.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = if (initialRule == null) "Add Priority Rule" else "Edit Priority Rule") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rule Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    placeholder = { Text("e.g. Client Emails") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Match Pattern Field
                OutlinedTextField(
                    value = matchPattern,
                    onValueChange = { matchPattern = it },
                    label = { Text("Match Pattern") },
                    placeholder = { Text("e.g. @client.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Rule Type Selection
                Text("Rule Type:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                // Priority Level Selection
                Text("Priority Level:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val levels = listOf(PriorityLevel.HIGH, PriorityLevel.MEDIUM, PriorityLevel.LOW)
                    levels.forEach { level ->
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
            Button(
                onClick = {
                    if (isInputValid) {
                        onSaveRule(name, matchPattern, selectedRuleType, selectedPriority)
                    }
                },
                enabled = isInputValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun AddEditRuleDialogPreview() {
    MaterialTheme {
        AddEditRuleDialog(
            onDismissRequest = {},
            onSaveRule = { _, _, _, _ -> }
        )
    }
}