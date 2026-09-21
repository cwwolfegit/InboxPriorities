package     com.chuckwolfe.inboxpriorities

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountRepository {

    // Initial default accounts for testing
    private val _accountsFlow = MutableStateFlow(getDefaultAccounts())
    val accountsFlow: Flow<List<EmailAccount>> = _accountsFlow.asStateFlow()

    fun addAccount(account: EmailAccount) {
        _accountsFlow.value = _accountsFlow.value + account
    }

    fun removeAccount(accountId: String) {
        _accountsFlow.value = _accountsFlow.value.filterNot { it.id == accountId }
    }

    private fun getDefaultAccounts(): List<EmailAccount> {
        return listOf(
            EmailAccount(
                emailAddress = "work@company.com",
                nickname = "Work",
                provider = AccountProvider.GMAIL,
                unreadToday = 12,
                unreadTotal = 110,
                priorityToday = 5,
                priorityTotal = 18
            ),
            EmailAccount(
                emailAddress = "personal@outlook.com",
                nickname = "Personal",
                provider = AccountProvider.OUTLOOK,
                unreadToday = 8,
                unreadTotal = 46,
                priorityToday = 3,
                priorityTotal = 3
            )
        )
    }
}