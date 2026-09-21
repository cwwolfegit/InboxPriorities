package com.chuckwolfe.inboxpriorities

import java.util.concurrent.ConcurrentHashMap

object ContactCache {
    // Maps account email -> set of lowercase contact email addresses
    private val cache = ConcurrentHashMap<String, Set<String>>()

    fun storeContacts(accountEmail: String, contacts: List<String>) {
        cache[accountEmail.lowercase()] = contacts.map { it.trim().lowercase() }.toSet()
    }

    fun getContacts(accountEmail: String): Set<String> {
        return cache[accountEmail.lowercase()] ?: emptySet()
    }

    fun isContact(accountEmail: String, senderEmail: String): Boolean {
        val contacts = getContacts(accountEmail)
        // Check exact match or if sender email contains the contact address
        val cleanedSender = senderEmail.substringAfterLast("<").substringBefore(">").trim().lowercase()
        return contacts.contains(cleanedSender) || contacts.any { cleanedSender.contains(it) }
    }

    fun clear() {
        cache.clear()
    }
}