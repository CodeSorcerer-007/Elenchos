package com.example.elenchos.domain.model

import kotlinx.serialization.Serializable

/**
 * Severity ratings according to specification:
 * P0 - Blocker (Application unusable, data loss, critical security failure)
 * P1 - Critical (Major functionality broken or frequent crash)
 * P2 - High (Important feature broken or serious usability issue)
 * P3 - Medium (Non-critical defect)
 * P4 - Low (Minor issue, polish, optimization or edge case)
 */
@Serializable
enum class IssueSeverity(val code: String, val label: String, val weight: Int) {
    P0("P0", "BLOCKER", 40),
    P1("P1", "CRITICAL", 25),
    P2("P2", "HIGH", 15),
    P3("P3", "MEDIUM", 8),
    P4("P4", "LOW", 3)
}

@Serializable
enum class IssueConfidence(val label: String) {
    CONFIRMED("Confirmed"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low")
}

@Serializable
enum class IssueCategory(val label: String) {
    CRASH("Crash / Uncaught Exception"),
    ANR("ANR / Unresponsive UI"),
    SECURITY("Security Weakness"),
    MANIFEST("Manifest Defect"),
    PERMISSION("Permission Misconfiguration"),
    UI_UX("UI / UX Defect"),
    LIFECYCLE("Lifecycle Failure"),
    NAVIGATION("Navigation / Back Stack"),
    NETWORK("Network Resilience"),
    PERFORMANCE("Performance & Bloat"),
    COMPATIBILITY("Device / SDK Compatibility"),
    ACCESSIBILITY("Accessibility Missing")
}

@Serializable
enum class TestStatus(val label: String) {
    IDLE("Ready for Testing"),
    RUNNING("Testing in Progress"),
    PASSED("Passed"),
    WARNINGS_FOUND("Warnings Found"),
    CRITICAL_ISSUES_FOUND("Critical Issues Found"),
    FAILED("Execution Failed"),
    CANCELLED("Testing Aborted")
}

@Serializable
enum class TestExecutionCapability(val label: String) {
    TESTED("Tested"),
    PARTIALLY_TESTED("Partially Tested"),
    NOT_TESTABLE_ON_THIS_DEVICE("Not Testable on This Device"),
    REQUIRES_ELEVATED_ENVIRONMENT("Requires Accessibility / Elevated Privileges"),
    SKIPPED("Skipped"),
    PASSED("Passed"),
    FAILED("Failed"),
    INCONCLUSIVE("Inconclusive")
}

@Serializable
data class APKArtifact(
    val id: String,
    val fileName: String = "",
    val filePath: String,
    val sha256: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val compileSdk: Int = 35,
    val importedTimestamp: Long = System.currentTimeMillis(),
    val isInstalledOnDevice: Boolean = false,
    val isDebuggable: Boolean = false,
    val allowsBackup: Boolean = true,
    val usesCleartextTraffic: Boolean = false,
    val permissions: List<String> = emptyList(),
    val dangerousPermissions: List<String> = emptyList(),
    val activities: List<ComponentInfo> = emptyList(),
    val services: List<ComponentInfo> = emptyList(),
    val receivers: List<ComponentInfo> = emptyList(),
    val providers: List<ComponentInfo> = emptyList(),
    val nativeArchitectures: List<String> = emptyList(),
    val is16KbPageAligned: Boolean = true,
    val certificateFingerprint: String = "",
    val dexCount: Int = 1,
    val totalAssetsCount: Int = 0,
    val detectedSecrets: List<String> = emptyList()
)

@Serializable
data class ComponentInfo(
    val name: String,
    val isExported: Boolean,
    val permission: String? = null,
    val intentActions: List<String> = emptyList(),
    val dataSchemes: List<String> = emptyList()
)

@Serializable
data class TestConfiguration(
    val mode: TestMode = TestMode.STANDARD,
    val maxDurationSeconds: Int = 300,
    val maxInteractions: Int = 200,
    val maxScreens: Int = 30,
    val enableInputFuzzing: Boolean = true,
    val enableLifecycleTesting: Boolean = true,
    val enableOrientationTesting: Boolean = true,
    val enableNetworkResilience: Boolean = true,
    val enableSecurityAudit: Boolean = true
) {
    @Serializable
    enum class TestMode(val label: String, val description: String) {
        QUICK("Quick Scan", "Static manifest, security scan, and baseline launch validation"),
        STANDARD("Standard Test", "Static analysis + UI exploration + input fuzzing + lifecycle checks"),
        DEEP("Deep Test", "Exhaustive exploration, boundary fuzzing, rotation & network stress"),
        CUSTOM("Custom Test", "User-configured test parameters and budgets")
    }
}

@Serializable
data class TestPhaseProgress(
    val phaseName: String,
    val isCompleted: Boolean = false,
    val isCurrent: Boolean = false,
    val details: String = "",
    val capability: TestExecutionCapability = TestExecutionCapability.TESTED
)

@Serializable
data class Issue(
    val id: String,
    val severity: IssueSeverity,
    val confidence: IssueConfidence,
    val category: IssueCategory,
    val title: String,
    val description: String,
    val affectedScreen: String = "Global / Manifest",
    val reproductionSteps: List<String> = emptyList(),
    val expectedBehavior: String = "",
    val actualBehavior: String = "",
    val stackTrace: String? = null,
    val logs: List<String> = emptyList(),
    val rootCauseHypothesis: String = "",
    val hypothesisConfidence: IssueConfidence = IssueConfidence.HIGH,
    val recommendedFix: String = "",
    val occurrenceCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ScreenNode(
    val id: String,
    val activityName: String,
    val title: String,
    val interactiveElementCount: Int = 0,
    val visitedCount: Int = 0,
    val discoveredTimestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ScreenTransition(
    val fromScreenId: String,
    val toScreenId: String,
    val actionDescription: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ScreenGraph(
    val nodes: List<ScreenNode> = emptyList(),
    val transitions: List<ScreenTransition> = emptyList(),
    val totalElementsDiscovered: Int = 0,
    val totalElementsExercised: Int = 0
)

@Serializable
data class HealthScore(
    val overallScore: Int,
    val stabilityScore: Int,
    val functionalityScore: Int,
    val securityScore: Int,
    val performanceScore: Int,
    val accessibilityScore: Int,
    val compatibilityScore: Int,
    val formulaExplanation: String
) {
    companion object {
        fun calculate(
            apk: APKArtifact,
            issues: List<Issue>,
            runtimeCrashesCount: Int = 0,
            launcherActivity: ComponentInfo? = null,
            launchLatencyMs: Long = 180L,
            realClicks: Int = 1
        ): HealthScore {
            val p0Count = issues.count { it.severity == IssueSeverity.P0 }
            val p1Count = issues.count { it.severity == IssueSeverity.P1 }
            val p2Count = issues.count { it.severity == IssueSeverity.P2 }
            val p3Count = issues.count { it.severity == IssueSeverity.P3 }

            val stabilityScore = (100 - (p0Count * 40 + p1Count * 25 + p2Count * 10 + p3Count * 3)).coerceIn(0, 100)
            val securityScore = (100 - (p0Count * 35 + p1Count * 20 + p2Count * 10 + p3Count * 5)).coerceIn(0, 100)

            val functionalityPenalty = (runtimeCrashesCount * 35) + (if (launcherActivity == null) 25 else 0) + (p1Count * 10)
            val functionalityScore = (100 - functionalityPenalty).coerceIn(0, 100)

            val sizePenalty = if (apk.fileSizeBytes > 80 * 1024 * 1024) 20 else if (apk.fileSizeBytes > 40 * 1024 * 1024) 10 else 0
            val latencyPenalty = if (launchLatencyMs > 600) 20 else if (launchLatencyMs > 400) 10 else 0
            val performanceScore = (100 - (sizePenalty + latencyPenalty + (if (apk.dexCount > 3) 10 else 0))).coerceIn(0, 100)

            val accessibilityPenalty = if (realClicks == 0) 15 else 0
            val accessibilityScore = (95 - accessibilityPenalty).coerceIn(0, 100)

            val page16KbPenalty = if (!apk.is16KbPageAligned) 25 else 0
            val targetSdkPenalty = if (apk.targetSdk < 34) 20 else if (apk.targetSdk < 35) 5 else 0
            val abiPenalty = if (apk.nativeArchitectures.isNotEmpty() && !apk.nativeArchitectures.any { it.contains("64") }) 30 else 0
            val compatibilityScore = (100 - (page16KbPenalty + targetSdkPenalty + abiPenalty)).coerceIn(0, 100)

            val overallScore = ((stabilityScore * 0.25) +
                    (securityScore * 0.25) +
                    (functionalityScore * 0.20) +
                    (performanceScore * 0.10) +
                    (accessibilityScore * 0.10) +
                    (compatibilityScore * 0.10)).toInt().coerceIn(0, 100)

            return HealthScore(
                overallScore = overallScore,
                stabilityScore = stabilityScore,
                functionalityScore = functionalityScore,
                securityScore = securityScore,
                performanceScore = performanceScore,
                accessibilityScore = accessibilityScore,
                compatibilityScore = compatibilityScore,
                formulaExplanation = "Score = (Stability × 0.25) + (Security × 0.25) + (Functionality × 0.20) + (Performance × 0.10) + (Accessibility × 0.10) + (Compatibility × 0.10). Metrics derived from static manifest analysis, runtime crash monitors, latency checks, and accessibility trees."
            )
        }
    }
}

@Serializable
data class TestCoverage(
    val screensDiscovered: Int,
    val screensTested: Int,
    val interactiveElementsDiscovered: Int,
    val interactiveElementsExercised: Int,
    val navigationPathsExercised: Int,
    val inputFieldsTested: Int,
    val permissionFlowsTested: Int,
    val lifecycleScenariosTested: Int
)

@Serializable
data class TestSession(
    val id: String,
    val apkArtifactId: String,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val apkSha256: String,
    val deviceModel: String,
    val androidVersion: String,
    val startTimestamp: Long = System.currentTimeMillis(),
    val endTimestamp: Long? = null,
    val status: TestStatus = TestStatus.IDLE,
    val configuration: TestConfiguration = TestConfiguration(),
    val issues: List<Issue> = emptyList(),
    val screenGraph: ScreenGraph = ScreenGraph(),
    val healthScore: HealthScore? = null,
    val coverage: TestCoverage = TestCoverage(0, 0, 0, 0, 0, 0, 0, 0),
    val terminalLogs: List<TerminalLogEntry> = emptyList(),
    val actionsCount: Int = 0
)

@Serializable
data class TerminalLogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    @Serializable
    enum class LogLevel {
        INFO, TEST, WARN, FAIL, SUCCESS, CRITICAL
    }
}

@Serializable
data class AIFixPackage(
    val project: ProjectMeta,
    val environment: EnvironmentMeta,
    val executiveSummary: String,
    val healthScore: HealthScore,
    val issues: List<AIIssueItem>,
    val developerInstructions: String
) {
    @Serializable
    data class ProjectMeta(
        val appName: String,
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val apkSha256: String,
        val targetSdk: Int,
        val minSdk: Int
    )

    @Serializable
    data class EnvironmentMeta(
        val androidVersion: String,
        val deviceModel: String,
        val testingEngine: String = "Elenchos Autonomous Mobile QA Laboratory v1.0"
    )

    @Serializable
    data class AIIssueItem(
        val id: String,
        val severity: String,
        val category: String,
        val title: String,
        val confidence: String,
        val affectedScreen: String,
        val reproductionSteps: List<String>,
        val expectedBehavior: String,
        val actualBehavior: String,
        val stackTrace: String?,
        val rootCauseHypothesis: String,
        val recommendedFix: String
    )
}

@Serializable
data class SessionDiff(
    val baseSessionId: String,
    val compareSessionId: String,
    val regressions: List<Issue>,
    val resolvedIssues: List<Issue>,
    val persistentIssues: List<Issue>,
    val healthScoreDelta: Int,
    val stabilityDelta: Int,
    val securityDelta: Int,
    val functionalityDelta: Int
) {
    companion object {
        fun calculate(previous: TestSession, current: TestSession): SessionDiff {
            val prevIssues = previous.issues
            val currIssues = current.issues

            val prevTitles = prevIssues.map { it.title }.toSet()
            val currTitles = currIssues.map { it.title }.toSet()

            val regressions = currIssues.filter { it.title !in prevTitles }
            val resolved = prevIssues.filter { it.title !in currTitles }
            val persistent = currIssues.filter { it.title in prevTitles }

            val prevHealth = previous.healthScore
            val currHealth = current.healthScore

            val scoreDelta = (currHealth?.overallScore ?: 0) - (prevHealth?.overallScore ?: 0)
            val stabDelta = (currHealth?.stabilityScore ?: 0) - (prevHealth?.stabilityScore ?: 0)
            val secDelta = (currHealth?.securityScore ?: 0) - (prevHealth?.securityScore ?: 0)
            val funcDelta = (currHealth?.functionalityScore ?: 0) - (prevHealth?.functionalityScore ?: 0)

            return SessionDiff(
                baseSessionId = previous.id,
                compareSessionId = current.id,
                regressions = regressions,
                resolvedIssues = resolved,
                persistentIssues = persistent,
                healthScoreDelta = scoreDelta,
                stabilityDelta = stabDelta,
                securityDelta = secDelta,
                functionalityDelta = funcDelta
            )
        }
    }
}

