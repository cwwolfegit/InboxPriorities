// TODO: Refactor deprecated GoogleSignIn to Credential Manager prior to production release
package com.example.inboxpriorities

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

class GmailAuthHelper(context: Context) {
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope("https://www.googleapis.com/auth/gmail.readonly"))
        .build()

    val signInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)
}