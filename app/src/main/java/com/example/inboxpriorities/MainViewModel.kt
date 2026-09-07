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

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers

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
    private val _ruleError = MutableStateFlow<String?>(null)
    val ruleError: StateFlow<String?> = _ruleError.asStateFlow()
    private val _priorityUnreadCounts = MutableStateFlow<Map<String, String>>(emptyMap())
    val priorityUnreadCounts: StateFlow<Map<String, String>> = _priorityUnreadCounts.asStateFlow()
    private val _priorityTodayCounts = MutableStateFlow<Map<String, String>>(emptyMap())
    val priorityTodayCounts: StateFlow<Map<String, String>> = _priorityTodayCounts.asStateFlow()
    private val _todayPriorityEmailsByAccount = MutableStateFlow<Map<String, List<Email>>>(emptyMap())
    val todayPriorityEmailsByAccount: StateFlow<Map<String, List<Email>>> = _todayPriorityEmailsByAccount.asStateFlow()
    // --- Priority Rules Functions ---

    fun toggleRule(ruleToToggle: PriorityRule, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedList = rules.value.map { rule ->
                if (rule.id == ruleToToggle.id) rule.copy(isEnabled = isEnabled) else rule
            }
            repository.saveRules(updatedList)
        }
    }

    fun clearRuleError() {
        _ruleError.value = null
    }

    fun addRule(name: String, pattern: String, ruleType: RuleType, priorityLevel: PriorityLevel, editingRuleId: String? = null): Boolean {
        val isDuplicate = rules.value.any { rule ->
            if (editingRuleId != null && rule.id == editingRuleId) return@any false

            rule.ruleType == ruleType &&
                    rule.matchPattern.equals(pattern.trim(), ignoreCase = true)
        }

        if (isDuplicate) {
            _ruleError.value = "A rule with this type and match pattern already exists."
            return false // Save blocked
        }

        _ruleError.value = null

        viewModelScope.launch {
            val updatedList = if (editingRuleId != null) {
                rules.value.map { rule ->
                    if (rule.id == editingRuleId) {
                        rule.copy(name = name, matchPattern = pattern, ruleType = ruleType, priorityLevel = priorityLevel)
                    } else {
                        rule
                    }
                }
            } else {
                val newRule = PriorityRule(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    matchPattern = pattern,
                    ruleType = ruleType,
                    priorityLevel = priorityLevel,
                    isEnabled = true
                )
                rules.value + newRule
            }
            repository.saveRules(updatedList)
        }
        return true // Save succeeded
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

                // 1. Launch all account fetches concurrently using async / awaitAll
                val deferredResults = accounts.value.map { account ->
                    async(Dispatchers.IO) {
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
                                val emailRepo: EmailRepository = when (account.provider) {
                                    AccountProvider.OUTLOOK -> OutlookEmailRepository()
                                    else -> GmailServiceRepository(
                                        context = context,
                                        emailAddress = account.emailAddress,
                                        tokenProvider = { activeGoogleTokens[account.emailAddress] }
                                    )
                                }

                                // Note: If Outlook doesn't use these specific unread count helpers yet,
                                // you can handle them per provider or add them to the interface later.
                                // For now, focusing on the core email fetch via the interface contract:

                                val unreadResult = (emailRepo as? GmailServiceRepository)?.getUnreadInboxCount() ?: Pair(0, false)
                                val unreadToday = (emailRepo as? GmailServiceRepository)?.getUnreadTodayCount() ?: 0

                                android.util.Log.d(
                                    "Debug_MainViewModel",
                                    "API Result for ${account.emailAddress} -> Count: ${unreadResult.first}, Plus: ${unreadResult.second}, Today: $unreadToday"
                                )

                                // Fetch recent emails
                                val recentEmails = emailRepo.fetchEmails(account.id)

                                if (unreadResult.first == -1 || unreadToday == -1) {
                                    AccountFetchResult(
                                        emailAddress = account.emailAddress,
                                        error = "Sign-in Expired",
                                        formattedCount = null,
                                        unreadTodayStr = null,
                                        totalPriorityStr = null,
                                        todayPriorityStr = null,
                                        sortedEmails = emptyList()
                                    )
                                } else {
                                    val applicableRules = rules.value.filter { rule ->
                                        rule.targetAccountId == null || rule.targetAccountId == account.id
                                    }

                                    val calendar = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }
                                    val startOfTodayMillis = calendar.timeInMillis

                                    var totalPriorityCount = 0
                                    var todayPriorityCount = 0
                                    val accountPriorityEmails = mutableListOf<Email>()

                                    for (email in recentEmails) {
                                        val (level, _) = PriorityEvaluator.evaluatePriority(email.sender, email.subject, applicableRules)

                                        if (level != PriorityLevel.NONE) {
                                            totalPriorityCount++
                                            accountPriorityEmails.add(email.copy(priorityLevel = level))

                                            if (email.timestamp >= startOfTodayMillis) {
                                                todayPriorityCount++
                                            }
                                        }
                                    }

                                    val formattedCount = if (unreadResult.second) "${unreadResult.first}+" else "${unreadResult.first}"
                                    val sortedEmails = if (accountPriorityEmails.isNotEmpty()) {
                                        accountPriorityEmails.sortedBy { email ->
                                            when (email.priorityLevel) {
                                                PriorityLevel.HIGH -> 0
                                                PriorityLevel.MEDIUM -> 1
                                                PriorityLevel.LOW -> 2
                                                PriorityLevel.NONE -> 3
                                            }
                                        }
                                    } else emptyList()

                                    AccountFetchResult(
                                        emailAddress = account.emailAddress,
                                        error = null,
                                        formattedCount = formattedCount,
                                        unreadTodayStr = unreadToday.toString(),
                                        totalPriorityStr = totalPriorityCount.toString(),
                                        todayPriorityStr = todayPriorityCount.toString(),
                                        sortedEmails = sortedEmails
                                    )
                                }
                            } else {
                                android.util.Log.d(
                                    "Debug_MainViewModel",
                                    "SKIPPED API call for ${account.emailAddress}: No cached token available at startup."
                                )
                                null
                            }
                        } else {
                            null
                        }
                    }
                }

                // 2. Await all background jobs concurrently
                val results = deferredResults.awaitAll()

                // 3. Aggregate results and update StateFlows safely
                val newErrors = _accountErrors.value.toMutableMap()
                val newUnread = _unreadCounts.value.toMutableMap()
                val newUnreadToday = _unreadTodayCounts.value.toMutableMap()
                val newPriorityUnread = _priorityUnreadCounts.value.toMutableMap()
                val newPriorityToday = _priorityTodayCounts.value.toMutableMap()
                val allAccountPriorityMap = mutableMapOf<String, List<Email>>()

                for (res in results) {
                    if (res == null) continue
                    if (res.error != null) {
                        newErrors[res.emailAddress] = res.error
                    } else {
                        newErrors.remove(res.emailAddress)
                        if (res.formattedCount != null) newUnread[res.emailAddress] = res.formattedCount
                        if (res.unreadTodayStr != null) newUnreadToday[res.emailAddress] = res.unreadTodayStr
                        if (res.totalPriorityStr != null) newPriorityUnread[res.emailAddress] = res.totalPriorityStr
                        if (res.todayPriorityStr != null) newPriorityToday[res.emailAddress] = res.todayPriorityStr
                        if (res.sortedEmails.isNotEmpty()) {
                            allAccountPriorityMap[res.emailAddress] = res.sortedEmails
                        }
                    }
                }

                _accountErrors.value = newErrors
                _unreadCounts.value = newUnread
                _unreadTodayCounts.value = newUnreadToday
                _priorityUnreadCounts.value = newPriorityUnread
                _priorityTodayCounts.value = newPriorityToday
                _todayPriorityEmailsByAccount.value = allAccountPriorityMap

            } finally {
                _isLoading.value = false
                android.util.Log.d("Debug_MainViewModel", "--- END loadAllDefinedAccounts ---")
            }
        }
    }

    // Helper data class for thread-safe result collection
    private data class AccountFetchResult(
        val emailAddress: String,
        val error: String?,
        val formattedCount: String?,
        val unreadTodayStr: String?,
        val totalPriorityStr: String?,
        val todayPriorityStr: String?,
        val sortedEmails: List<Email>
    )
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
    fun retryAccountAuth(email: String, newToken: String, context: Context) {
        if (email.isNotBlank() && newToken.isNotBlank()) {
            cacheGoogleToken(email, newToken)

            _accountErrors.value = _accountErrors.value.toMutableMap().apply {
                remove(email)
            }

            // Allow the accounts to reload by resetting the startup flag
            hasLoadedAccountsOnStartup = false

            viewModelScope.launch {
                loadAllDefinedAccounts(context)
            }
        }
    }
}