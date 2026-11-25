package com.polotech.inboxpriorities

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.polotech.inboxpriorities.ContactCache.isContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class AccountAuthNotice(
    val title: String,
    val reason: String,
    val howToFix: String,
    val technicalDetail: String? = null
)

class MainViewModel(
    application: Application,
    private val repository: RulesRepository
) : AndroidViewModel(application) {

    // --- SharedPreferences & Token Caches ---
    private val prefs = application.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

// --- State Flows (Declared first so they initialize before the init block) ---

    // Global in-memory pro state initialized from persistent storage
    private val _isPro = MutableStateFlow(prefs.getBoolean("is_pro_unlocked", false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    fun toggleTier() {
        val newState = !_isPro.value
        _isPro.value = newState
        prefs.edit().putBoolean("is_pro_unlocked", newState).apply()
        // Force a reload so emails re-evaluate with the new pro status immediately
        hasLoadedAccountsOnStartup = false
        loadAllDefinedAccounts(getApplication(), force = true)
    }

    // Global contact priority state initialized from persistent storage
    private val _contactPriority = MutableStateFlow(prefs.getString("contact_priority_level", "NONE") ?: "NONE")
    val contactPriority: StateFlow<String> = _contactPriority.asStateFlow()

    fun updateContactPriority(priority: String) {
        android.util.Log.d("NBX_Debug_ContactPriority", "MainViewModel updating priority to: $priority")
        _contactPriority.value = priority
        prefs.edit().putString("contact_priority_level", priority).apply()
        android.util.Log.d("NBX_Debug_ContactPriority", "Saved to SharedPreferences: ${prefs.getString("contact_priority_level", "NOT_FOUND")}")
    }

    val rules: StateFlow<List<PriorityRule>> = if (USE_MOCK_SCREENSHOT_DATA) {
        MutableStateFlow(
            listOf(
                PriorityRule(
                    id = "1",
                    name = "Important Client",
                    matchPattern = "client@example.com",
                    ruleType = RuleType.SENDER_CONTAINS,
                    priorityLevel = PriorityLevel.HIGH,
                    isEnabled = true
                ),
                PriorityRule(
                    id = "2",
                    name = "Project Updates",
                    matchPattern = "updates",
                    ruleType = RuleType.SUBJECT_CONTAINS,
                    priorityLevel = PriorityLevel.HIGH,
                    isEnabled = true
                ),
                PriorityRule(
                    id = "3",
                    name = "Billing Alerts",
                    matchPattern = "invoice",
                    ruleType = RuleType.SUBJECT_CONTAINS,
                    priorityLevel = PriorityLevel.HIGH,
                    isEnabled = true
                )
            )
        ).asStateFlow()
    } else {
        repository.rulesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    private val _accounts = MutableStateFlow<List<EmailAccount>>(emptyList())
    val accounts: StateFlow<List<EmailAccount>> = _accounts.asStateFlow()

    private val _emails = MutableStateFlow<List<Email>>(emptyList())
    val emails: StateFlow<List<Email>> = _emails.asStateFlow()

    private val _unreadCounts = MutableStateFlow<Map<String, String>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, String>> = _unreadCounts.asStateFlow()

    private val _unreadTodayCounts = MutableStateFlow<Map<String, String>>(emptyMap())
    val unreadTodayCounts: StateFlow<Map<String, String>> = _unreadTodayCounts.asStateFlow()

    private val _accountErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val accountErrors: StateFlow<Map<String, String>> = _accountErrors.asStateFlow()
    private val _accountAuthNotices = MutableStateFlow<Map<String, AccountAuthNotice>>(emptyMap())
    val accountAuthNotices: StateFlow<Map<String, AccountAuthNotice>> = _accountAuthNotices.asStateFlow()

    private val _ruleError = MutableStateFlow<String?>(null)
    val ruleError: StateFlow<String?> = _ruleError.asStateFlow()

    private val _accountError = MutableStateFlow<String?>(null)
    val accountError: StateFlow<String?> = _accountError.asStateFlow()

    private val _priorityUnreadCounts = MutableStateFlow<Map<String, String>>(emptyMap())
    val priorityUnreadCounts: StateFlow<Map<String, String>> = _priorityUnreadCounts.asStateFlow()

    private val _priorityTodayCounts = MutableStateFlow<Map<String, String>>(emptyMap())
    val priorityTodayCounts: StateFlow<Map<String, String>> = _priorityTodayCounts.asStateFlow()

    private val _todayPriorityEmailsByAccount = MutableStateFlow<Map<String, List<Email>>>(emptyMap())
    val todayPriorityEmailsByAccount: StateFlow<Map<String, List<Email>>> = _todayPriorityEmailsByAccount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val activeGoogleTokens = mutableMapOf<String, String>()
    private val activeOutlookTokens = mutableMapOf<String, String>()
    private val activeGoogleAccounts = mutableMapOf<String, GoogleSignInAccount>()

    private var hasLoadedAccountsOnStartup = false

    companion object {
        private const val USE_MOCK_SCREENSHOT_DATA = false
    }

    init {
        try {
            // Automatically restore saved tokens from storage on startup
            prefs.all.forEach { (key, value) ->
                if (key.startsWith("token_") && value is String) {
                    val email = key.removePrefix("token_")
                    if (email.isNotBlank() && value.isNotBlank()) {
                        if (email.endsWith("@outlook.com", ignoreCase = true)) {
                            activeOutlookTokens[email] = value
                            android.util.Log.d("Debug_MainViewModel", "Restored saved Outlook token for $email from SharedPreferences")
                        } else {
                            activeGoogleTokens[email] = value
                            android.util.Log.d("Debug_MainViewModel", "Restored saved Google token for $email from SharedPreferences")
                        }
                    }
                }
            }

            // Wait for Room database to emit the saved accounts before triggering fetches
            viewModelScope.launch {
                try {
                    if (USE_MOCK_SCREENSHOT_DATA) {
                        _accounts.value = MockDataRepository.accountDashboards.mapIndexed { index, dashboard ->
                            EmailAccount(
                                id = (index + 1).toString(),
                                emailAddress = dashboard.email,
                                nickname = dashboard.email,
                                provider = if (dashboard.provider.equals("Outlook", ignoreCase = true)) AccountProvider.OUTLOOK else AccountProvider.GMAIL
                            )
                        }
                    } else {
                        repository.accountsFlow.collect { accountList ->
                            _accounts.value = accountList
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Debug_MainViewModel", "Error collecting accounts flow", e)
                }
            }

            viewModelScope.launch {
                try {
                    refreshDeviceAccountsCache(getApplication())

                    accounts.collect { accountList ->
                        if (accountList.isNotEmpty() && !hasLoadedAccountsOnStartup) {
                            loadAllDefinedAccounts(getApplication())
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Debug_MainViewModel", "Error in accounts initialization coroutine", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Debug_MainViewModel", "Error in init block preference parsing", e)
        }

        android.util.Log.d("Debug_MainViewModelDebug", "MainViewModel initialized")
    }

    fun cacheGoogleToken(email: String, idToken: String) {
        android.util.Log.d("Debug_MainViewModel", "cacheGoogleToken called for email: '$email', token length:${idToken.length}")
        if (email.isNotBlank() && idToken.isNotBlank()) {
            activeGoogleTokens[email] = idToken
            prefs.edit().putString("token_$email", idToken).apply()
            android.util.Log.d("Debug_MainViewModel", "Successfully cached and persisted token for $email")
        } else {
            android.util.Log.d("Debug_MainViewModel", "FAILED to cache token: email or token was blank.")
        }
    }

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

    fun clearAccountError() {
        _accountError.value = null
    }

    fun addAccount(email: String, nickname: String, provider: AccountProvider): Boolean {
        val isDuplicate = accounts.value.any { it.emailAddress.equals(email, ignoreCase = true) }

        if (isDuplicate) {
            _accountError.value = "An account with this email address already exists."
            android.util.Log.d("Debug_MainViewModel", "Account already exists: $email. Skipping duplicate insertion.")
            return false // Save blocked
        }

        _accountError.value = null

        viewModelScope.launch {
            val newAccount = EmailAccount(
                id = UUID.randomUUID().toString(),
                emailAddress = email,
                nickname = nickname,
                provider = provider
            )
            val updatedList = accounts.value + newAccount
            repository.saveAccounts(updatedList)
            android.util.Log.d("Debug_MainViewModel", "Added new account: $email")
        }
        return true // Save succeeded
    }

    fun updateAccountOrder(newOrder: List<EmailAccount>) {
        viewModelScope.launch {
            repository.saveAccounts(newOrder)
        }
    }

    fun retryAccountAuth(email: String, newToken: String, context: Context) {
        cacheGoogleToken(email, newToken)
        loadAllDefinedAccounts(context, force = true)
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

    private fun buildAuthNoticeFromException(account: EmailAccount, e: Throwable): AccountAuthNotice? {
        if (e is AccountAuthException) {
            return AccountAuthNotice(
                title = e.title,
                reason = e.reason,
                howToFix = e.howToFix,
                technicalDetail = e.technicalDetail
            )
        }

        if (e is AuthExpiredException) {
            return null
        }

        val providerName = if (account.provider == AccountProvider.OUTLOOK) "Outlook" else "Gmail"
        val message = e.localizedMessage.orEmpty()
        return when {
            message.contains("403", ignoreCase = true) ||
                    message.contains("insufficientPermissions", ignoreCase = true) ||
                    message.contains("access_denied", ignoreCase = true) -> AccountAuthNotice(
                title = "OAuth Permission Issue",
                reason = "$providerName denied OAuth access for ${account.emailAddress}:${message.ifBlank { "Missing or ungranted scope." }}",
                howToFix = "Reconnect ${account.emailAddress} and approve$providerName read permission."
            )
            else -> AccountAuthNotice(
                title = "Account Sync Issue",
                reason = message.ifBlank { "Unable to verify sign-in or load mail for ${account.emailAddress}." },
                howToFix = "Check your connection and sign in again to ${account.emailAddress}."
            )
        }
    }

// --- Email Sync Functions ---

    fun loadAllDefinedAccounts(context: Context, force: Boolean = false) {

        if (USE_MOCK_SCREENSHOT_DATA) {
            android.util.Log.d("Debug_MainViewModel", "loadAllDefinedAccounts intercepted by mock mode.")
            _isLoading.value = true

            val mockAccountEntities = MockDataRepository.accountDashboards.mapIndexed { index, dashboard ->
                EmailAccount(
                    id = (index + 1).toString(),
                    emailAddress = dashboard.email,
                    nickname = dashboard.email,
                    provider = if (dashboard.provider.equals("Outlook", ignoreCase = true)) AccountProvider.OUTLOOK else AccountProvider.GMAIL
                )
            }
            _accounts.value = mockAccountEntities

            val newUnread = mutableMapOf<String, String>()
            val newUnreadToday = mutableMapOf<String, String>()
            val newPriorityUnread = mutableMapOf<String, String>()
            val newPriorityToday = mutableMapOf<String, String>()
            val allAccountPriorityMap = mutableMapOf<String, List<Email>>()

            for (dashboard in MockDataRepository.accountDashboards) {
                val emailKey = dashboard.email
                newUnread[emailKey] = dashboard.unreadTotalCount.toString()
                newUnreadToday[emailKey] = dashboard.unreadTodayCount.toString()
                newPriorityUnread[emailKey] = dashboard.priorityUnreadTotalCount.toString()
                newPriorityToday[emailKey] = dashboard.priorityUnreadTodayCount.toString()

                val mappedEmails = dashboard.emails.map { mock ->
                    Email(
                        id = mock.id,
                        sender = mock.sender,
                        subject = mock.subject,
                        body = mock.snippet,
                        timestamp = System.currentTimeMillis(),
                        priorityLevel = if (mock.isPriority) PriorityLevel.HIGH else PriorityLevel.NONE
                    )
                }
                if (mappedEmails.isNotEmpty()) {
                    allAccountPriorityMap[emailKey] = mappedEmails
                }
            }

            _unreadCounts.value = newUnread
            _unreadTodayCounts.value = newUnreadToday
            _priorityUnreadCounts.value = newPriorityUnread
            _priorityTodayCounts.value = newPriorityToday
            _todayPriorityEmailsByAccount.value = allAccountPriorityMap
            _accountAuthNotices.value = emptyMap()
            _isLoading.value = false
            hasLoadedAccountsOnStartup = true
            return
        }

        if (accounts.value.isEmpty()) {
            android.util.Log.d("Debug_MainViewModel", "loadAllDefinedAccounts deferred: accounts list is still empty.")
            hasLoadedAccountsOnStartup = false // Allow retry when database emits items
            return
        }

        if (hasLoadedAccountsOnStartup && !force) {
            android.util.Log.d("Debug_MainViewModel", "loadAllDefinedAccounts skipped: already loaded on startup.")
            return
        }
        hasLoadedAccountsOnStartup = true

        _isLoading.value = true
        viewModelScope.launch {
            try {
                android.util.Log.d("Debug_MainViewModel", "--- START loadAllDefinedAccounts ---")
                android.util.Log.d(
                    "Debug_MainViewModel",
                    "Accounts in memory count: ${accounts.value.size}, details:${accounts.value.map { it.emailAddress to it.provider }}"
                )
                android.util.Log.d(
                    "Debug_MainViewModel",
                    "Active Google tokens currently cached keys: ${activeGoogleTokens.keys}"
                )

                val prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                android.util.Log.d(
                    "Debug_MainViewModel",
                    "AuthPrefs all stored keys: ${prefs.all.keys}"
                )

                if (accounts.value.isEmpty()) {
                    android.util.Log.w(
                        "Debug_MainViewModel",
                        "WARNING: accounts.value is empty at startup! Check account database/persistence initialization."
                    )
                }

                val deferredResults = accounts.value.map { account ->
                    async(Dispatchers.IO) {
                        try {
                            android.util.Log.d(
                                "Debug_MainViewModel",
                                "Checking account: ${account.emailAddress} (ID: ${account.id}, Provider:${account.provider})"
                            )
                            val token = when (account.provider) {
                                AccountProvider.OUTLOOK -> prefs.getString("microsoft_access_token", null)
                                else -> activeGoogleTokens[account.emailAddress]
                            }

                            android.util.Log.d(
                                "Debug_MainViewModel",
                                "Token lookup for '${account.emailAddress}' (${account.provider}): found =${!token.isNullOrBlank()}"
                            )

                            if (token.isNullOrBlank()) {
                                val notice = if (account.provider == AccountProvider.OUTLOOK) {
                                    AccountAuthNotice(
                                        title = "Not Signed In",
                                        reason = "No saved Microsoft sign-in token was found for ${account.emailAddress}.",
                                        howToFix = "Sign in to ${account.emailAddress} to load Outlook counts."
                                    )
                                } else {
                                    AccountAuthNotice(
                                        title = "Not Signed In",
                                        reason = "No active Google OAuth token is available for ${account.emailAddress}.",
                                        howToFix = "Sign in with ${account.emailAddress} and grant Gmail read permission."
                                    )
                                }
                                android.util.Log.w("Debug_MainViewModel", "Missing token for ${account.emailAddress}:${notice.reason}")
                                return@async AccountFetchResult(
                                    emailAddress = account.emailAddress,
                                    error = notice.title,
                                    authNotice = notice,
                                    formattedCount = null,
                                    unreadTodayStr = null,
                                    totalPriorityStr = null,
                                    todayPriorityStr = null,
                                    sortedEmails = emptyList()
                                )
                            }

                            val emailRepo: EmailRepository = when (account.provider) {
                                AccountProvider.OUTLOOK -> OutlookEmailRepository(
                                    emailAddress = account.emailAddress,
                                    tokenProvider = {
                                        prefs.getString("microsoft_access_token", null)
                                    }
                                )
                                else -> GmailServiceRepository(
                                    context = context,
                                    emailAddress = account.emailAddress,
                                    tokenProvider = { token }
                                )
                            }

                            android.util.Log.d("Debug_MainViewModel", "Fetching unread counts for ${account.emailAddress}...")
                            val unreadResult = emailRepo.getUnreadInboxCount()
                            val unreadToday = emailRepo.getUnreadTodayCount()

                            android.util.Log.d(
                                "Debug_MainViewModel",
                                "API Result for ${account.emailAddress} -> Count:${unreadResult.first}, Plus: ${unreadResult.second}, Today:$unreadToday"
                            )

                            val recentEmails = emailRepo.fetchEmails(account.id)
                            android.util.Log.d("Debug_MainViewModel", "Fetched ${recentEmails.size} recent emails for${account.emailAddress}")

                            if (unreadResult.first == -1 || unreadToday == -1) {
                                val providerLabel = if (account.provider == AccountProvider.OUTLOOK) "Outlook" else "Gmail"
                                android.util.Log.w("Debug_MainViewModel", "Invalid unread count (-1) detected for ${account.emailAddress} ($providerLabel)")
                                AccountFetchResult(
                                    emailAddress = account.emailAddress,
                                    error = "Tap to Sign-in!",
                                    signInPrompt = "Tap to Sign-in!",
                                    authNotice = null,
                                    formattedCount = null,
                                    unreadTodayStr = null,
                                    totalPriorityStr = null,
                                    todayPriorityStr = null,
                                    sortedEmails = emptyList()
                                )
                            } else {
                                // Load device contacts into cache so Contacts setting can match successfully
                                ContactCache.loadDeviceContacts(context, account.emailAddress)

                                val applicableRules = rules.value.filter { rule ->
                                    android.util.Log.d("Debug_MainViewModel", "Rule '${rule.name}' targetAccountId: ${rule.targetAccountId}, current account.id:${account.id}")
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

                                Log.d("isPro_MainViewModel", "Evaluating with isPro.value: ${isPro.value}")
                                for (email in recentEmails) {
                                    val (level, _) = PriorityEvaluator.evaluatePriority(
                                        sender = email.sender,
                                        subject = email.subject,
                                        rules = applicableRules,
                                        isPro = isPro.value,
                                        contactPriority = contactPriority.value,
                                        isContact = { cleanedEmail ->
                                            isContact(account.emailAddress, cleanedEmail)
                                        }
                                    )

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
                                    authNotice = null,
                                    formattedCount = formattedCount,
                                    unreadTodayStr = unreadToday.toString(),
                                    totalPriorityStr = totalPriorityCount.toString(),
                                    todayPriorityStr = todayPriorityCount.toString(),
                                    sortedEmails = sortedEmails
                                )
                            }
                        } catch (e: Exception) {
                            val notice = buildAuthNoticeFromException(account, e)
                            val isSimpleSignIn = (e is AuthExpiredException && e !is AccountAuthException)
                            android.util.Log.w(
                                "Debug_MainViewModel",
                                "Account issue for ${account.emailAddress}: ${notice?.title ?: "Tap to Sign-in!"} - ${notice?.reason ?: e.message}",
                                e
                            )
                            AccountFetchResult(
                                emailAddress = account.emailAddress,
                                error = if (isSimpleSignIn) "Tap to Sign-in!" else notice?.title,
                                signInPrompt = if (isSimpleSignIn) "Tap to Sign-in!" else null,
                                authNotice = notice,
                                formattedCount = null,
                                unreadTodayStr = null,
                                totalPriorityStr = null,
                                todayPriorityStr = null,
                                sortedEmails = emptyList()
                            )
                        }
                    }
                }

                val results = deferredResults.awaitAll()
                android.util.Log.d("Debug_MainViewModel", "All account fetch jobs completed. Results count: ${results.size}")

                val newErrors = _accountErrors.value.toMutableMap()
                val newNotices = _accountAuthNotices.value.toMutableMap()
                val newUnread = _unreadCounts.value.toMutableMap()
                val newUnreadToday = _unreadTodayCounts.value.toMutableMap()
                val newPriorityUnread = _priorityUnreadCounts.value.toMutableMap()
                val newPriorityToday = _priorityTodayCounts.value.toMutableMap()
                val allAccountPriorityMap = mutableMapOf<String, List<Email>>()

                for (res in results) {
                    if (res == null) {
                        android.util.Log.d("Debug_MainViewModel", "Skipping null result item.")
                        continue
                    }
                    if (res.error != null || res.authNotice != null || res.signInPrompt != null) {
                        android.util.Log.d("Debug_MainViewModel", "Result notice for ${res.emailAddress}:${res.authNotice?.reason ?: res.signInPrompt ?: res.error}")
                        if (res.signInPrompt != null) {
                            newErrors[res.emailAddress] = res.signInPrompt
                            newNotices.remove(res.emailAddress)
                        } else {
                            if (res.error != null) newErrors[res.emailAddress] = res.error
                            if (res.authNotice != null) newNotices[res.emailAddress] = res.authNotice
                        }
                    } else {
                        newErrors.remove(res.emailAddress)
                        newNotices.remove(res.emailAddress)
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
                _accountAuthNotices.value = newNotices
                _unreadCounts.value = newUnread
                _unreadTodayCounts.value = newUnreadToday
                _priorityUnreadCounts.value = newPriorityUnread
                _priorityTodayCounts.value = newPriorityToday
                _todayPriorityEmailsByAccount.value = allAccountPriorityMap
                android.util.Log.d("Debug_MainViewModel", "StateFlows successfully updated after account load.")

            } catch (e: Exception) {
                android.util.Log.e("Debug_MainViewModel", "Fatal exception in loadAllDefinedAccounts coroutine", e)
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
        val signInPrompt: String? = null,
        val authNotice: AccountAuthNotice?,
        val formattedCount: String?,
        val unreadTodayStr: String?,
        val totalPriorityStr: String?,
        val todayPriorityStr: String?,
        val sortedEmails: List<Email>
    )

    fun cacheGoogleAccount(account: GoogleSignInAccount) {
        val email = account.email
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
            if (allowedEmails.contains(acc.name) && !activeGoogleAccounts.containsKey(acc.name)) {
                activeGoogleAccounts[acc.name] = GoogleSignInAccount.fromAccount(acc)
                android.util.Log.d("Debug_MainViewModel", "Auto-mapped allowed account: ${acc.name}")
            }
        }
    }


}