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

import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException

enum class AppTab {
    DASHBOARD,
    ACCOUNTS,
    RULES,
    HELP
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = RulesRepository(applicationContext)

                @Suppress("UNCHECKED_CAST")
                return MainViewModel(
                    application = application,
                    repository = repository
                ) as T
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

        // Intercept redirect if app was launched via the OAuth intent
        handleOAuthRedirect(intent)


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

                LaunchedEffect(Unit) {
                    viewModel.loadAllDefinedAccounts(context = this@MainActivity)
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
                            NavigationBarItem(
                                selected = selectedTab == AppTab.HELP,
                                onClick = { selectedTab = AppTab.HELP },
                                icon = { Text("❓", fontSize = 20.sp) },
                                label = { Text("Help") }
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
                                    viewModel = viewModel,
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
                                val ruleError by viewModel.ruleError.collectAsState()
                                RulesScreen(
                                    rules = rules,
                                    ruleError = ruleError,
                                    onClearError = { viewModel.clearRuleError() },
                                    onToggleRule = { rule, isEnabled ->
                                        viewModel.toggleRule(rule, isEnabled)
                                    },
                                    onAddRule = { name, pattern, ruleType, priority ->
                                        viewModel.addRule(name, pattern, ruleType, priority, null)
                                    },
                                    onUpdateRule = { oldRule, newRule ->
                                        viewModel.addRule(newRule.name, newRule.matchPattern, newRule.ruleType, newRule.priorityLevel, oldRule.id)
                                    },
                                    onDeleteRule = { rule ->
                                        viewModel.deleteRule(rule)
                                    }
                                )
                            }
                            AppTab.HELP -> {
                                HelpScreen(
                                    onContactSupportClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:support@inboxpriorities.com")
                                            putExtra(Intent.EXTRA_SUBJECT, "Inbox Priorities Support / Feedback")
                                        }
                                        try {
                                            this@MainActivity.startActivity(intent)
                                        } catch (e: ActivityNotFoundException) {
                                            // Handle case where no email app is available
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthRedirect(intent)
    }
    private fun handleOAuthRedirect(intent: Intent) {
        val uri = intent.data
        if (uri != null && uri.scheme == "inboxpriorities" && uri.host == "oauth2redirect") {
            val authorizationCode = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")

            if (!authorizationCode.isNullOrEmpty()) {
                intent.data = null
                viewModel.exchangeCodeForToken(authorizationCode)
            } else if (!error.isNullOrEmpty()) {
                android.util.Log.e("OAuth", "Authorization failed: $error")
            }
        }
    }
}