package com.example.elenchos.testing.static

import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.ComponentInfo
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticSecurityAnalyzerTest {

    private fun createBaseApk(): APKArtifact {
        return APKArtifact(
            id = "test-apk-id",
            filePath = "/fake/path/app.apk",
            appName = "Secure Test App",
            packageName = "com.elenchos.testapp",
            versionName = "1.0.0",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 35,
            compileSdk = 35,
            fileSizeBytes = 15 * 1024 * 1024, // 15MB
            formattedSize = "15.0 MB",
            sha256 = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
            isDebuggable = false,
            allowsBackup = false,
            usesCleartextTraffic = false,
            is16KbPageAligned = true,
            isInstalledOnDevice = true,
            dexCount = 1,
            totalAssetsCount = 5,
            nativeArchitectures = listOf("arm64-v8a", "x86_64"),
            permissions = listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE"),
            dangerousPermissions = emptyList(),
            activities = listOf(
                ComponentInfo("com.elenchos.testapp.MainActivity", isExported = true, intentActions = listOf("android.intent.action.MAIN"))
            ),
            services = emptyList(),
            receivers = emptyList(),
            providers = emptyList(),
            detectedSecrets = emptyList()
        )
    }

    @Test
    fun analyze_cleanApk_producesNoSecurityIssues() {
        val cleanApk = createBaseApk()
        val issues = StaticSecurityAnalyzer.analyze(cleanApk)
        assertTrue("Clean APK should produce 0 security issues", issues.isEmpty())
    }

    @Test
    fun analyze_debuggableTrue_flagsP0Blocker() {
        val apk = createBaseApk().copy(isDebuggable = true)
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val debugIssue = issues.firstOrNull { it.id.startsWith("SEC-DBG") }
        assertNotNull("Should detect debuggable issue", debugIssue)
        assertEquals(IssueSeverity.P0, debugIssue?.severity)
        assertEquals(IssueCategory.SECURITY, debugIssue?.category)
        assertTrue(debugIssue?.title?.contains("android:debuggable") == true)
    }

    @Test
    fun analyze_cleartextTraffic_flagsP2Issue() {
        val apk = createBaseApk().copy(usesCleartextTraffic = true)
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val cleartextIssue = issues.firstOrNull { it.id.startsWith("SEC-NET") }
        assertNotNull("Should detect cleartext issue", cleartextIssue)
        assertEquals(IssueSeverity.P2, cleartextIssue?.severity)
        assertTrue(cleartextIssue?.title?.contains("Cleartext HTTP") == true)
    }

    @Test
    fun analyze_multipleExportedActivitiesWithoutPermission_flagsP1Issue() {
        val exportedActivities = listOf(
            ComponentInfo("com.elenchos.testapp.Act1", isExported = true, permission = null),
            ComponentInfo("com.elenchos.testapp.Act2", isExported = true, permission = null),
            ComponentInfo("com.elenchos.testapp.Act3", isExported = true, permission = null)
        )
        val apk = createBaseApk().copy(activities = exportedActivities)
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val expIssue = issues.firstOrNull { it.id.startsWith("SEC-EXP-ACT") }
        assertNotNull("Should detect exported activities without permission", expIssue)
        assertEquals(IssueSeverity.P1, expIssue?.severity)
    }

    @Test
    fun analyze_exportedServiceWithoutPermission_flagsP1Issue() {
        val exportedServices = listOf(
            ComponentInfo("com.elenchos.testapp.SyncService", isExported = true, permission = null)
        )
        val apk = createBaseApk().copy(services = exportedServices)
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val srvIssue = issues.firstOrNull { it.id.startsWith("SEC-EXP-SRV") }
        assertNotNull("Should detect exported service without permission", srvIssue)
        assertEquals(IssueSeverity.P1, srvIssue?.severity)
    }

    @Test
    fun analyze_exportedReceiverWithoutPermission_flagsP2Issue() {
        val exportedReceivers = listOf(
            ComponentInfo("com.elenchos.testapp.MyReceiver", isExported = true, permission = null)
        )
        val apk = createBaseApk().copy(receivers = exportedReceivers)
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val rcvIssue = issues.firstOrNull { it.id.startsWith("SEC-EXP-RCV") }
        assertNotNull("Should detect exported receiver", rcvIssue)
        assertEquals(IssueSeverity.P2, rcvIssue?.severity)
    }

    @Test
    fun analyze_allowBackupTrue_flagsP3Issue() {
        val apk = createBaseApk().copy(allowsBackup = true)
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val backupIssue = issues.firstOrNull { it.id.startsWith("SEC-BCK") }
        assertNotNull("Should detect backup allowed", backupIssue)
        assertEquals(IssueSeverity.P3, backupIssue?.severity)
    }

    @Test
    fun analyze_overlayPermission_flagsP2Issue() {
        val apk = createBaseApk().copy(permissions = listOf("android.permission.SYSTEM_ALERT_WINDOW"))
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val overlayIssue = issues.firstOrNull { it.id.startsWith("SEC-PERM-OVR") }
        assertNotNull("Should detect SYSTEM_ALERT_WINDOW", overlayIssue)
        assertEquals(IssueSeverity.P2, overlayIssue?.severity)
    }

    @Test
    fun analyze_requestInstallPackagesPermission_flagsP2Issue() {
        val apk = createBaseApk().copy(permissions = listOf("android.permission.REQUEST_INSTALL_PACKAGES"))
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val installIssue = issues.firstOrNull { it.id.startsWith("SEC-PERM-INS") }
        assertNotNull("Should detect REQUEST_INSTALL_PACKAGES", installIssue)
        assertEquals(IssueSeverity.P2, installIssue?.severity)
    }

    @Test
    fun analyze_missing64BitAbi_flagsP1CompatibilityIssue() {
        val apk = createBaseApk().copy(nativeArchitectures = listOf("armeabi-v7a"))
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val abiIssue = issues.firstOrNull { it.id.startsWith("CMP-64BIT") }
        assertNotNull("Should detect lack of 64-bit ABI", abiIssue)
        assertEquals(IssueSeverity.P1, abiIssue?.severity)
        assertEquals(IssueCategory.COMPATIBILITY, abiIssue?.category)
    }

    @Test
    fun analyze_detectedSecrets_flagsP1Issue() {
        val apk = createBaseApk().copy(detectedSecrets = listOf("AKIAIOSFODNN7EXAMPLE"))
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val secretIssue = issues.firstOrNull { it.id.startsWith("SEC-LEAK") }
        assertNotNull("Should detect hardcoded secrets", secretIssue)
        assertEquals(IssueSeverity.P1, secretIssue?.severity)
    }

    @Test
    fun analyze_bloatedPackageSize_flagsP4PerformanceIssue() {
        val apk = createBaseApk().copy(
            fileSizeBytes = 75 * 1024 * 1024, // 75MB > 60MB
            formattedSize = "75.0 MB"
        )
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val sizeIssue = issues.firstOrNull { it.id.startsWith("PERF-SIZE") }
        assertNotNull("Should detect oversized APK", sizeIssue)
        assertEquals(IssueSeverity.P4, sizeIssue?.severity)
        assertEquals(IssueCategory.PERFORMANCE, sizeIssue?.category)
    }

    @Test
    fun analyze_unaligned16Kb_flagsP0CompatibilityIssue() {
        val apk = createBaseApk().copy(
            is16KbPageAligned = false,
            nativeArchitectures = listOf("arm64-v8a")
        )
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val issue16Kb = issues.firstOrNull { it.id.startsWith("CMP-16KB") }
        assertNotNull("Should detect unaligned 16KB native libraries", issue16Kb)
        assertEquals(IssueSeverity.P0, issue16Kb?.severity)
        assertEquals(IssueCategory.COMPATIBILITY, issue16Kb?.category)
    }

    @Test
    fun analyze_lowTargetSdk_flagsP2CompatibilityIssue() {
        val apk = createBaseApk().copy(targetSdk = 33)
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val sdkIssue = issues.firstOrNull { it.id.startsWith("CMP-SDK") }
        assertNotNull("Should detect targetSdk below 34", sdkIssue)
        assertEquals(IssueSeverity.P2, sdkIssue?.severity)
        assertEquals(IssueCategory.COMPATIBILITY, sdkIssue?.category)
    }

    @Test
    fun analyze_exportedContentProviderWithoutPermission_flagsP1Issue() {
        val exportedProviders = listOf(
            ComponentInfo("com.elenchos.testapp.UserDataProvider", isExported = true, permission = null)
        )
        val apk = createBaseApk().copy(providers = exportedProviders)
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val prvIssue = issues.firstOrNull { it.id.startsWith("SEC-EXP-PRV") }
        assertNotNull("Should detect exported ContentProvider without permission", prvIssue)
        assertEquals(IssueSeverity.P1, prvIssue?.severity)
        assertEquals(IssueCategory.SECURITY, prvIssue?.category)
    }

    @Test
    fun analyze_onlyLauncherActivityExported_producesNoActivityIssues() {
        val activities = listOf(
            ComponentInfo(
                "com.elenchos.testapp.MainActivity",
                isExported = true,
                intentActions = listOf("android.intent.action.MAIN", "android.intent.category.LAUNCHER")
            )
        )
        val apk = createBaseApk().copy(activities = activities)
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val actIssue = issues.firstOrNull { it.id.startsWith("SEC-EXP-ACT") }
        assertTrue("Launcher activity alone should not trigger SEC-EXP-ACT", actIssue == null)
    }

    @Test
    fun analyze_nonLauncherExportedActivity_flagsP1Issue() {
        val activities = listOf(
            ComponentInfo(
                "com.elenchos.testapp.MainActivity",
                isExported = true,
                intentActions = listOf("android.intent.action.MAIN")
            ),
            ComponentInfo(
                "com.elenchos.testapp.InternalDebugActivity",
                isExported = true,
                intentActions = emptyList(),
                permission = null
            )
        )
        val apk = createBaseApk().copy(activities = activities)
        val issues = StaticSecurityAnalyzer.analyze(apk)
        val actIssue = issues.firstOrNull { it.id.startsWith("SEC-EXP-ACT") }
        assertNotNull("Non-launcher exported activity should trigger SEC-EXP-ACT", actIssue)
        assertEquals(IssueSeverity.P1, actIssue?.severity)
    }
}
