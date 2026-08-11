package com.example.inboxpriorities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

enum class AppScreen { DASHBOARD, RULES }

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

                // Collect state streams from ViewModel
                val rules by viewModel.rules.collectAsState()
                val rawEmails by viewModel.rawEmails.collectAsState()

                // Dynamically evaluate incoming emails
                val evaluatedEmails = remember(rawEmails, rules) {
                    rawEmails.map { email ->
                        val result = PriorityEvaluator.evaluatePriority(
                            sender = email.senderAddress.ifEmpty { email.sender },
                            subject = email.subject,
                            rules = rules
                        )
                        email.copy(
                            priorityLevel = result.first,
                            matchedRuleName = result.second
                        )
                    }
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
                                    viewModel.toggleRule(toggledRule, isEnabled)
                                },
                                onAddRule = { name, pattern, ruleType, priority ->
                                    viewModel.addRule(name, pattern, ruleType, priority)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}