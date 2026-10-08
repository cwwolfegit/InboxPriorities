package com.chuckwolfe.inboxpriorities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FaqItem(val question: String, val answer: String)

val faqList = listOf(
    FaqItem(
        question = "Q: How does Inbox Priorities handle my credentials and email data securely?",
        answer = "A: Inbox Priorities uses secure OAuth authentication protocols directly through your email providers, ensuring your credentials are never stored locally."
    ),
    FaqItem(
        question = "Q: Does Inbox Priorities collect any personal data?",
        answer = "A: No, Inbox Priorities processes your unread messages locally, and via secure provider APIs, without collecting, storing, or selling your personal data or email contents on external servers."
    ),
    FaqItem(
        question = "Q: What permissions does Inbox Priorities require to access my email?",
        answer = "A: Inbox Priorities requests only the read-only or scoped permissions necessary via OAuth to retrieve unread message headers and snippets for your priority dashboard, without allowing Inbox Priorities to delete or alter your emails."
    ),
    FaqItem(
        question = "Q: Does Inbox Priorities store my account passwords?",
        answer = "A: No. Inbox Priorities maintains a lightweight architecture by utilizing your device's native mail clients and system intents directly, meaning your credentials stay securely inside your chosen email apps."
    ),
    FaqItem(
        question = "Q: Are my custom rules and app preferences stored securely?",
        answer = "A: Yes, all local custom rules, filters, and dashboard preferences are saved securely in your device's local storage."
    ),
    FaqItem(
        question = "Q: Is there a limit to the number of unread emails that Inbox Priorities will check?",
        answer = "A: Yes, Inbox Priorities checks up to 201 unread emails in total and then processes the first 50 unread messages specifically for priority counts and categorization."
    ),
    FaqItem(
        question = "Q: How many email accounts can I add?",
        answer = "A: In the free tier, one email account is supported. This limit is removed if you upgrade to the pro tier."
    ),
    FaqItem(
        question = "Q: How many priority rules can I add?",
        answer = "A: In the free tier, you can add one priority rule, with unlimited rules available in the pro tier."
    ),
    FaqItem(
        question = "Q: How do I add or remove an email account?",
        answer = "A: Select \"Accounts\" from the menu. In the free tier, if you have not reached the one-account limit, click \"Add Account\" and enter the required details. Use the edit or delete icons to modify or remove an existing account."
    ),
    FaqItem(
        question = "Q: How do I add or remove a priority rule?",
        answer = "A: Select \"Rules\" from the menu. In the free tier, if you have not reached the one-rule limit, click \"Add Rule\" and enter the required details. Use the edit, delete, or \"active\" switch to modify, remove, or toggle the rule between active and inactive states."
    ),
    FaqItem(
        question = "Q: How do priority rules work?",
        answer = "A: The match pattern is evaluated alongside the rule type setting (Sender or Subject). For Sender rules, a match occurs when the sender's email contains the match pattern. For Subject rules, a match occurs when the email subject contains the match pattern. When a match is found, the priority level is assigned and counted; otherwise, the email is not designated as a priority message."
    ),
    FaqItem(
        question = "Q: What happens if my email sign-in or session expires?",
        answer = "A: If your provider session or OAuth token expires, Inbox Priorities will prompt you to re-authenticate securely through your account settings to resume fetching unread messages."
    ),
    FaqItem(
        question = "Q: How do I view my categorized priority emails on the dashboard?",
        answer = "A: Tap or expand any summary section on your dashboard to view the full list of identified priority emails, along with their headers and snippets.  Tap the envelope to open your native email application."
    ),
    FaqItem(
        question = "Q: What happens when I tap the envelope in the expanded account card on the dashboard?",
        answer = "A: Tapping the envelope from the account card directly launches your native email application (such as Microsoft Outlook for Outlook accounts or Gmail for Google accounts) straight to your inbox, allowing you to easily view, read, and respond to your messages."
    ),
    FaqItem(
        question = "Q: Why doesn't clicking the envelope always open the exact/correct inbox directly?",
        answer = "A: Inbox Priorities successfully launches the correct email provider application, but Android requires you to select or switch to your specific account inbox within your native email application."
    ),
    FaqItem(
        question = "Q: Why can I still open my inbox using the envelope icon if my account shows a sign-in error?",
        answer = "A: The envelope icon acts as a direct shortcut to your your native email application. Even if your in-app sync token has expired and requires re-authentication, you can still use the envelope to jump straight into your email client's inbox."
    ),
    FaqItem(
        question = "Q: Can I reply to emails directly inside Inbox Priorities?",
        answer = "A: No. Inbox Priorities is designed as a lightweight, non-invasive dashboard. When you tap an email, it seamlessly hands off control to your native email app so you can manage your messages securely within your trusted email client."
    ),
    FaqItem(
        question = "Q: What email providers are supported?",
        answer = "A: Gmail and Outlook."
    ),
    FaqItem(
        question = "Q: Can I use both Gmail and Outlook with Inbox Priorities?",
        answer = "A: Yes. You can either configure your preferred mail app to act as a Unified Inbox (adding both your Google and Microsoft accounts into a single app like Gmail or Outlook), or clear your device's default app settings under Android Settings > Apps > [App Name] > Open by default to choose your client dynamically each time."
    ),
    FaqItem(
        question = "Q: What are the minimum system requirements to run Inbox Priorities?",
        answer = "A: Inbox Priorities requires Android 8.0 (API level 26) or higher, a standard physical smartphone, a minimum of 2GB of RAM, and an active internet connection (either Wi-Fi or cellular data) to communicate securely with the email APIs."
    ),
    FaqItem(
        question = "Q: Why don't my Inbox Priorities unread gmail message counts match my main web inbox count?",
        answer = "A: Inbox Priorities focuses strictly on your Primary inbox tab (category:primary) to filter out promotional and social clutter. Your main web inbox displays an aggregate count across all tabs, which usually results in a higher total count. To query your emails like Inbox Priorities, search your emails using this filter: label:unread category:primary"
    ),
    FaqItem(
        question = "Q: What does Inbox Priorities's unread count actually measure?",
        answer = "A: It tracks unread threads and messages located specifically in your Primary inbox so you can focus on direct conversations and important emails without the noise."
    ),
    FaqItem(
        question = "Q: Can I use my Gmail and Outlook contacts to prioritize emails?",
        answer = "A: Yes, the pro tier allows you to do this by adding a special priority rule using \"@Contacts\" as the rule name. Any email address found in your contacts list will be prioritized. You can override the \"@Contacts\" priority by setting up specific priority rules for specific email addresses."
    ),
    FaqItem(
        question = "Q: How does Inbox Priorities recognize my contacts?",
        answer = "A: Inbox Priorities evaluates rules against your Android device's central address book (Contacts)."
    ),
    FaqItem(
        question = "Q: Do I need to sync my Gmail and Outlook contacts to my phone?",
        answer = "A: Yes. Because Inbox Priorities reads from your device's native address book, your contacts need to be synced down to your phone's storage:\n\n" +
                "• Google Contacts: Usually syncs automatically with your Android device login.\n\n" +
                "• Outlook Contacts: If you use the Microsoft Outlook app, you can ensure your contacts are visible to the device by opening Outlook, going to your account settings, and turning Sync Contacts ON. Alternatively, you can add your Outlook account directly under your Android device's Settings > Passwords & accounts and ensure contact synchronization is enabled."
    )
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onContactSupportClick: () -> Unit
) {
    var isFaqExpanded by remember { mutableStateOf(false) }
    var faqSearchQuery by remember { mutableStateOf("") }

    // Filter FAQ list based on search query matching question or answer
    val filteredFaqList = remember(faqSearchQuery) {
        if (faqSearchQuery.isBlank()) {
            faqList
        } else {
            faqList.filter {
                it.question.contains(faqSearchQuery, ignoreCase = true) ||
                        it.answer.contains(faqSearchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FAQ Expandable Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { isFaqExpanded = !isFaqExpanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Frequently Asked Questions", style = MaterialTheme.typography.titleMedium)
                            Icon(
                                imageVector = if (isFaqExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle FAQ"
                            )
                        }

                        if (isFaqExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Search bar inside the expanded FAQ card
                            OutlinedTextField(
                                value = faqSearchQuery,
                                onValueChange = { faqSearchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                placeholder = { Text("Search FAQ...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search FAQ Icon"
                                    )
                                },
                                trailingIcon = {
                                    if (faqSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { faqSearchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear Search"
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            if (filteredFaqList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No FAQ matches \"$faqSearchQuery\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                filteredFaqList.forEach { item ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = item.question,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.answer,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Contact Us Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onContactSupportClick
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Contact Us", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Send feedback or report an issue", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(imageVector = Icons.Default.Email, contentDescription = "Email Support")
                    }
                }
            }

            // About Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "About Inbox Priorities", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        //Text(text = "Version 1.0.10")
                        Text(text = "Inbox Priorities lets you monitor your email inboxes for important messages using customizable sender and subject rules. The Dashboard displays unread priority-message counts for each configured inbox, along with total unread-message counts. Expand an inbox to preview its priority emails, then click any message to open it in your email client, where you can read and respond.")                    }
                }
            }
        }
    }
}