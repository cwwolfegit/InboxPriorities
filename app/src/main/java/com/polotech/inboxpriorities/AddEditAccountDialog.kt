package com.polotech.inboxpriorities

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    accountToEdit: EmailAccount? = null,
    accountError: String? = null,
    onClearError: () -> Unit,
    onDismissRequest: () -> Unit,
    onSaveAccount: (email: String, nickname: String, provider: AccountProvider) -> Unit
) {
    var email by remember { mutableStateOf(accountToEdit?.emailAddress ?: "") }
    var nickname by remember { mutableStateOf(accountToEdit?.nickname ?: "") }
    var selectedProvider by remember { mutableStateOf(accountToEdit?.provider ?: AccountProvider.GMAIL) }
    var expanded by remember { mutableStateOf(false) }

    val trimmedEmail = email.trim()
    val isEmailValid = trimmedEmail.isNotEmpty() &&
            Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    val showFormatError = trimmedEmail.isNotEmpty() && !isEmailValid
    val hasError = !accountError.isNullOrBlank() || showFormatError
    val isEditing = accountToEdit != null

    // Clear external error when typing a new email
    LaunchedEffect(email) {
        if (!accountError.isNullOrBlank()) {
            onClearError()
        }
    }

    AlertDialog(
        onDismissRequest = {
            onClearError()
            onDismissRequest()
        },
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
                    isError = hasError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    supportingText = if (hasError) {
                        {
                            Text(
                                text = accountError?.takeIf { it.isNotBlank() }
                                    ?: "Enter a valid email address"
                            )
                        }
                    } else {
                        null
                    },
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
                        onSaveAccount(trimmedEmail, nickname.trim(), selectedProvider)
                    }
                },
                enabled = isEmailValid
            ) {
                Text(if (isEditing) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onClearError()
                    onDismissRequest()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}