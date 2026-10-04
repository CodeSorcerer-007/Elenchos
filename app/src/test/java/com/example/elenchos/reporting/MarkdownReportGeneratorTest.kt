package com.example.elenchos.reporting

import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.HealthScore
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueConfidence
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.domain.model.TestCoverage
import com.example.elenchos.domain.model.TestSession
import com.example.elenchos.domain.model.TestStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownReportGeneratorTest {

    private fun createSessionAndApk(): Pair<TestSession, APKArtifact> {
        val apk = APKArtifact(
            id = "apk-1",
            filePath = "/path/to/apk.apk",
            appName = "Calculator Lab",
            packageName = "com.elenchos.calc",
            versionName = "3.2.1",
            versionCode = 15,
            minSdk = 24,
            targetSdk = 35,
            compileSdk = 35,
            fileSizeBytes = 5 * 1024 * 1024,
            formattedSize = "5.0 MB",
            sha256 = "abc123sha256",
            isDebuggable = false,
            allowsBackup = false,
            usesCleartextTraffic = false,
            is16KbPageAligned = true,
            isInstalledOnDevice = true,
            dexCount = 1,
            totalAssetsCount = 3,
            nativeArchitectures = listOf("arm64-v8a"),
            permissions = emptyList(),
            dangerousPermissions = emptyList(),
            activities = emptyList(),
            services = emptyList(),
            receivers = emptyList(),
            providers = emptyList(),
            detectedSecrets = emptyList()
        )

        val issue = Issue(
            id = "ISSUE-01",
            title = "Arithmetic Overflow on Multiply",
            severity = IssueSeverity.P1,
            category = IssueCategory.CRASH,
            confidence = IssueConfidence.HIGH,
            affectedScreen = "com.elenchos.calc.MainActivity",
            description = "Multiplying max integer values causes unhandled NumberFormatException",
            reproductionSteps = listOf("Enter Int.MAX_VALUE", "Press multiply", "Enter 2"),
            expectedBehavior = "Display overflow indicator or BigInteger result",
            actualBehavior = "App crashes",
            rootCauseHypothesis = "Standard Int was used without overflow handling",
            recommendedFix = "Switch to BigInteger or clamp input"
        )

        val session = TestSession(
            id = "TEST-SESSION-001",
            apkArtifactId = apk.id,
            packageName = apk.packageName,
            appName = apk.appName,
            versionName = apk.versionName,
            apkSha256 = apk.sha256,
            deviceModel = "Pixel 8",
            androidVersion = "Android 15",
            startTimestamp = 1700000000000L,
            status = TestStatus.WARNINGS_FOUND,
            issues = listOf(issue),
            healthScore = HealthScore(85, 90, 80, 95, 85, 90, 80, "Formula text"),
            coverage = TestCoverage(4, 4, 16, 12, 3, 2, 0, 1)
        )

        return Pair(session, apk)
    }

    @Test
    fun generateReport_includesHeaderAndMetadata() {
        val (session, apk) = createSessionAndApk()
        val markdown = MarkdownReportGenerator.generateReport(session, apk)

        assertTrue(markdown.contains("# ELENCHOS APP HEALTH REPORT"))
        assertTrue(markdown.contains("Calculator Lab"))
        assertTrue(markdown.contains("com.elenchos.calc"))
        assertTrue(markdown.contains("3.2.1"))
        assertTrue(markdown.contains("TEST-SESSION-001"))
    }

    @Test
    fun generateReport_includesHealthScoreTable() {
        val (session, apk) = createSessionAndApk()
        val markdown = MarkdownReportGenerator.generateReport(session, apk)

        assertTrue(markdown.contains("## HEALTH SCORE: 85 / 100"))
        assertTrue(markdown.contains("**Stability**"))
        assertTrue(markdown.contains("90 / 100"))
        assertTrue(markdown.contains("**Security**"))
        assertTrue(markdown.contains("95 / 100"))
    }

    @Test
    fun generateReport_includesCoverageAndDetailedIssues() {
        val (session, apk) = createSessionAndApk()
        val markdown = MarkdownReportGenerator.generateReport(session, apk)

        assertTrue(markdown.contains("## TEST COVERAGE METRICS"))
        assertTrue(markdown.contains("Screens Discovered"))
        assertTrue(markdown.contains("## DETAILED ISSUES"))
        assertTrue(markdown.contains("[P1] Arithmetic Overflow on Multiply"))
        assertTrue(markdown.contains("Multiplying max integer values"))
        assertTrue(markdown.contains("Switch to BigInteger"))
    }
}
