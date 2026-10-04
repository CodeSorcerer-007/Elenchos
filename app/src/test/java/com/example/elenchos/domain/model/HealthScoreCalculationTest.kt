package com.example.elenchos.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthScoreCalculationTest {

    private fun createBaseApk(): APKArtifact {
        return APKArtifact(
            id = "test-apk",
            filePath = "/fake/app.apk",
            appName = "Test App",
            packageName = "com.example.test",
            versionName = "1.0.0",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 35,
            compileSdk = 35,
            fileSizeBytes = 10 * 1024 * 1024,
            formattedSize = "10.0 MB",
            sha256 = "1234567890abcdef",
            isDebuggable = false,
            allowsBackup = false,
            usesCleartextTraffic = false,
            is16KbPageAligned = true,
            isInstalledOnDevice = true,
            dexCount = 1,
            totalAssetsCount = 5,
            nativeArchitectures = listOf("arm64-v8a"),
            permissions = emptyList(),
            dangerousPermissions = emptyList(),
            activities = listOf(
                ComponentInfo("com.example.test.MainActivity", isExported = true, intentActions = listOf("android.intent.action.MAIN"))
            ),
            services = emptyList(),
            receivers = emptyList(),
            providers = emptyList(),
            detectedSecrets = emptyList()
        )
    }

    private fun createIssue(category: IssueCategory, severity: IssueSeverity, id: String = "ISSUE-1"): Issue {
        return Issue(
            id = id,
            severity = severity,
            confidence = IssueConfidence.CONFIRMED,
            category = category,
            title = "Test issue in $category",
            description = "Detailed description",
            affectedScreen = "Screen",
            reproductionSteps = listOf("Step 1"),
            expectedBehavior = "Expected",
            actualBehavior = "Actual",
            rootCauseHypothesis = "Root cause",
            recommendedFix = "Fix"
        )
    }

    @Test
    fun calculate_pristineWithNoIssues_returnsOptimalScore() {
        val apk = createBaseApk()
        val launcher = apk.activities.first()
        val score = HealthScore.calculate(apk = apk, issues = emptyList(), launcherActivity = launcher)

        assertEquals(100, score.stabilityScore)
        assertEquals(100, score.securityScore)
        assertEquals(100, score.functionalityScore)
        assertEquals(100, score.performanceScore)
        assertEquals(95, score.accessibilityScore)
        assertEquals(100, score.compatibilityScore)
        assertEquals(99, score.overallScore)
        assertTrue(score.formulaExplanation.contains("Stability × 0.25"))
    }

    @Test
    fun calculate_withP0Issue_penalizesStabilityAndSecurity() {
        val apk = createBaseApk()
        val issues = listOf(
            createIssue(IssueCategory.CRASH, IssueSeverity.P0)
        )
        val score = HealthScore.calculate(apk = apk, issues = issues, launcherActivity = apk.activities.first())

        // P0 issue penalizes stability: 100 - (1 * 40) = 60
        assertEquals(60, score.stabilityScore)
        // P0 issue penalizes security: 100 - (1 * 35) = 65
        assertEquals(65, score.securityScore)
        assertTrue("Overall score should drop significantly with P0 blocker", score.overallScore < 85)
    }

    @Test
    fun calculate_with16KbUnalignedAndLowTargetSdk_penalizesCompatibility() {
        val unalignedApk = createBaseApk().copy(
            is16KbPageAligned = false, // -25
            targetSdk = 33 // -20
        )
        val score = HealthScore.calculate(apk = unalignedApk, issues = emptyList(), launcherActivity = unalignedApk.activities.first())

        // 100 - (25 + 20) = 55
        assertEquals(55, score.compatibilityScore)
    }

    @Test
    fun calculate_boundsScoresBetweenZeroAndOneHundred() {
        val catastrophicApk = createBaseApk().copy(
            fileSizeBytes = 100 * 1024 * 1024,
            is16KbPageAligned = false,
            targetSdk = 28,
            nativeArchitectures = listOf("armeabi-v7a")
        )
        val catastrophicIssues = (1..10).map { createIssue(IssueCategory.CRASH, IssueSeverity.P0, "P0-$it") }
        val score = HealthScore.calculate(
            apk = catastrophicApk,
            issues = catastrophicIssues,
            runtimeCrashesCount = 5,
            launcherActivity = null,
            launchLatencyMs = 1200L,
            realClicks = 0
        )

        assertEquals(0, score.stabilityScore)
        assertEquals(0, score.securityScore)
        assertEquals(0, score.functionalityScore)
        assertTrue("Overall score should be bounded low", score.overallScore in 10..25)
        assertTrue(score.overallScore >= 0)
    }
}
