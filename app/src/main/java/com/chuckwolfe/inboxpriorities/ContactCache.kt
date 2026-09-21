package com.chuckwolfe.inboxpriorities

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

object ContactCache {
    private const val TAG = "Debug_ContactCache"

    // Maps account email -> set of lowercase contact email addresses
    private val cache = ConcurrentHashMap<String, Set<String>>()

    fun storeContacts(accountEmail: String, contacts: List<String>) {
        val normalizedKey = accountEmail.lowercase()
        val cleanedContacts = contacts.map { it.trim().lowercase() }.toSet()
        cache[normalizedKey] = cleanedContacts
        Log.d(TAG, "Stored ${cleanedContacts.size} contacts for account: $normalizedKey")
    }

    fun getContacts(accountEmail: String): Set<String> {
        val normalizedKey = accountEmail.lowercase()
        val contacts = cache[normalizedKey] ?: emptySet()
        Log.d(TAG, "Retrieved ${contacts.size} contacts for account: $normalizedKey")
        return contacts
    }

    fun isContact(accountEmail: String, senderEmail: String): Boolean {
        val contacts = getContacts(accountEmail)
        val cleanedSender = senderEmail.substringAfterLast("<").substringBefore(">").trim().lowercase()

        val matched = contacts.contains(cleanedSender) || contacts.any { cleanedSender.contains(it) }
        Log.d(TAG, "Checking sender '$cleanedSender' against cache for '$accountEmail'. Match found? $matched (Cache size: ${contacts.size})")

        return matched
    }

    /**
     * Loads device contacts into the cache for the given account.
     */
    fun loadDeviceContacts(context: Context, accountEmail: String) {
        val contactEmails = mutableSetOf<String>()
        val contentResolver = context.contentResolver

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            null,
            null,
            null
        )

        cursor?.use {
            val emailIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (it.moveToNext()) {
                if (emailIndex != -1) {
                    it.getString(emailIndex)?.let { email ->
                        if (email.isNotBlank()) {
                            contactEmails.add(email.trim().lowercase())
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Loaded ${contactEmails.size} total email addresses from device contacts.")
        storeContacts(accountEmail, contactEmails.toList())
    }

    fun clear() {
        cache.clear()
        Log.d(TAG, "Contact cache cleared.")
    }
}