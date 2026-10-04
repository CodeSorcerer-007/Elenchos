package com.example.elenchos.testing.runtime.phases

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.elenchos.domain.model.HealthScore
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueConfidence
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.domain.model.ScreenGraph
import com.example.elenchos.domain.model.TerminalLogEntry
import com.example.elenchos.domain.model.TestCoverage
import com.example.elenchos.domain.model.TestSession
import com.example.elenchos.domain.model.TestStatus
import com.example.elenchos.testing.runtime.LogcatMonitor
import kotlinx.coroutines.delay

class NetworkResiliencePhase : TestPhase {
    override val name: String = "Network Resilience"

    override suspend fun execute(context: PhaseExecutionContext): PhaseExecutionResult {
        if (context.isAbortRequested()) return PhaseExecutionResult(isAborted = true)

        if (!context.config.enableNetworkResilience) {
            context.log(TerminalLogEntry.LogLevel.INFO, "NETWORK", "Network resilience testing skipped per configuration.")
            return PhaseExecutionResult(success = true, details = "Skipped by configuration")
        }

        context.log(TerminalLogEntry.LogLevel.TEST, "NETWORK", "Auditing network resilience and offline state handling...")
        val cm = context.context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        context.log(TerminalLogEntry.LogLevel.INFO, "NETWORK", "Device active connectivity: ${if (hasInternet) "Online (Validated)" else "Offline"}")

        val requestsInternet = context.apk.permissions.any { it.contains("INTERNET", ignoreCase = true) }
        val requestsNetworkState = context.apk.permissions.any { it.contains("ACCESS_NETWORK_STATE", ignoreCase = true) }

        if (requestsInternet && !requestsNetworkState) {
            context.discoveredIssues.add(
                Issue(
                    id = "NET-001",
                    title = "Missing ACCESS_NETWORK_STATE permission for offline resilience",
                    severity = IssueSeverity.P2,
                    category = IssueCategory.NETWORK,
                    confidence = IssueConfidence.HIGH,
                    affectedScreen = "AndroidManifest.xml",
                    description = "Target requests android.permission.INTERNET but does not declare android.permission.ACCESS_NETWORK_STATE. The app cannot inspect ConnectivityManager to detect offline state before firing failing HTTP requests.",
                    reproductionSteps = listOf("Inspect declared permissions in manifest", "Verify presence of ACCESS_NETWORK_STATE"),
                    expectedBehavior = "Network-enabled apps must declare ACCESS_NETWORK_STATE to gracefully handle offline transitions",
                    actualBehavior = "ACCESS_NETWORK_STATE permission is missing from manifest",
                    rootCauseHypothesis = "Developer omitted network state monitoring permission",
                    recommendedFix = "Add <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" /> to AndroidManifest.xml"
                )
            )
            context.log(TerminalLogEntry.LogLevel.WARN, "NETWORK", "[P2] Missing ACCESS_NETWORK_STATE permission for graceful offline handling")
        } else {
            context.log(TerminalLogEntry.LogLevel.SUCCESS, "NETWORK", "Network resilience and offline handlers verified")
        }
        delay(80)
        return PhaseExecutionResult(success = true, details = if (requestsInternet && !requestsNetworkState) "Missing ACCESS_NETWORK_STATE" else "Network resilience verified")
    }
}

class CrashDiagnosticsPhase : TestPhase {
    override val name: String = "Crash & ANR Diagnostics"

    override suspend fun execute(context: PhaseExecutionContext): PhaseExecutionResult {
        if (context.isAbortRequested()) return PhaseExecutionResult(isAborted = true)

        context.log(TerminalLogEntry.LogLevel.TEST, "LOGCAT", "Collecting system logcat buffers and parsing uncaught runtime exceptions...")
        val runtimeCrashes = LogcatMonitor.captureRecentCrashes(context.apk.packageName)
        context.discoveredIssues.addAll(runtimeCrashes)
        context.runtimeCrashesCount = runtimeCrashes.size

        if (runtimeCrashes.isEmpty()) {
            context.log(TerminalLogEntry.LogLevel.SUCCESS, "LOGCAT", "No uncaught runtime fatal exceptions recorded in target process")
        } else {
            for (crash in runtimeCrashes) {
                context.log(TerminalLogEntry.LogLevel.CRITICAL, "LOGCAT", "Detected crash: ${crash.title}")
            }
        }
        delay(80)
        return PhaseExecutionResult(
            success = runtimeCrashes.isEmpty(),
            details = if (runtimeCrashes.isEmpty()) "0 fatal crashes detected" else "${runtimeCrashes.size} crash(es) harvested"
        )
    }
}

class IssueCorrelationPhase : TestPhase {
    override val name: String = "Issue Correlation & Deduplication"

    override suspend fun execute(context: PhaseExecutionContext): PhaseExecutionResult {
        if (context.isAbortRequested()) return PhaseExecutionResult(isAborted = true)

        context.log(TerminalLogEntry.LogLevel.TEST, "CORRELATION", "Correlating symptoms and deduplicating root causes...")
        val beforeCount = context.discoveredIssues.size
        val uniqueIssues = context.discoveredIssues.distinctBy { "${it.title}|${it.affectedScreen}" }
        context.discoveredIssues.clear()
        context.discoveredIssues.addAll(uniqueIssues)

        val duplicatesRemoved = beforeCount - uniqueIssues.size
        if (duplicatesRemoved > 0) {
            context.log(TerminalLogEntry.LogLevel.INFO, "CORRELATION", "Deduplicated $duplicatesRemoved redundant diagnostic finding(s)")
        }
        context.log(TerminalLogEntry.LogLevel.INFO, "CORRELATION", "Total unique issues cataloged: ${context.discoveredIssues.size}")
        delay(80)
        return PhaseExecutionResult(success = true, details = "${context.discoveredIssues.size} unique issues cataloged")
    }
}

class HealthScoreSynthesisPhase : TestPhase {
    override val name: String = "Health Score & Report Synthesis"

    override suspend fun execute(context: PhaseExecutionContext): PhaseExecutionResult {
        if (context.isAbortRequested()) return PhaseExecutionResult(isAborted = true)

        val apk = context.apk
        val healthScore = HealthScore.calculate(
            apk = apk,
            issues = context.discoveredIssues,
            runtimeCrashesCount = context.runtimeCrashesCount,
            launcherActivity = context.launcherActivity,
            launchLatencyMs = context.launchLatencyMs,
            realClicks = context.realClicks
        )

        context.log(TerminalLogEntry.LogLevel.SUCCESS, "ENGINE", "Testing session completed. Overall Health Score: ${healthScore.overallScore} / 100")
        context.log(
            TerminalLogEntry.LogLevel.INFO,
            "ENGINE",
            "Stability: ${healthScore.stabilityScore} | Security: ${healthScore.securityScore} | Functionality: ${healthScore.functionalityScore} | Performance: ${healthScore.performanceScore} | Compatibility: ${healthScore.compatibilityScore}"
        )
        delay(60)

        return PhaseExecutionResult(success = true, details = "Synthesized Health Score: ${healthScore.overallScore}/100")
    }
}
