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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.inboxpriorities.ui.theme.InboxPrioritiesTheme

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.inboxpriorities.diagnostics.GmailDiagnostic

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
                return MainViewModel(application = application, repository = repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        println("DEBUG_START: Entering onCreate()")
        super.onCreate(savedInstanceState)

        // --- RUN GMAIL DIAGNOSTIC TEST HERE ---
        //lifecycleScope.launch {
            //val token = "YOUR_TEST_OAUTH_TOKEN_HERE" // Or look up your active token here
            //GmailDiagnostic().runDiagnostic(token)
        //}
        // -------------------------------------

        setContent {
            InboxPrioritiesTheme {
                var selectedTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }
                val rules by viewModel.rules.collectAsStateWithLifecycle()
                val accounts by viewModel.accounts.collectAsStateWithLifecycle()
                val emails by viewModel.emails.collectAsStateWithLifecycle()
                val unreadCounts by viewModel.unreadCounts.collectAsStateWithLifecycle()
                val unreadTodayCounts by viewModel.unreadTodayCounts.collectAsStateWithLifecycle()

                // Automatically triggers once accounts are loaded from disk storage on startup
                val accountList by viewModel.accounts.collectAsState()

                LaunchedEffect(accountList) {
                    if (accountList.isNotEmpty()) {
                        viewModel.loadAllDefinedAccounts(context = this@MainActivity)
                    }
                }

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
                                    emails = emails,
                                    rules = rules,
                                    accounts = accounts,
                                    unreadCounts = unreadCounts,
                                    unreadTodayCounts = unreadTodayCounts
                                )
                            }
                            AppTab.ACCOUNTS -> {
                                AccountsScreen(
                                    viewModel = viewModel,
                                    accounts = accounts,
                                    onAddAccount = { email, nickname, provider ->
                                        viewModel.addAccount(email, nickname, provider)
                                    },
                                    onUpdateAccount = { oldAccount, updatedAccount ->
                                        viewModel.updateAccount(oldAccount, updatedAccount)
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