package com.polotech.inboxpriorities

import android.os.Build
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class DiagnosticEvent(
    val timestamp: String,
    val account: String,
    val operation: String,
    val errorType: String,
    val message: String
)

object DiagnosticLogger {
    private const val MAX_EVENTS = 30
    private val events = ArrayDeque<DiagnosticEvent>()
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Synchronized
    fun record(
        account: String,
        operation: String,
        errorType: String,
        message: String
    ) {
        if (events.size >= MAX_EVENTS) {
            events.removeFirst()
        }
        events.addLast(
            DiagnosticEvent(
                timestamp = LocalDateTime.now().format(timeFormatter),
                account = account,
                operation = operation,
                errorType = errorType,
                message = message
            )
        )
    }

    @Synchronized
    fun buildReport(
        appVersion: String,
        activeNoticeSummary: String? = null
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== Inbox Priorities Diagnostic Report ===")
        sb.appendLine("App Version: $appVersion")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})")
        if (!activeNoticeSummary.isNullOrBlank()) {
            sb.appendLine("Active Notice: $activeNoticeSummary")
        }
        sb.appendLine("--- Recent Events (${events.size}) ---")
        events.reversed().forEach { e ->
            sb.appendLine("[${e.timestamp}] ${e.account} | ${e.operation} | ${e.errorType}: ${e.message}")
        }
        return sb.toString()
    }
}