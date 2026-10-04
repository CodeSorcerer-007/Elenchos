package com.example.elenchos.reporting

import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.HealthScore
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueConfidence
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.domain.model.TestSession
import com.example.elenchos.domain.model.TestStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIFixPackageGeneratorTest {

    private fun createTestFixture(): Pair<TestSession, APKArtifact> {
        val apk = APKArtifact(
            id = "apk-123",
            filePath = "/test/app.apk",
            appName = "Demo App",
            packageName = "com.example.demo",
            versionName = "2.1.0",
            versionCode = 42,
            minSdk = 26,
            targetSdk = 35,
            compileSdk = 35,
            fileSizeBytes = 20 * 1024 * 1024,
            formattedSize = "20.0 MB",
            sha256 = "deadbeef12345678deadbeef12345678deadbeef12345678deadbeef12345678",
            isDebuggable = false,
            allowsBackup = false,
            usesCleartextTraffic = false,
            is16KbPageAligned = true,
            isInstalledOnDevice = true,
            dexCount = 1,
            totalAssetsCount = 10,
            nativeArchitectures = listOf("arm64-v8a"),
            permissions = listOf("android.permission.INTERNET"),
            dangerousPermissions = emptyList(),
            activities = emptyList(),
            services = emptyList(),
            receivers = emptyList(),
            providers = emptyList(),
            detectedSecrets = emptyList()
        )

        val issue = Issue(
            id = "SEC-001",
            title = "Dangerous Debug Mode",
            severity = IssueSeverity.P0,
            category = IssueCategory.SECURITY,
            confidence = IssueConfidence.CONFIRMED,
            affectedScreen = "AndroidManifest.xml",
            description = "App is debuggable with injected ```code``` block",
            reproductionSteps = listOf("Step 1: Check manifest", "Step 2: Connect debugger"),
            expectedBehavior = "Debuggable should be false",
            actualBehavior = "Debuggable is true",
            stackTrace = "java.lang.SecurityException: Debugger attached",
            rootCauseHypothesis = "Debug flag was hardcoded",
            recommendedFix = "Set debuggable=false with injected ```code``` block"
        )

        val health = HealthScore(
            overallScore = 82,
            stabilityScore = 90,
            functionalityScore = 85,
            securityScore = 75,
            performanceScore = 90,
            accessibilityScore = 80,
            compatibilityScore = 85,
            formulaExplanation = "Weighted health calculation"
        )

        val session = TestSession(
            id = "SESSION-XYZ",
            apkArtifactId = apk.id,
            packageName = apk.packageName,
            appName = apk.appName,
            versionName = apk.versionName,
            apkSha256 = apk.sha256,
            deviceModel = "Pixel 9 Pro",
            androidVersion = "Android 15 (API 35)",
            startTimestamp = 1700000000000L,
            endTimestamp = 1700000010000L,
            status = TestStatus.WARNINGS_FOUND,
            issues = listOf(issue),
            healthScore = health
        )

        return Pair(session, apk)
    }

    @Test
    fun generatePackage_createsCompleteAIFixPackage() {
        val (session, apk) = createTestFixture()
        val pkg = AIFixPackageGenerator.generatePackage(session, apk)

        assertEquals("Demo App", pkg.project.appName)
        assertEquals("com.example.demo", pkg.project.packageName)
        assertEquals(42, pkg.project.versionCode)
        assertEquals(35, pkg.project.targetSdk)
        assertEquals(1, pkg.issues.size)
        assertEquals("SEC-001", pkg.issues[0].id)
        assertEquals("P0", pkg.issues[0].severity)
        assertEquals(82, pkg.healthScore.overallScore)
        assertTrue(pkg.executiveSummary.contains("Demo App"))
        assertTrue(pkg.executiveSummary.contains("1 Blocker"))
    }

    @Test
    fun exportJson_producesValidJson() {
        val (session, apk) = createTestFixture()
        val pkg = AIFixPackageGenerator.generatePackage(session, apk)
        val jsonStr = AIFixPackageGenerator.exportJson(pkg)

        assertNotNull(jsonStr)
        assertTrue(jsonStr.contains("\"appName\": \"Demo App\""))
        assertTrue(jsonStr.contains("\"overallScore\": 82"))

        // Verify it parses back
        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<com.example.elenchos.domain.model.AIFixPackage>(jsonStr)
        assertEquals(pkg.project.packageName, parsed.project.packageName)
    }

    @Test
    fun generateAIAgentHandoffPrompt_containsAllRequiredSectionsAndSanitizesDelimiters() {
        val (session, apk) = createTestFixture()
        val pkg = AIFixPackageGenerator.generatePackage(session, apk)
        val prompt = AIFixPackageGenerator.generateAIAgentHandoffPrompt(pkg)

        assertTrue("Should contain role definition", prompt.contains("senior Android platform engineer"))
        assertTrue("Should contain project metadata section", prompt.contains("PROJECT METADATA"))
        assertTrue("Should contain laboratory health score section", prompt.contains("LABORATORY HEALTH SCORE: 82 / 100"))
        assertTrue("Should contain executive summary section", prompt.contains("EXECUTIVE SUMMARY"))
        assertTrue("Should contain detected issues section", prompt.contains("DETECTED ISSUES & EVIDENCE"))
        assertTrue("Should contain engineering instructions", prompt.contains("ENGINEERING INSTRUCTIONS"))
        assertTrue("Should list issue details", prompt.contains("[P0] Dangerous Debug Mode"))

        // Verify description with ``` was sanitized to ''' so it doesn't break prompt markdown blocks
        assertFalse(prompt.contains("with injected ```code``` block"))
        assertTrue(prompt.contains("with injected '''code''' block"))
    }
}
