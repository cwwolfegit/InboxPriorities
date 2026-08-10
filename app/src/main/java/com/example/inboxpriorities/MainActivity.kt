package com.example.inboxpriorities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

// 1. Fetch evaluated mock emails
        val mockEmails = MockDataRepository.getEvaluatedEmails()

        setContent {
            MaterialTheme {
                Surface {
                    // 2. Render the Dashboard Screen with data
                    DashboardScreen(emails = mockEmails)
                }
            }
        }
    }
}