package com.polotech.inboxpriorities

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    fun getAppVersionLabel(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val versionName = packageInfo.versionName ?: "unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "$versionName ($versionCode)"
        } catch (_: Exception) {
            "unknown"
        }
    }

    fun getNetworkSummary(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "Unknown"
            val network = cm.activeNetwork ?: return "Offline"
            val caps = cm.getNetworkCapabilities(network) ?: return "Offline"
            val transports = buildList {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("Wi-Fi")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("Cellular")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("Ethernet")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
            }
            if (transports.isEmpty()) "Connected (Other)" else transports.joinToString(" + ")
        } catch (_: Exception) {
            "Unknown"
        }
    }

    fun buildEnvironmentHeader(
        context: Context,
        appVersion: String = getAppVersionLabel(context)
    ): String {
        val prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        val tier = if (prefs.getBoolean("is_pro_unlocked", false)) "Pro" else "Free"
        val sb = StringBuilder()
        sb.appendLine("App Version: $appVersion")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Network: ${getNetworkSummary(context)}")
        sb.appendLine("Tier: $tier")
        return sb.toString().trimEnd()
    }

    @Synchronized
    fun buildReport(
        appVersion: String,
        activeNoticeSummary: String? = null,
        context: Context? = null
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== Inbox Priorities Diagnostic Report ===")
        if (context != null) {
            sb.appendLine(buildEnvironmentHeader(context, appVersion))
        } else {
            sb.appendLine("App Version: $appVersion")
            sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        }
        if (!activeNoticeSummary.isNullOrBlank()) {
            sb.appendLine("Active Notice: $activeNoticeSummary")
        }
        sb.appendLine("--- Recent Events (${events.size}) ---")
        events.reversed().forEach { e ->
            sb.appendLine("[${e.timestamp}] ${e.account} | ${e.operation} | ${e.errorType}: ${e.message}")
        }
        return sb.toString()
    }

    @Synchronized
    fun buildHelpSupportEmailBody(context: Context): String {
        val sb = StringBuilder()
        sb.appendLine("Please describe your question or issue above this line.")
        sb.appendLine()
        sb.appendLine("=== System & App Info ===")
        sb.appendLine(buildEnvironmentHeader(context))
        if (events.isNotEmpty()) {
            val latest = events.last()
            sb.appendLine("Last Recorded Event: [${latest.timestamp}] ${latest.operation} | ${latest.errorType}")
        }
        return sb.toString()
    }
}
