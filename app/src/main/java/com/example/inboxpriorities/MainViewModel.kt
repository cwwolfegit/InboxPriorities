package com.example.inboxpriorities

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import android.content.Context

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainViewModel(
    application: Application,
    private val repository: RulesRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    // In-memory cache mapping email addresses to their active Google ID tokens
    private val activeGoogleTokens = mutableMapOf<String, String>()

    init {
        // Automatically restore saved tokens from storage on startup
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("token_") && value is String) {
                val email = key.removePrefix("token_")
                if (email.isNotBlank() && value.isNotBlank()) {
                    activeGoogleTokens[email] = value
                    android.util.Log.d("Debug_MainViewModel", "Restored saved token for $email from SharedPreferences")
                }
            }
        }
    }

    fun cacheGoogleToken(email: String, idToken: String) {
        android.util.Log.d("Debug_MainViewModel", "cacheGoogleToken called for email: '$email', token length: ${idToken.length}")
        if (email.isNotBlank() && idToken.isNotBlank()) {
            activeGoogleTokens[email] = idToken
            prefs.edit().putString("token_$email", idToken).apply()
            android.util.Log.d("Debug_MainViewModel", "Successfully cached and persisted token for $email")
        } else {
            android.util.Log.d("Debug_MainViewModel", "FAILED to cache token: email or token was blank.")
        }
    }

    // --- State Flows ---

    val rules: StateFlow<List<PriorityRule>> = repository.rulesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val accounts: StateFlow<List<EmailAccount>> = repository.accountsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _emails = MutableStateFlow<List<Email>>(emptyList())
    val emails: StateFlow<List<Email>> = _emails.asStateFlow()

    private val _unreadCounts = MutableStateFlow<Map<String, String>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, String>> = _unreadCounts.asStateFlow()
    private val _unreadTodayCounts = MutableStateFlow<Map<String, String>>(emptyMap())
    val unreadTodayCounts: StateFlow<Map<String, String>> = _unreadTodayCounts.asStateFlow()
    private val _accountErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val accountErrors: StateFlow<Map<String, String>> = _accountErrors.asStateFlow()

    // --- Priority Rules Functions ---

    fun toggleRule(ruleToToggle: PriorityRule, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedList = rules.value.map { rule ->
                if (rule.id == ruleToToggle.id) rule.copy(isEnabled = isEnabled) else rule
            }
            repository.saveRules(updatedList)
        }
    }

    fun addRule(name: String, pattern: String, ruleType: RuleType, priorityLevel: PriorityLevel) {
        viewModelScope.launch {
            val newRule = PriorityRule(
                id = UUID.randomUUID().toString(),
                name = name,
                matchPattern = pattern,
                ruleType = ruleType,
                priorityLevel = priorityLevel,
                isEnabled = true
            )
            val updatedList = rules.value + newRule
            repository.saveRules(updatedList)
        }
    }

    fun updateRule(oldRule: PriorityRule, newRule: PriorityRule) {
        viewModelScope.launch {
            val updatedList = rules.value.map { rule ->
                if (rule.id == oldRule.id) newRule else rule
            }
            repository.saveRules(updatedList)
        }
    }

    fun deleteRule(ruleToDelete: PriorityRule) {
        viewModelScope.launch {
            val updatedList = rules.value.filterNot { it.id == ruleToDelete.id }
            repository.saveRules(updatedList)
        }
    }

    // --- Account Management Functions ---

    fun addAccount(email: String, nickname: String, provider: AccountProvider) {
        viewModelScope.launch {
            // Check if the account is already in the list to prevent duplicates
            val existingAccount = accounts.value.find { it.emailAddress.equals(email, ignoreCase = true) }

            if (existingAccount == null) {
                val newAccount = EmailAccount(
                    id = UUID.randomUUID().toString(),
                    emailAddress = email,
                    nickname = nickname,
                    provider = provider
                )
                val updatedList = accounts.value + newAccount
                repository.saveAccounts(updatedList)
                android.util.Log.d("Debug_MainViewModel", "Added new account: $email")
            } else {
                android.util.Log.d("Debug_MainViewModel", "Account already exists: $email. Skipping duplicate insertion.")
                // Optional: Update the existing account's nickname or details instead if needed
            }
        }
    }

    fun removeAccount(accountId: String) {
        viewModelScope.launch {
            val updatedList = accounts.value.filterNot { it.id == accountId }
            repository.saveAccounts(updatedList)
        }
    }

    fun updateAccount(oldAccount: EmailAccount, updatedAccount: EmailAccount) {
        viewModelScope.launch {
            val updatedList = accounts.value.map { account ->
                if (account.id == oldAccount.id) updatedAccount else account
            }
            repository.saveAccounts(updatedList)
        }
    }

    // --- Gmail Sync Functions ---
    init {
        // Optional: If you want it to attempt loading once accounts are loaded,
        // or you can call it explicitly from your Activity/Fragment/Composable.
        android.util.Log.d("Debug_MainViewModelDebug", "MainViewModel initialized")
    }

    private var hasLoadedAccountsOnStartup = false
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    fun loadAllDefinedAccounts(context: Context) {
        if (hasLoadedAccountsOnStartup) return
        hasLoadedAccountsOnStartup = true

        _isLoading.value = true
        viewModelScope.launch {
            try {
                android.util.Log.d("Debug_MainViewModel", "--- START loadAllDefinedAccounts ---")
                android.util.Log.d(
                    "Debug_MainViewModel",
                    "Accounts in memory count: ${accounts.value.size}"
                )
                android.util.Log.d(
                    "Debug_MainViewModel",
                    "Active tokens currently cached: ${activeGoogleTokens.keys}"
                )

                for (account in accounts.value) {
                    android.util.Log.d(
                        "Debug_MainViewModel",
                        "Checking account: ${account.emailAddress}"
                    )

                    if (account.provider == AccountProvider.GMAIL) {
                        val token = activeGoogleTokens[account.emailAddress]
                        android.util.Log.d(
                            "Debug_MainViewModel",
                            "Token lookup for '${account.emailAddress}': found = ${!token.isNullOrBlank()}"
                        )

                        if (!token.isNullOrBlank()) {
                            val gmailRepo = GmailServiceRepository(
                                context = context,
                                emailAddress = account.emailAddress,
                                tokenProvider = { activeGoogleTokens[account.emailAddress] }
                            )

                            val unreadResult = gmailRepo.getUnreadInboxCount()
                            val unreadToday = gmailRepo.getUnreadTodayCount()

                            android.util.Log.d(
                                "Debug_MainViewModel",
                                "API Result for ${account.emailAddress} -> Count: ${unreadResult.first}, Plus: ${unreadResult.second}, Today: $unreadToday"
                            )

                            if (unreadResult.first == -1 || unreadToday == -1) {
                                // Flag this account with an error instead of crashing the count labels
                                _accountErrors.value = _accountErrors.value.toMutableMap().apply {
                                    put(account.emailAddress, "Sign-in Expired")
                                }
                            } else {
                                // Clear any previous error and update with clean numbers
                                _accountErrors.value = _accountErrors.value.toMutableMap().apply {
                                    remove(account.emailAddress)
                                }

                                val formattedCount = if (unreadResult.second) "${unreadResult.first}+" else "${unreadResult.first}"

                                _unreadCounts.value = _unreadCounts.value.toMutableMap().apply {
                                    put(account.emailAddress, formattedCount)
                                }
                                _unreadTodayCounts.value = _unreadTodayCounts.value.toMutableMap().apply {
                                    put(account.emailAddress, unreadToday.toString())
                                }
                            }
                        } else {
                            android.util.Log.d(
                                "Debug_MainViewModel",
                                "SKIPPED API call for ${account.emailAddress}: No cached token available at startup."
                            )
                        }
                    }
                }
            } finally {
                _isLoading.value = false
                android.util.Log.d("Debug_MainViewModel", "--- END loadAllDefinedAccounts ---")
            }
        }
    }

    // Cache map for GoogleSignInAccount objects
    private val activeGoogleAccounts = mutableMapOf<String, GoogleSignInAccount>()

    fun cacheGoogleAccount(account: GoogleSignInAccount) {
        val email = account.email
        // Ensure we only cache accounts that have been explicitly added to the app
        if (!email.isNullOrBlank() && accounts.value.any { it.emailAddress.equals(email, ignoreCase = true) }) {
            activeGoogleAccounts[email] = account
        }
    }

    fun cacheSession(email: String, idToken: String) {
        if (email.isNotBlank() && accounts.value.any { it.emailAddress.equals(email, ignoreCase = true) }) {
            activeGoogleAccounts[email] = GoogleSignInAccount.fromAccount(android.accounts.Account(email, "com.google"))
        }
    }

    private fun refreshDeviceAccountsCache(context: Context) {
        val manager = android.accounts.AccountManager.get(context)
        val googleAccounts = manager.getAccountsByType("com.google")
        val allowedEmails = accounts.value.map { it.emailAddress }

        for (acc in googleAccounts) {
            // Only cache if the device account matches an account explicitly added to the app
            if (allowedEmails.contains(acc.name) && !activeGoogleAccounts.containsKey(acc.name)) {
                activeGoogleAccounts[acc.name] = GoogleSignInAccount.fromAccount(acc)
                android.util.Log.d("Debug_MainViewModel", "Auto-mapped allowed account: ${acc.name}")
            }
        }
    }
    fun refreshCountsIfNeeded(context: Context) {
        // Automatically trigger account unread check if we haven't fetched them yet
        if (_unreadCounts.value.isEmpty() && accounts.value.isNotEmpty()) {
            loadAllDefinedAccounts(context)
        }
    }
}