package com.example.elenchos.testing.runtime.phases

import android.content.Context
import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.ComponentInfo
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.ScreenNode
import com.example.elenchos.domain.model.ScreenTransition
import com.example.elenchos.domain.model.TerminalLogEntry
import com.example.elenchos.domain.model.TestConfiguration
import com.example.elenchos.domain.model.TestExecutionCapability

interface TestPhase {
    val name: String
    val capability: TestExecutionCapability get() = TestExecutionCapability.TESTED
    suspend fun execute(context: PhaseExecutionContext): PhaseExecutionResult
}

data class PhaseExecutionResult(
    val success: Boolean = true,
    val details: String = "",
    val isAborted: Boolean = false
)

class PhaseExecutionContext(
    val context: Context,
    val apk: APKArtifact,
    val config: TestConfiguration,
    val isAbortRequested: () -> Boolean,
    val log: (TerminalLogEntry.LogLevel, String, String) -> Unit
) {
    val discoveredIssues = mutableListOf<Issue>()
    val screenNodes = mutableListOf<ScreenNode>()
    val transitions = mutableListOf<ScreenTransition>()

    // Real exploration and stress metrics
    var realClicks = 0
    var realScrolls = 0
    var realFuzzInjections = 0
    var backActionsTested = 0
    var launchLatencyMs = 0L
    var actualInputFieldsTested = 0
    var actualLifecycleScenariosTested = 0
    var runtimeCrashesCount = 0
    var launcherActivity: ComponentInfo? = null
}
