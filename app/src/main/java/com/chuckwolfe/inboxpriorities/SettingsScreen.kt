package com.chuckwolfe.inboxpriorities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
currentPriority: String, // e.g., "NONE", "LOW", "MEDIUM", "HIGH"
isPro: Boolean,
onPriorityChanged: (String) -> Unit,
onUpgradeClicked: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val priorities = listOf("NONE", "LOW", "MEDIUM", "HIGH")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("App Settings") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Priority Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Setting Item: Android Contacts Priority
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Prioritize Emails Using Android Contacts",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPro) "Applies fallback priority to incoming emails from your contacts."
                        else "Requires Pro Tier to customize (Locked to NONE).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                if (isPro) {
                                    expanded = true
                                } else {
                                    onUpgradeClicked()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Priority Level: $currentPriority ${if (!isPro) "🔒" else ""}")
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            priorities.forEach { priority ->
                                DropdownMenuItem(
                                    text = { Text(priority) },
                                    onClick = {
                                        onPriorityChanged(priority)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}