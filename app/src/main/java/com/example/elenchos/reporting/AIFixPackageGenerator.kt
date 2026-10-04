package com.example.elenchos.reporting

import com.example.elenchos.domain.model.AIFixPackage
import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.HealthScore
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.TestSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AIFixPackageGenerator {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun generatePackage(session: TestSession, apk: APKArtifact): AIFixPackage {
        val health = session.healthScore ?: HealthScore(
            overallScore = 80,
            stabilityScore = 80,
            functionalityScore = 80,
            securityScore = 80,
            performanceScore = 80,
            accessibilityScore = 80,
            compatibilityScore = 80,
            formulaExplanation = "Baseline score"
        )

        val aiIssues = session.issues.map { issue ->
            AIFixPackage.AIIssueItem(
                id = issue.id,
                severity = issue.severity.code,
                category = issue.category.name,
                title = issue.title,
                confidence = issue.confidence.name,
                affectedScreen = issue.affectedScreen,
                reproductionSteps = issue.reproductionSteps,
                expectedBehavior = issue.expectedBehavior,
                actualBehavior = issue.actualBehavior,
                stackTrace = issue.stackTrace,
                rootCauseHypothesis = issue.rootCauseHypothesis,
                recommendedFix = issue.recommendedFix
            )
        }

        val executiveSummary = "Elenchos Autonomous QA Testing Laboratory completed evaluation of ${apk.appName} (${apk.packageName} v${apk.versionName}) on ${session.deviceModel} running ${session.androidVersion}. Discovered ${session.issues.size} issues (${session.issues.count { it.severity.code == "P0" }} Blocker, ${session.issues.count { it.severity.code == "P1" }} Critical, ${session.issues.count { it.severity.code == "P2" }} High). Overall Health Score: ${health.overallScore}/100."

        val instructions = """
            1. Resolve all P0 (Blocker) and P1 (Critical) issues first before proceeding to P2/P3.
            2. Follow the exact Reproduction Steps to confirm bug state before writing patches.
            3. Address root causes directly rather than wrapping exceptions in generic try-catch blocks.
            4. Verify Android lifecycle state preservation during configuration changes (orientation/backgrounding).
            5. Add unit tests and regression assertions for each resolved defect.
        """.trimIndent()

        return AIFixPackage(
            project = AIFixPackage.ProjectMeta(
                appName = apk.appName,
                packageName = apk.packageName,
                versionName = apk.versionName,
                versionCode = apk.versionCode,
                apkSha256 = apk.sha256,
                targetSdk = apk.targetSdk,
                minSdk = apk.minSdk
            ),
            environment = AIFixPackage.EnvironmentMeta(
                androidVersion = session.androidVersion,
                deviceModel = session.deviceModel,
                testingEngine = "Elenchos Autonomous Mobile QA Laboratory v1.0"
            ),
            executiveSummary = executiveSummary,
            healthScore = health,
            issues = aiIssues,
            developerInstructions = instructions
        )
    }

    fun exportJson(pkg: AIFixPackage): String {
        return json.encodeToString(pkg)
    }

    private fun sanitizePromptText(text: String?): String {
        if (text == null) return ""
        return text.replace("```", "'''")
            .replace("==================================================", "--------------------------------------------------")
            .trim()
    }

    fun generateAIAgentHandoffPrompt(pkg: AIFixPackage): String {
        val issuesText = buildString {
            for ((idx, issue) in pkg.issues.withIndex()) {
                appendLine("### ISSUE #${idx + 1}: [${issue.severity}] ${sanitizePromptText(issue.title)} (${issue.id})")
                appendLine("- **Category:** ${issue.category}")
                appendLine("- **Confidence:** ${issue.confidence}")
                appendLine("- **Affected Component/Screen:** ${sanitizePromptText(issue.affectedScreen)}")
                appendLine("- **Expected Behavior:** ${sanitizePromptText(issue.expectedBehavior)}")
                appendLine("- **Actual Behavior:** ${sanitizePromptText(issue.actualBehavior)}")
                appendLine("- **Reproduction Steps:**")
                issue.reproductionSteps.forEach { appendLine("  - ${sanitizePromptText(it)}") }
                if (!issue.stackTrace.isNullOrBlank()) {
                    appendLine("- **Stack Trace:**")
                    appendLine("```text")
                    appendLine(sanitizePromptText(issue.stackTrace))
                    appendLine("```")
                }
                appendLine("- **Probable Root Cause:** ${sanitizePromptText(issue.rootCauseHypothesis)}")
                appendLine("- **Recommended Remediation:** ${sanitizePromptText(issue.recommendedFix)}")
                appendLine()
            }
        }

        return """
You are a senior Android platform engineer and mobile security specialist.

Analyze the following automated QA laboratory report generated by Elenchos.

Your task is to:
1. Identify root causes of each confirmed defect.
2. Fix confirmed issues cleanly and robustly.
3. Preserve existing functionality without introducing regressions.
4. Verify Android lifecycle behavior and edge-case inputs.
5. Provide code diffs and patch explanations ready for commit.

Do not hide symptoms by adding empty try-catches. Fix the underlying architectural or logical cause.

==================================================
PROJECT METADATA
==================================================
Application: ${pkg.project.appName}
Package: ${pkg.project.packageName}
Version: ${pkg.project.versionName} (code: ${pkg.project.versionCode})
Target SDK: ${pkg.project.targetSdk} | Min SDK: ${pkg.project.minSdk}
APK SHA-256: ${pkg.project.apkSha256}
Testing Environment: ${pkg.environment.deviceModel} - ${pkg.environment.androidVersion}

==================================================
LABORATORY HEALTH SCORE: ${pkg.healthScore.overallScore} / 100
==================================================
Stability:      ${pkg.healthScore.stabilityScore} / 100
Security:       ${pkg.healthScore.securityScore} / 100
Functionality:  ${pkg.healthScore.functionalityScore} / 100
Performance:    ${pkg.healthScore.performanceScore} / 100
Accessibility:  ${pkg.healthScore.accessibilityScore} / 100
Compatibility:  ${pkg.healthScore.compatibilityScore} / 100

==================================================
EXECUTIVE SUMMARY
==================================================
${pkg.executiveSummary}

==================================================
DETECTED ISSUES & EVIDENCE
==================================================
$issuesText

==================================================
ENGINEERING INSTRUCTIONS
==================================================
${pkg.developerInstructions}
        """.trimIndent()
    }
}
