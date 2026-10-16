package com.polotech.inboxpriorities

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
import com.polotech.inboxpriorities.ui.theme.InboxPrioritiesTheme

import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException
import android.widget.Toast

import kotlinx.coroutines.launch

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

    // Initialize the BillingViewModel using the activity's ViewModel provider
    private val billingViewModel: BillingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        println("DEBUG_START: Entering onCreate()")
        super.onCreate(savedInstanceState)

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
                val contactPriority by viewModel.contactPriority.collectAsStateWithLifecycle()

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
                                    unreadTodayCounts = unreadTodayCounts,
                                    contactPriority = contactPriority
                                )
                            }
                            AppTab.ACCOUNTS -> {
                                val accountError by viewModel.accountError.collectAsState()
                                AccountsScreen(
                                    viewModel = viewModel,
                                    billingViewModel = billingViewModel,
                                    accounts = accounts,
                                    accountError = accountError,
                                    onClearError = { viewModel.clearAccountError() },
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
                                    billingViewModel = billingViewModel,
                                    rules = rules,
                                    ruleError = ruleError,
                                    currentContactPriority = contactPriority,
                                    onContactPriorityChanged = { newPriority ->
                                        viewModel.updateContactPriority(newPriority)
                                    },
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
                                            data = Uri.parse("mailto:InboxPriorities@outlook.com")
                                            putExtra(Intent.EXTRA_SUBJECT, "Inbox Priorities Support / Feedback")
                                        }
                                        try {
                                            this@MainActivity.startActivity(intent)
                                        } catch (e: ActivityNotFoundException) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "No email app found on this device.",
                                                Toast.LENGTH_SHORT
                                            ).show()
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
        if (uri != null && uri.scheme == "com.polotech.inboxpriorities" && uri.host == "oauth2redirect") {
            val error = uri.getQueryParameter("error")
            if (!error.isNullOrEmpty()) {
                android.util.Log.e("OAuth", "Authorization failed: $error")
                Toast.makeText(this, "Sign-in failed: $error", Toast.LENGTH_LONG).show()
                return
            }

            val code = uri.getQueryParameter("code")
            if (!code.isNullOrEmpty()) {
                android.util.Log.d("OAuth", "Received authorization code, exchanging for token...")

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val prefs = getSharedPreferences("AuthPrefs", android.content.Context.MODE_PRIVATE)
                        val codeVerifier = prefs.getString("code_verifier", null) ?: MicrosoftAuthHelper.getSavedVerifier()

                        if (codeVerifier.isNullOrEmpty()) {
                            android.util.Log.e("OAuth", "Code verifier is missing!")
                            return@launch
                        }

                        val client = okhttp3.OkHttpClient()
                        val requestBody = okhttp3.FormBody.Builder()
                            .add("client_id", "6c306dd0-6ac1-4811-8af8-578861b2c5bb")
                            .add("scope", "https://graph.microsoft.com/Mail.Read offline_access")
                            .add("code", code)
                            .add("redirect_uri", "com.polotech.inboxpriorities://oauth2redirect")
                            .add("grant_type", "authorization_code")
                            .add("code_verifier", codeVerifier)
                            .build()

                        val request = okhttp3.Request.Builder()
                            .url("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                            .post(requestBody)
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                val errorBody = response.body?.string()
                                android.util.Log.e("OAuth", "Token exchange failed: ${response.code} - $errorBody")
                                return@use
                            }

                            val responseString = response.body?.string()
                            val jsonObject = com.google.gson.Gson().fromJson(responseString, com.google.gson.JsonObject::class.java)
                            val accessToken = jsonObject.get("access_token")?.asString

                            if (!accessToken.isNullOrEmpty()) {
                                prefs.edit().putString("microsoft_access_token", accessToken).apply()
                                android.util.Log.d("OAuth", "Successfully acquired and saved microsoft_access_token!")

                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(this@MainActivity, "Outlook signed in successfully!", Toast.LENGTH_SHORT).show()
                                    viewModel.loadAllDefinedAccounts(this@MainActivity, force = true)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OAuth", "Exception during token exchange", e)
                    }
                }
            }
        }
    }
}