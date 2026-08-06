package com.example.inboxpriorities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

            // --- TEMPORARY TEST CALLS ---
            //val test1 = PriorityEvaluator.evaluatePriority("boss@amazon.com", "Project update")
            //val test2 = PriorityEvaluator.evaluatePriority("someone@gov.us", "Tax info")
            //val test3 = PriorityEvaluator.evaluatePriority("friend@gmail.com", "URGENT question")
            //val test4 = PriorityEvaluator.evaluatePriority("spam@random.com", "Weekly newsletter")

            //Log.d("PriorityTest", "Amazon email priority: $test1")
            //Log.d("PriorityTest", "Gov email priority: $test2")
            //Log.d("PriorityTest", "Urgent subject priority: $test3")
            //Log.d("PriorityTest", "Random email priority: $test4")
            // ----------------------------

        // Add this temporarily in onCreate() in MainActivity.kt
        MockDataRepository.getEvaluatedEmails().forEach { email ->
            Log.d("PriorityCheck", "'${email.subject}' from ${email.sender} -> Priority: ${email.priorityLevel}")
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen()
                }
            }
        }
    }
}