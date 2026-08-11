package com.example.inboxpriorities

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    // Manage Rules state
    private val _rules = MutableStateFlow<List<PriorityRule>>(MockDataRepository.mockRules)
    val rules: StateFlow<List<PriorityRule>> = _rules.asStateFlow()

    // Manage Emails state
    private val _rawEmails = MutableStateFlow<List<Email>>(MockDataRepository.mockEmails)
    val rawEmails: StateFlow<List<Email>> = _rawEmails.asStateFlow()

    fun toggleRule(toggledRule: PriorityRule, isEnabled: Boolean) {
        _rules.value = _rules.value.map { rule ->
            if (rule == toggledRule) rule.copy(isEnabled = isEnabled) else rule
        }
    }

    fun addRule(name: String, pattern: String, ruleType: RuleType, priority: PriorityLevel) {
        val newRule = PriorityRule(
            name = name,
            ruleType = ruleType,
            matchPattern = pattern,
            priorityLevel = priority,
            isEnabled = true
        )
        _rules.value = _rules.value + newRule
    }

    fun updateRule(oldRule: PriorityRule, updatedRule: PriorityRule) {
        _rules.value = _rules.value.map { if (it == oldRule) updatedRule else it }
    }

    fun deleteRule(ruleToDelete: PriorityRule) {
        _rules.value = _rules.value.filter { it != ruleToDelete }
    }
}