package com.example.inboxpriorities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import android.content.Context

class MainViewModel(
    private val repository: RulesRepository
) : ViewModel() {

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

    // Changed to store formatted strings (e.g., "15" or "200+") so the UI can display them cleanly
    private val _unreadCounts = MutableStateFlow<Map<String, String>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, String>> = _unreadCounts.asStateFlow()

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
            val newAccount = EmailAccount(
                id = UUID.randomUUID().toString(),
                emailAddress = email,
                nickname = nickname,
                provider = provider
            )
            val updatedList = accounts.value + newAccount
            repository.saveAccounts(updatedList)
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

    fun loadGmailMessages(context: Context, account: GoogleSignInAccount) {
        viewModelScope.launch {
            val gmailRepo = GmailServiceRepository(context, account)

            // Fetch recent messages for display
            val fetchedEmails = gmailRepo.fetchRecentEmails(maxResults = 10)
            _emails.value = fetchedEmails

            // Fetch capped unread count pair (count, isExceeded)
            val unreadResult = gmailRepo.getUnreadInboxCount()
            val formattedCount = if (unreadResult.second) {
                "${unreadResult.first}+"
            } else {
                "${unreadResult.first}"
            }

            account.email?.let { emailAddress ->
                _unreadCounts.value = _unreadCounts.value.toMutableMap().apply {
                    put(emailAddress, formattedCount)
                }
            }
        }
    }

    fun checkAndLoadExistingAccount(context: Context) {
        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastSignedInAccount != null) {
            loadGmailMessages(context, lastSignedInAccount)
        }
    }
}