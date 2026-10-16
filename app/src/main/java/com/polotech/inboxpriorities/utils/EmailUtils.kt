package com.polotech.inboxpriorities.utils

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast

private const val TAG = "EmailUtils"

fun openEmailInbox(context: Context, senderEmail: String = "") {
    // Check if this is an Outlook account based on the email domain
    val isOutlook = senderEmail.contains("outlook", ignoreCase = true) ||
            senderEmail.contains("hotmail", ignoreCase = true) ||
            senderEmail.contains("live.com", ignoreCase = true)

    if (isOutlook) {
        Log.d(TAG, "Detected Outlook email. Attempting to launch Microsoft Outlook package directly.")
        val outlookLaunchIntent = context.packageManager.getLaunchIntentForPackage("com.microsoft.office.outlook")
        if (outlookLaunchIntent != null) {
            outlookLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(outlookLaunchIntent)
            Log.d(TAG, "Successfully launched Microsoft Outlook app directly.")
            return
        } else {
            Log.w(TAG, "Microsoft Outlook package not found or has no launch intent. Falling back to general app intent.")
        }
    }

    // Default intent behavior for other providers or if Outlook package launch wasn't found
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_APP_EMAIL)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
        Log.d(TAG, "Successfully launched default email app intent.")
    } catch (e: Exception) {
        Log.w(TAG, "Primary intent failed. Falling back to mailto:. Reason: ${e.localizedMessage}")

        // Fallback: Use generic mailto
        val fallbackIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(fallbackIntent)
            Log.d(TAG, "Successfully launched mailto: fallback intent.")
        } catch (e2: Exception) {
            Log.e(TAG, "Failed to launch any email app: ${e2.localizedMessage}")
            Toast.makeText(context, "No email app found on device", Toast.LENGTH_SHORT).show()
        }
    }
}