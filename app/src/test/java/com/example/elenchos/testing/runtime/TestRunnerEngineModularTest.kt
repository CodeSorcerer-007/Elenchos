package com.example.elenchos.testing.runtime

import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.TerminalLogEntry
import com.example.elenchos.domain.model.TestConfiguration
import com.example.elenchos.domain.model.TestExecutionCapability
import com.example.elenchos.domain.model.TestStatus
import com.example.elenchos.testing.runtime.phases.PhaseExecutionContext
import com.example.elenchos.testing.runtime.phases.PhaseExecutionResult
import com.example.elenchos.testing.runtime.phases.TestPhase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TestRunnerEngineModularTest {

    @Test
    fun defaultPhases_hasAll14Phases() {
        val phases = TestRunnerEngine.defaultPhases()
        assertEquals(14, phases.size)
        assertEquals("APK Verification & Integrity", phases[0].name)
        assertEquals("Static Security & Manifest Scan", phases[1].name)
        assertEquals("Target Installation & State Check", phases[2].name)
        assertEquals("Startup & Launch Latency", phases[3].name)
        assertEquals("Screen Graph & UI Discovery", phases[4].name)
        assertEquals("Automated UI Interaction", phases[5].name)
        assertEquals("Input Robustness & Fuzzing", phases[6].name)
        assertEquals("Navigation & Back Stack Stress", phases[7].name)
        assertEquals("Lifecycle & State Preservation", phases[8].name)
        assertEquals("Configuration & Orientation", phases[9].name)
        assertEquals("Network Resilience", phases[10].name)
        assertEquals("Crash & ANR Diagnostics", phases[11].name)
        assertEquals("Issue Correlation & Deduplication", phases[12].name)
        assertEquals("Health Score & Report Synthesis", phases[13].name)
    }

    @Test
    fun executeTestSession_withCustomPhases_executesSequentially() = runTest {
        var phase1Executed = false
        var phase2Executed = false

        val customPhase1 = object : TestPhase {
            override val name: String = "Custom Phase 1"
            override suspend fun execute(context: PhaseExecutionContext): PhaseExecutionResult {
                phase1Executed = true
                context.log(TerminalLogEntry.LogLevel.INFO, "TEST", "Phase 1 finished")
                return PhaseExecutionResult(success = true)
            }
        }

        val customPhase2 = object : TestPhase {
            override val name: String = "Custom Phase 2"
            override suspend fun execute(context: PhaseExecutionContext): PhaseExecutionResult {
                phase2Executed = true
                context.log(TerminalLogEntry.LogLevel.SUCCESS, "TEST", "Phase 2 finished")
                return PhaseExecutionResult(success = true)
            }
        }

        val engine = TestRunnerEngine(
            context = android.app.Application(),
            phasesList = listOf(customPhase1, customPhase2)
        )

        val apk = APKArtifact(
            id = "test-apk-id",
            filePath = "/fake/path/app.apk",
            sha256 = "test-sha256",
            fileSizeBytes = 1024,
            formattedSize = "1.0 KB",
            appName = "Test App",
            packageName = "com.test.app",
            versionName = "1.0",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 35
        )

        val session = engine.executeTestSession(apk, TestConfiguration()) { _, _, _ -> }

        assertTrue(phase1Executed)
        assertTrue(phase2Executed)
        assertEquals(TestStatus.PASSED, session.status)
        assertNotNull(session.healthScore)
        assertTrue(session.terminalLogs.any { it.message.contains("Phase 1 finished") })
        assertTrue(session.terminalLogs.any { it.message.contains("Phase 2 finished") })
    }

    @Test
    fun abortTest_marksSessionCancelled() = runTest {
        val abortingPhase = object : TestPhase {
            override val name: String = "Aborting Phase"
            override suspend fun execute(context: PhaseExecutionContext): PhaseExecutionResult {
                return PhaseExecutionResult(isAborted = true)
            }
        }

        val engine = TestRunnerEngine(
            context = android.app.Application(),
            phasesList = listOf(abortingPhase)
        )

        val apk = APKArtifact(
            id = "test-apk-id",
            filePath = "/fake/path/app.apk",
            sha256 = "test-sha256",
            fileSizeBytes = 1024,
            formattedSize = "1.0 KB",
            appName = "Test App",
            packageName = "com.test.app",
            versionName = "1.0",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 35
        )

        val session = engine.executeTestSession(apk, TestConfiguration()) { _, _, _ -> }

        assertEquals(TestStatus.CANCELLED, session.status)
    }

    @Test
    fun issueCorrelationPhase_deduplicatesIdenticalIssues() = runTest {
        val issue1 = com.example.elenchos.domain.model.Issue(
            id = "ISSUE-1",
            title = "Duplicate Crash",
            severity = com.example.elenchos.domain.model.IssueSeverity.P0,
            category = com.example.elenchos.domain.model.IssueCategory.CRASH,
            confidence = com.example.elenchos.domain.model.IssueConfidence.CONFIRMED,
            affectedScreen = "MainActivity",
            description = "NullPointerException",
            reproductionSteps = emptyList(),
            expectedBehavior = "No crash",
            actualBehavior = "Crashed",
            rootCauseHypothesis = "Null ref",
            recommendedFix = "Null check"
        )
        val issue2 = issue1.copy(id = "ISSUE-2")

        val phaseAddingDuplicates = object : TestPhase {
            override val name: String = "Phase With Duplicates"
            override suspend fun execute(context: PhaseExecutionContext): PhaseExecutionResult {
                context.discoveredIssues.add(issue1)
                context.discoveredIssues.add(issue2)
                return PhaseExecutionResult(success = true)
            }
        }

        val correlationPhase = com.example.elenchos.testing.runtime.phases.IssueCorrelationPhase()
        val engine = TestRunnerEngine(
            context = android.app.Application(),
            phasesList = listOf(phaseAddingDuplicates, correlationPhase)
        )

        val apk = APKArtifact(
            id = "test-apk-id",
            filePath = "/fake/path/app.apk",
            sha256 = "test-sha256",
            fileSizeBytes = 1024,
            formattedSize = "1.0 KB",
            appName = "Test App",
            packageName = "com.test.app",
            versionName = "1.0",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 35
        )

        val session = engine.executeTestSession(apk, TestConfiguration()) { _, _, _ -> }
        assertEquals(1, session.issues.size)
        assertEquals("Duplicate Crash", session.issues[0].title)
    }
}
