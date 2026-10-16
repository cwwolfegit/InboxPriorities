package     com.polotech.inboxpriorities

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption

class GoogleAuthHelper(val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val authorizationClient: AuthorizationClient = Identity.getAuthorizationClient(context)

    private val webClientId = "927625541886-v96u4nnfrtdh38tlfo2cg4fccdr182qo.apps.googleusercontent.com"

    fun createCredentialRequest(): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    // Request OAuth Access Tokens for reading Gmail and Contacts
    fun createAuthorizationRequest(email: String): AuthorizationRequest {
        val gmailScope = Scope("https://www.googleapis.com/auth/gmail.readonly")
        val contactsScope = Scope("https://www.googleapis.com/auth/contacts.readonly")
        val account = android.accounts.Account(email, "com.google")
        return AuthorizationRequest.Builder()
            .setRequestedScopes(listOf(gmailScope, contactsScope))
            .setAccount(account)
            .build()
    }

    val manager: CredentialManager
        get() = credentialManager

    val authClient: AuthorizationClient
        get() = authorizationClient
}