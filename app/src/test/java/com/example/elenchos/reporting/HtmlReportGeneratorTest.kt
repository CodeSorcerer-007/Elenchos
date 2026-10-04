package com.example.elenchos.reporting

import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.HealthScore
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueConfidence
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.domain.model.TestSession
import com.example.elenchos.domain.model.TestStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlReportGeneratorTest {

    @Test
    fun generateHtml_escapesHtmlSpecialCharactersInMetadataAndIssues() {
        val apk = APKArtifact(
            id = "apk-xss",
            filePath = "/test.apk",
            appName = "Malicious <script>alert(1)</script> App & \"Quotes\"",
            packageName = "com.test.<hack>",
            versionName = "1.0 <beta>",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 35,
            compileSdk = 35,
            fileSizeBytes = 1024,
            formattedSize = "1 KB",
            sha256 = "hash123",
            isDebuggable = false,
            allowsBackup = false,
            usesCleartextTraffic = false,
            is16KbPageAligned = true,
            isInstalledOnDevice = true,
            dexCount = 1,
            totalAssetsCount = 0,
            nativeArchitectures = emptyList(),
            permissions = emptyList(),
            dangerousPermissions = emptyList(),
            activities = emptyList(),
            services = emptyList(),
            receivers = emptyList(),
            providers = emptyList(),
            detectedSecrets = emptyList()
        )

        val issue = Issue(
            id = "ISSUE-<b>01</b>",
            title = "Potential <script> Tag in Title",
            severity = IssueSeverity.P0,
            category = IssueCategory.SECURITY,
            confidence = IssueConfidence.CONFIRMED,
            affectedScreen = "<MainActivity>",
            description = "Description with <b>raw html</b> & unescaped 'single' and \"double\" quotes",
            reproductionSteps = listOf("Step <1>", "Step <2> & check"),
            expectedBehavior = "Expected <sanitized>",
            actualBehavior = "Actual <unsanitized>",
            stackTrace = "java.lang.Exception: <crash>",
            rootCauseHypothesis = "Root cause with <brackets>",
            recommendedFix = "Fix with <remediation>"
        )

        val session = TestSession(
            id = "SESSION-<id>",
            apkArtifactId = apk.id,
            packageName = apk.packageName,
            appName = apk.appName,
            versionName = apk.versionName,
            apkSha256 = apk.sha256,
            deviceModel = "Pixel <b>Pro</b>",
            androidVersion = "Android <15>",
            startTimestamp = 1700000000000L,
            status = TestStatus.CRITICAL_ISSUES_FOUND,
            issues = listOf(issue),
            healthScore = HealthScore(50, 40, 50, 40, 60, 60, 50, "Formula")
        )

        val html = HtmlReportGenerator.generateHtml(session, apk)

        // Verify HTML tags from raw user input are escaped
        assertFalse("Raw script tag should not be present in output HTML", html.contains("<script>alert(1)</script>"))
        assertTrue("Escaped script tag should be present", html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"))
        assertTrue("Ampersand should be escaped", html.contains("&amp;"))
        assertFalse("Unescaped raw HTML tag in issue description should not be present", html.contains("<b>raw html</b>"))
        assertTrue("Escaped tag in description should be present", html.contains("&lt;b&gt;raw html&lt;/b&gt;"))

        // Verify structure
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("ELENCHOS QA LABORATORY REPORT"))
        assertTrue(html.contains("Health Score"))
        assertTrue(html.contains("Stability"))
        assertTrue(html.contains("P0 - BLOCKER"))
    }
}
