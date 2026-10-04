package com.example.elenchos.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsSerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun enums_serializeAndDeserializeCorrectly() {
        for (severity in IssueSeverity.values()) {
            val encoded = json.encodeToString(severity)
            val decoded = json.decodeFromString<IssueSeverity>(encoded)
            assertEquals(severity, decoded)
        }

        for (confidence in IssueConfidence.values()) {
            val encoded = json.encodeToString(confidence)
            val decoded = json.decodeFromString<IssueConfidence>(encoded)
            assertEquals(confidence, decoded)
        }

        for (category in IssueCategory.values()) {
            val encoded = json.encodeToString(category)
            val decoded = json.decodeFromString<IssueCategory>(encoded)
            assertEquals(category, decoded)
        }

        for (status in TestStatus.values()) {
            val encoded = json.encodeToString(status)
            val decoded = json.decodeFromString<TestStatus>(encoded)
            assertEquals(status, decoded)
        }

        for (capability in TestExecutionCapability.values()) {
            val encoded = json.encodeToString(capability)
            val decoded = json.decodeFromString<TestExecutionCapability>(encoded)
            assertEquals(capability, decoded)
        }

        for (mode in TestConfiguration.TestMode.values()) {
            val encoded = json.encodeToString(mode)
            val decoded = json.decodeFromString<TestConfiguration.TestMode>(encoded)
            assertEquals(mode, decoded)
        }

        for (level in TerminalLogEntry.LogLevel.values()) {
            val encoded = json.encodeToString(level)
            val decoded = json.decodeFromString<TerminalLogEntry.LogLevel>(encoded)
            assertEquals(level, decoded)
        }
    }

    @Test
    fun testSession_withAllEnums_serializesAndDeserializesWithoutCrash() {
        val issue = Issue(
            id = "ISSUE-001",
            severity = IssueSeverity.P1,
            confidence = IssueConfidence.CONFIRMED,
            category = IssueCategory.SECURITY,
            title = "Test Issue Title",
            description = "Test Issue Description",
            affectedScreen = "MainActivity",
            reproductionSteps = listOf("Step 1", "Step 2"),
            expectedBehavior = "Should work",
            actualBehavior = "Crashed",
            rootCauseHypothesis = "Null pointer dereference",
            recommendedFix = "Add null check"
        )

        val log = TerminalLogEntry(
            timestamp = "12:00:00",
            level = TerminalLogEntry.LogLevel.TEST,
            tag = "ENGINE",
            message = "Test message"
        )

        val session = TestSession(
            id = "SESSION-TEST-001",
            apkArtifactId = "apk-123",
            packageName = "com.example.test",
            appName = "Test App",
            versionName = "2.1.0",
            apkSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            deviceModel = "Pixel 8",
            androidVersion = "Android 15",
            status = TestStatus.WARNINGS_FOUND,
            configuration = TestConfiguration(mode = TestConfiguration.TestMode.DEEP),
            issues = listOf(issue),
            terminalLogs = listOf(log),
            healthScore = HealthScore(
                overallScore = 88,
                stabilityScore = 90,
                functionalityScore = 85,
                securityScore = 90,
                performanceScore = 85,
                accessibilityScore = 90,
                compatibilityScore = 88,
                formulaExplanation = "Transparent score formula"
            )
        )

        val encoded = json.encodeToString(session)
        assertNotNull("JSON string should not be null", encoded)
        assertTrue("JSON should contain package name", encoded.contains("com.example.test"))

        val decoded = json.decodeFromString<TestSession>(encoded)
        assertEquals(session.id, decoded.id)
        assertEquals(session.status, decoded.status)
        assertEquals(IssueSeverity.P1, decoded.issues[0].severity)
        assertEquals(TerminalLogEntry.LogLevel.TEST, decoded.terminalLogs[0].level)
        assertEquals(TestConfiguration.TestMode.DEEP, decoded.configuration.mode)
    }

    @Test
    fun sessionDiff_calculatesRegressionsAndImprovementsCorrectly() {
        val issueOld = Issue(
            id = "OLD-1",
            severity = IssueSeverity.P2,
            confidence = IssueConfidence.HIGH,
            category = IssueCategory.UI_UX,
            title = "Old UI Glitch",
            description = "Button misaligned"
        )

        val issuePersistent = Issue(
            id = "PER-1",
            severity = IssueSeverity.P1,
            confidence = IssueConfidence.CONFIRMED,
            category = IssueCategory.CRASH,
            title = "Uncaught NPE in onCreate",
            description = "Persistent crash"
        )

        val issueNew = Issue(
            id = "NEW-1",
            severity = IssueSeverity.P0,
            confidence = IssueConfidence.CONFIRMED,
            category = IssueCategory.SECURITY,
            title = "Cleartext HTTP traffic permitted",
            description = "Security regression"
        )

        val sessionA = TestSession(
            id = "S-A",
            apkArtifactId = "apk-1",
            packageName = "com.sample",
            appName = "Sample",
            versionName = "1.0",
            apkSha256 = "sha1",
            deviceModel = "Pixel 7",
            androidVersion = "14",
            issues = listOf(issueOld, issuePersistent),
            healthScore = HealthScore(80, 80, 80, 80, 80, 80, 80, "Formula")
        )

        val sessionB = TestSession(
            id = "S-B",
            apkArtifactId = "apk-1",
            packageName = "com.sample",
            appName = "Sample",
            versionName = "1.1",
            apkSha256 = "sha2",
            deviceModel = "Pixel 7",
            androidVersion = "14",
            issues = listOf(issuePersistent, issueNew),
            healthScore = HealthScore(65, 60, 75, 55, 80, 80, 70, "Formula")
        )

        val diff = SessionDiff.calculate(sessionA, sessionB)

        assertEquals("S-A", diff.baseSessionId)
        assertEquals("S-B", diff.compareSessionId)
        assertEquals(-15, diff.healthScoreDelta)
        assertEquals(1, diff.regressions.size)
        assertEquals("Cleartext HTTP traffic permitted", diff.regressions[0].title)
        assertEquals(1, diff.resolvedIssues.size)
        assertEquals("Old UI Glitch", diff.resolvedIssues[0].title)
        assertEquals(1, diff.persistentIssues.size)
        assertEquals("Uncaught NPE in onCreate", diff.persistentIssues[0].title)

        // Verify SessionDiff itself is serializable
        val diffJson = json.encodeToString(diff)
        val decodedDiff = json.decodeFromString<SessionDiff>(diffJson)
        assertEquals(diff.healthScoreDelta, decodedDiff.healthScoreDelta)
        assertEquals(1, decodedDiff.regressions.size)
    }
}
