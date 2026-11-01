package com.example.elenchos.testing.runtime.phases

import android.app.Application
import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.ComponentInfo
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueConfidence
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.domain.model.TerminalLogEntry
import com.example.elenchos.domain.model.TestConfiguration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhasesUnitTest {

    private fun createSampleApk(
        isInstalled: Boolean = false,
        isDebuggable: Boolean = false,
        usesCleartext: Boolean = false,
        permissions: List<String> = emptyList(),
        activities: List<ComponentInfo> = listOf(
            ComponentInfo(name = "com.sample.MainActivity", isExported = true, intentActions = listOf("android.intent.action.MAIN")),
            ComponentInfo(name = "com.sample.DetailActivity", isExported = false)
        ),
        services: List<ComponentInfo> = emptyList()
    ): APKArtifact {
        return APKArtifact(
            id = "sample-apk-id",
            filePath = "/data/local/sample.apk",
            sha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            fileSizeBytes = 5 * 1024 * 1024,
            formattedSize = "5.0 MB",
            appName = "Sample App",
            packageName = "com.sample.app",
            versionName = "2.1.0",
            versionCode = 42,
            minSdk = 24,
            targetSdk = 35,
            isInstalledOnDevice = isInstalled,
            isDebuggable = isDebuggable,
            usesCleartextTraffic = usesCleartext,
            permissions = permissions,
            activities = activities,
            services = services
        )
    }

    private fun createExecutionContext(
        apk: APKArtifact = createSampleApk(),
        config: TestConfiguration = TestConfiguration(),
        isAbortRequested: () -> Boolean = { false },
        logs: MutableList<TerminalLogEntry> = mutableListOf()
    ): PhaseExecutionContext {
        return PhaseExecutionContext(
            context = Application(),
            apk = apk,
            config = config,
            isAbortRequested = isAbortRequested,
            log = { level, tag, msg ->
                logs.add(TerminalLogEntry("00:00:00", level, tag, msg))
            }
        )
    }

    @Test
    fun apkVerificationPhase_successAndAbort() = runTest {
        val phase = ApkVerificationPhase()
        val logs = mutableListOf<TerminalLogEntry>()
        val ctx = createExecutionContext(logs = logs)

        val result = phase.execute(ctx)
        assertTrue(result.success)
        assertFalse(result.isAborted)
        assertTrue(logs.any { it.message.contains("Verifying SHA-256") })

        val abortedCtx = createExecutionContext(isAbortRequested = { true })
        val abortedResult = phase.execute(abortedCtx)
        assertTrue(abortedResult.isAborted)
    }

    @Test
    fun staticSecurityScanPhase_identifiesIssues() = runTest {
        val phase = StaticSecurityScanPhase()
        val apk = createSampleApk(isDebuggable = true, usesCleartext = true)
        val ctx = createExecutionContext(apk = apk)

        val result = phase.execute(ctx)
        assertTrue(result.success)
        assertTrue(ctx.discoveredIssues.any { it.title.contains("android:debuggable") })
        assertTrue(ctx.discoveredIssues.any { it.title.contains("Cleartext HTTP traffic") })
    }

    @Test
    fun targetInstallationCheckPhase_reflectsInstalledStatus() = runTest {
        val phase = TargetInstallationCheckPhase()

        val installedCtx = createExecutionContext(apk = createSampleApk(isInstalled = true))
        val resInstalled = phase.execute(installedCtx)
        assertTrue(resInstalled.details.contains("Target is installed"))

        val headlessCtx = createExecutionContext(apk = createSampleApk(isInstalled = false))
        val resHeadless = phase.execute(headlessCtx)
        assertTrue(resHeadless.details.contains("Headless mode"))
    }

    @Test
    fun startupLatencyPhase_computesDeterministicFootprint() = runTest {
        val phase = StartupLatencyPhase()
        val ctx = createExecutionContext()

        val result = phase.execute(ctx)
        assertTrue(result.success)
        assertTrue(ctx.launchLatencyMs > 0)
        assertTrue(result.details.contains("Computed static footprint"))
    }

    @Test
    fun screenGraphDiscoveryPhase_mapsNodesAndTransitions() = runTest {
        val phase = ScreenGraphDiscoveryPhase()
        val ctx = createExecutionContext()

        val result = phase.execute(ctx)
        assertTrue(result.success)
        assertEquals(2, ctx.screenNodes.size)
        assertEquals(1, ctx.transitions.size)
        assertEquals("SCREEN-MAINACTIVITY", ctx.screenNodes[0].id)
    }

    @Test
    fun inputFuzzingPhase_executesWhenEnabledAndSkipsWhenDisabled() = runTest {
        val phase = InputFuzzingPhase()

        // Enabled
        val enabledCtx = createExecutionContext()
        val enabledRes = phase.execute(enabledCtx)
        assertTrue(enabledRes.success)
        assertTrue(enabledCtx.actualInputFieldsTested >= 1)

        // Disabled
        val disabledCtx = createExecutionContext(config = TestConfiguration(enableInputFuzzing = false))
        val disabledRes = phase.execute(disabledCtx)
        assertTrue(disabledRes.success)
        assertTrue(disabledRes.details.contains("skipped"))
    }

    @Test
    fun lifecyclePreservationPhase_computesDynamicScenarios() = runTest {
        val phase = LifecyclePreservationPhase()
        val apk = createSampleApk(
            services = listOf(ComponentInfo(name = "com.sample.SyncService", isExported = false))
        )
        val ctx = createExecutionContext(apk = apk)

        val result = phase.execute(ctx)
        assertTrue(result.success)
        // 3 base + 2 activities + 2 services = 7 scenarios
        assertEquals(7, ctx.actualLifecycleScenariosTested)
        assertTrue(result.details.contains("7 scenarios validated"))

        // Disabled in config
        val disabledCtx = createExecutionContext(config = TestConfiguration(enableLifecycleTesting = false))
        val disabledRes = phase.execute(disabledCtx)
        assertTrue(disabledRes.details.contains("Skipped"))
    }

    @Test
    fun configurationOrientationPhase_detectsOrientationLocks() = runTest {
        val phase = ConfigurationOrientationPhase()
        val logs = mutableListOf<TerminalLogEntry>()
        val apk = createSampleApk(
            activities = listOf(
                ComponentInfo(name = "com.sample.PortraitPlayerActivity", isExported = true)
            )
        )
        val ctx = createExecutionContext(apk = apk, logs = logs)

        val result = phase.execute(ctx)
        assertTrue(result.success)
        assertTrue(logs.any { it.message.contains("fixed orientation locks") })
    }

    @Test
    fun networkResiliencePhase_detectsMissingNetworkStatePermission() = runTest {
        val phase = NetworkResiliencePhase()
        val apkWithMissingPerm = createSampleApk(
            permissions = listOf("android.permission.INTERNET")
        )
        val ctx = createExecutionContext(apk = apkWithMissingPerm)

        val result = phase.execute(ctx)
        assertTrue(result.success)
        assertTrue(ctx.discoveredIssues.any { it.id == "NET-001" })

        // When ACCESS_NETWORK_STATE is present
        val apkWithBoth = createSampleApk(
            permissions = listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE")
        )
        val ctxBoth = createExecutionContext(apk = apkWithBoth)
        phase.execute(ctxBoth)
        assertFalse(ctxBoth.discoveredIssues.any { it.id == "NET-001" })
    }

    @Test
    fun issueCorrelationPhase_deduplicatesByTitleAndScreen() = runTest {
        val phase = IssueCorrelationPhase()
        val ctx = createExecutionContext()

        val issue1 = Issue(
            id = "1",
            severity = IssueSeverity.P1,
            confidence = IssueConfidence.HIGH,
            category = IssueCategory.CRASH,
            title = "Uncaught NullPointerException",
            description = "Null pointer in MainActivity",
            affectedScreen = "MainActivity"
        )
        val issue2 = issue1.copy(id = "2")
        val issue3 = issue1.copy(id = "3", affectedScreen = "DetailActivity")

        ctx.discoveredIssues.addAll(listOf(issue1, issue2, issue3))
        val result = phase.execute(ctx)

        assertTrue(result.success)
        assertEquals(2, ctx.discoveredIssues.size)
    }

    @Test
    fun healthScoreSynthesisPhase_synthesizesScore() = runTest {
        val phase = HealthScoreSynthesisPhase()
        val ctx = createExecutionContext()

        val result = phase.execute(ctx)
        assertTrue(result.success)
        assertTrue(result.details.contains("Synthesized Health Score"))
    }
}

