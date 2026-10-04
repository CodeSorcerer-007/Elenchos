package com.example.elenchos.fakes

import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.HealthScore
import com.example.elenchos.domain.model.TestConfiguration
import com.example.elenchos.domain.model.TestPhaseProgress
import com.example.elenchos.domain.model.TestSession
import com.example.elenchos.domain.model.TestStatus
import com.example.elenchos.testing.runtime.ITestRunnerEngine

class FakeTestRunnerEngine : ITestRunnerEngine {

    var wasAborted = false
    var executionCount = 0

    override fun abortTest() {
        wasAborted = true
    }

    override suspend fun executeTestSession(
        apk: APKArtifact,
        config: TestConfiguration,
        onUpdate: (TestSession, List<TestPhaseProgress>, Int) -> Unit
    ): TestSession {
        executionCount++
        val session = TestSession(
            id = "FAKE-SESSION-$executionCount",
            apkArtifactId = apk.id,
            packageName = apk.packageName,
            appName = apk.appName,
            versionName = apk.versionName,
            apkSha256 = apk.sha256,
            deviceModel = "Test Phone",
            androidVersion = "Android 15",
            status = if (wasAborted) TestStatus.CANCELLED else TestStatus.PASSED,
            healthScore = HealthScore(95, 95, 95, 95, 95, 95, 95, "Fake formula")
        )
        val phases = listOf(TestPhaseProgress("Phase 1", isCompleted = true))
        onUpdate(session, phases, 100)
        return session
    }
}
