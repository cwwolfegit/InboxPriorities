package     com.chuckwolfe.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    accountToEdit: EmailAccount? = null,
    onDismissRequest: () -> Unit,
    onSaveAccount: (email: String, nickname: String, provider: AccountProvider) -> Unit
) {
    var email by remember { mutableStateOf(accountToEdit?.emailAddress ?: "") }
    var nickname by remember { mutableStateOf(accountToEdit?.nickname ?: "") }
    var selectedProvider by remember { mutableStateOf(accountToEdit?.provider ?: AccountProvider.GMAIL) }
    var expanded by remember { mutableStateOf(false) }

    val isEmailValid = email.isNotBlank() && email.contains("@")
    val isEditing = accountToEdit != null

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = if (isEditing) "Edit Email Account" else "Add Email Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Nickname Field
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Provider Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedProvider.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AccountProvider.entries.forEach { provider ->
                            DropdownMenuItem(
                                text = {
                                    Text(provider.name.lowercase().replaceFirstChar { it.uppercase() })
                                },
                                onClick = {
                                    selectedProvider = provider
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isEmailValid) {
                        onSaveAccount(email.trim(), nickname.trim(), selectedProvider)
                    }
                },
                enabled = isEmailValid
            ) {
                Text(if (isEditing) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}