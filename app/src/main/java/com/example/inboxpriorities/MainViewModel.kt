package com.example.inboxpriorities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(
    private val repository: RulesRepository,
    private val accountRepository: AccountRepository = AccountRepository()
) : ViewModel() {

    // --- State Flows ---

    val rules: StateFlow<List<PriorityRule>> = repository.rulesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val accounts: StateFlow<List<EmailAccount>> = accountRepository.accountsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    fun addAccount(email: String, provider: AccountProvider) {
        val newAccount = EmailAccount(
            emailAddress = email,
            provider = provider
        )
        accountRepository.addAccount(newAccount)
    }

    fun removeAccount(accountId: String) {
        accountRepository.removeAccount(accountId)
    }
}