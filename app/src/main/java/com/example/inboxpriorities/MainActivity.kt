package com.example.inboxpriorities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

enum class AppScreen { DASHBOARD, RULES }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

                var rules by remember {
                    mutableStateOf(
                        listOf(
                            PriorityRule(
                                name = "Client Emails",
                                ruleType = RuleType.SENDER_CONTAINS,
                                matchPattern = "@client.com",
                                priorityLevel = PriorityLevel.HIGH,
                                isEnabled = true
                            ),
                            PriorityRule(
                                name = "Digest Updates",
                                ruleType = RuleType.SUBJECT_CONTAINS,
                                matchPattern = "Digest",
                                priorityLevel = PriorityLevel.MEDIUM,
                                isEnabled = true
                            )
                        )
                    )
                }

                // Raw incoming sample emails matching the Email data class
                val rawEmails = remember {
                    listOf(
                        Email(
                            id = "1",
                            sender = "Acme Client Corp",
                            senderAddress = "john@client.com",
                            subject = "Project Deliverables Review",
                            body = "Please find attached the latest project updates for review...",
                            timestamp = "10:15 AM"
                        ),
                        Email(
                            id = "2",
                            sender = "Dev Digest",
                            senderAddress = "newsletter@devdigest.com",
                            subject = "Weekly Android Development Digest",
                            body = "Learn what's new in Jetpack Compose and Kotlin Multiplatform...",
                            timestamp = "9:00 AM"
                        ),
                        Email(
                            id = "3",
                            sender = "General Inquiry",
                            senderAddress = "hello@randomsite.org",
                            subject = "Quick Question about your App",
                            body = "Hi team, I had a question regarding user pricing...",
                            timestamp = "Yesterday"
                        )
                    )
                }

                // Dynamically evaluate each email against active rules
                val evaluatedEmails = rawEmails.map { EmailItem ->
                    val result = PriorityEvaluator.evaluatePriority(
                        sender = EmailItem.senderAddress.ifEmpty { EmailItem.sender },
                        subject = EmailItem.subject,
                        rules = rules
                    )
                    EmailItem.copy(
                        priorityLevel = result.first,
                        matchedRuleName = result.second
                    )
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = { currentScreen = AppScreen.DASHBOARD },
                                label = { Text("Dashboard") },
                                icon = { Text("📊") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.RULES,
                                onClick = { currentScreen = AppScreen.RULES },
                                label = { Text("Rules") },
                                icon = { Text("⚙️") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            AppScreen.DASHBOARD -> DashboardScreen(emails = evaluatedEmails)
                            AppScreen.RULES -> RulesScreen(
                                rules = rules,
                                onToggleRule = { toggledRule, isEnabled ->
                                    rules = rules.map { rule ->
                                        if (rule == toggledRule) rule.copy(isEnabled = isEnabled) else rule
                                    }
                                },
                                onAddRule = { name, pattern, ruleType, priority ->
                                    val newRule = PriorityRule(
                                        name = name,
                                        ruleType = ruleType,
                                        matchPattern = pattern,
                                        priorityLevel = priority,
                                        isEnabled = true
                                    )
                                    rules = rules + newRule
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}