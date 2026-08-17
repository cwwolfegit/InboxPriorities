package com.example.inboxpriorities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.inboxpriorities.ui.theme.InboxPrioritiesTheme

enum class AppTab {
    DASHBOARD,
    ACCOUNTS,
    RULES
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = RulesRepository(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InboxPrioritiesTheme {
                var selectedTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }
                val rules by viewModel.rules.collectAsState()
                val accounts by viewModel.accounts.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == AppTab.DASHBOARD,
                                onClick = { selectedTab = AppTab.DASHBOARD },
                                icon = { Text("📊", fontSize = 20.sp) },
                                label = { Text("Dashboard") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.ACCOUNTS,
                                onClick = { selectedTab = AppTab.ACCOUNTS },
                                icon = { Text("👤", fontSize = 20.sp) },
                                label = { Text("Accounts") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.RULES,
                                onClick = { selectedTab = AppTab.RULES },
                                icon = { Text("⚙️", fontSize = 20.sp) },
                                label = { Text("Rules") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (selectedTab) {
                            AppTab.DASHBOARD -> {
                                DashboardScreen(
                                    emails = MockDataRepository.mockEmails,
                                    rules = rules,
                                    accounts = accounts
                                )
                            }
                            AppTab.ACCOUNTS -> {
                                AccountsScreen(
                                    accounts = accounts,
                                    onAddAccount = { email, nickname, provider ->
                                        viewModel.addAccount(email, nickname, provider)
                                    },
                                    onRemoveAccountClick = { accountId ->
                                        viewModel.removeAccount(accountId)
                                    }
                                )
                            }
                            AppTab.RULES -> {
                                RulesScreen(
                                    rules = rules,
                                    onToggleRule = { rule, isEnabled ->
                                        viewModel.toggleRule(rule, isEnabled)
                                    },
                                    onAddRule = { name, pattern, ruleType, priority ->
                                        viewModel.addRule(name, pattern, ruleType, priority)
                                    },
                                    onUpdateRule = { oldRule, newRule ->
                                        viewModel.updateRule(oldRule, newRule)
                                    },
                                    onDeleteRule = { rule ->
                                        viewModel.deleteRule(rule)
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