package com.example.elenchos.testing.runtime

import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.TestConfiguration
import com.example.elenchos.domain.model.TestPhaseProgress
import com.example.elenchos.domain.model.TestSession

/**
 * Interface contract for the autonomous QA laboratory test execution engine.
 */
interface ITestRunnerEngine {
    fun abortTest()
    suspend fun executeTestSession(
        apk: APKArtifact,
        config: TestConfiguration = TestConfiguration(),
        onUpdate: (TestSession, List<TestPhaseProgress>, Int) -> Unit
    ): TestSession
}
