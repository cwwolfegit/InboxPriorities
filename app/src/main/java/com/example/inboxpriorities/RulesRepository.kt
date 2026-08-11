package com.example.inboxpriorities

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

// Extension property to initialize DataStore instance
private val Context.dataStore by preferencesDataStore(name = "inbox_priorities_prefs")

class RulesRepository(private val context: Context) {

    private val RULES_KEY = stringPreferencesKey("saved_priority_rules")

    // Continuous flow of rules from DataStore
    val rulesFlow: Flow<List<PriorityRule>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[RULES_KEY]
            if (!jsonString.isNullOrEmpty()) {
                try {
                    Json.decodeFromString<List<PriorityRule>>(jsonString)
                } catch (e: Exception) {
                    getDefaultRules()
                }
            } else {
                getDefaultRules()
            }
        }

    // Save full list of rules as a JSON string
    suspend fun saveRules(rules: List<PriorityRule>) {
        val jsonString = Json.encodeToString(rules)
        context.dataStore.edit { preferences ->
            preferences[RULES_KEY] = jsonString
        }
    }

    // Default starter rules using named arguments to prevent positional mismatch
    private fun getDefaultRules(): List<PriorityRule> {
        return listOf(
            PriorityRule(
                id = UUID.randomUUID().toString(),
                name = "Direct Manager",
                ruleType = RuleType.SENDER_CONTAINS,
                matchPattern = "boss@company.com",
                priorityLevel = PriorityLevel.HIGH,
                isEnabled = true
            ),
            PriorityRule(
                id = UUID.randomUUID().toString(),
                name = "Key Client Domain",
                ruleType = RuleType.SENDER_CONTAINS,
                matchPattern = "clientcorp.com",
                priorityLevel = PriorityLevel.HIGH,
                isEnabled = true
            ),
            PriorityRule(
                id = UUID.randomUUID().toString(),
                name = "Urgent Subject Line",
                ruleType = RuleType.SUBJECT_CONTAINS,
                matchPattern = "URGENT",
                priorityLevel = PriorityLevel.HIGH,
                isEnabled = true
            ),
            PriorityRule(
                id = UUID.randomUUID().toString(),
                name = "Invoices & Billing",
                ruleType = RuleType.SUBJECT_CONTAINS,
                matchPattern = "Invoice",
                priorityLevel = PriorityLevel.MEDIUM,
                isEnabled = true
            ),
            PriorityRule(
                id = UUID.randomUUID().toString(),
                name = "Old Vendor Alert",
                ruleType = RuleType.SENDER_CONTAINS,
                matchPattern = "support@oldvendor.com",
                priorityLevel = PriorityLevel.LOW,
                isEnabled = false
            )
        )
    }
}