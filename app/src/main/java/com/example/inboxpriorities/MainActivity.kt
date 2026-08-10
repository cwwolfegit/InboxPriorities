package com.example.inboxpriorities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

enum class AppScreen { DASHBOARD, RULES }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.RULES) }

                var rules by remember {
                    mutableStateOf(
                        listOf(
                            PriorityRule(
                                name = "Client Emails",
                                ruleType = RuleType.SENDER_CONTAINS,
                                matchPattern = "@client.com",
                                priorityLevel = PriorityLevel.HIGH,
                                isEnabled = true
                            )
                        )
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
                            AppScreen.DASHBOARD -> DashboardPlaceholder()
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

@Composable
fun DashboardPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Dashboard Feed Coming Soon!", style = MaterialTheme.typography.titleMedium)
    }
}