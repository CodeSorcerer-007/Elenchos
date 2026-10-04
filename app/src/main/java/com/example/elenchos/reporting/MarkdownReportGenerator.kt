package com.example.elenchos.reporting

import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.domain.model.TestSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MarkdownReportGenerator {

    fun generateReport(session: TestSession, apk: APKArtifact): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val dateStr = dateFormat.format(Date(session.startTimestamp))
        val health = session.healthScore

        return buildString {
            appendLine("# ELENCHOS APP HEALTH REPORT")
            appendLine()
            appendLine("| Metadata | Value |")
            appendLine("| :--- | :--- |")
            appendLine("| **Application** | ${apk.appName} |")
            appendLine("| **Package** | `${apk.packageName}` |")
            appendLine("| **Version** | ${apk.versionName} (Code: ${apk.versionCode}) |")
            appendLine("| **Test Session ID** | `${session.id}` |")
            appendLine("| **Test Date** | $dateStr |")
            appendLine("| **Device** | ${session.deviceModel} |")
            appendLine("| **Android Version** | ${session.androidVersion} |")
            appendLine("| **APK SHA-256** | `${apk.sha256}` |")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## HEALTH SCORE: ${health?.overallScore ?: 0} / 100")
            appendLine()
            appendLine("| Domain | Score | Weight |")
            appendLine("| :--- | :---: | :---: |")
            appendLine("| **Stability** | ${health?.stabilityScore ?: 0} / 100 | 25% |")
            appendLine("| **Security** | ${health?.securityScore ?: 0} / 100 | 25% |")
            appendLine("| **Functionality** | ${health?.functionalityScore ?: 0} / 100 | 20% |")
            appendLine("| **Performance** | ${health?.performanceScore ?: 0} / 100 | 10% |")
            appendLine("| **Accessibility** | ${health?.accessibilityScore ?: 0} / 100 | 10% |")
            appendLine("| **Compatibility** | ${health?.compatibilityScore ?: 0} / 100 | 10% |")
            appendLine()
            appendLine("> **Calculation Note:** ${health?.formulaExplanation}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## EXECUTIVE SUMMARY")
            appendLine()
            appendLine("Automated testing finished with ${session.actionsCount} automated diagnostic events across ${session.screenGraph.nodes.size} discovered screens.")
            appendLine()
            val p0s = session.issues.filter { it.severity == IssueSeverity.P0 }
            val p1s = session.issues.filter { it.severity == IssueSeverity.P1 }
            val p2s = session.issues.filter { it.severity == IssueSeverity.P2 }
            val p3s = session.issues.filter { it.severity == IssueSeverity.P3 }
            val p4s = session.issues.filter { it.severity == IssueSeverity.P4 }

            appendLine("- **P0 Blocker Issues:** ${p0s.size}")
            appendLine("- **P1 Critical Issues:** ${p1s.size}")
            appendLine("- **P2 High Priority Issues:** ${p2s.size}")
            appendLine("- **P3 Medium Priority Issues:** ${p3s.size}")
            appendLine("- **P4 Low Priority Issues:** ${p4s.size}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## TEST COVERAGE METRICS")
            appendLine()
            appendLine("| Metric | Observed Value |")
            appendLine("| :--- | :---: |")
            appendLine("| Screens Discovered | ${session.coverage.screensDiscovered} |")
            appendLine("| Screens Tested | ${session.coverage.screensTested} |")
            appendLine("| Interactive Elements Discovered | ${session.coverage.interactiveElementsDiscovered} |")
            appendLine("| Interactive Elements Exercised | ${session.coverage.interactiveElementsExercised} |")
            appendLine("| Navigation Paths Traversed | ${session.coverage.navigationPathsExercised} |")
            appendLine("| Input Fields Fuzzed | ${session.coverage.inputFieldsTested} |")
            appendLine("| Permission Scenarios Audited | ${session.coverage.permissionFlowsTested} |")
            appendLine("| Lifecycle State Transitions | ${session.coverage.lifecycleScenariosTested} |")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## DETAILED ISSUES")
            appendLine()

            for ((index, issue) in session.issues.withIndex()) {
                appendLine("### ${index + 1}. [${issue.severity.code}] ${issue.title}")
                appendLine("- **Issue ID:** `${issue.id}`")
                appendLine("- **Severity:** ${issue.severity.label} (${issue.severity.code})")
                appendLine("- **Confidence:** ${issue.confidence.label}")
                appendLine("- **Category:** ${issue.category.label}")
                appendLine("- **Affected Screen / Component:** `${issue.affectedScreen}`")
                appendLine()
                appendLine("**Description:**")
                appendLine(issue.description)
                appendLine()
                if (issue.reproductionSteps.isNotEmpty()) {
                    appendLine("**Reproduction Steps:**")
                    issue.reproductionSteps.forEach { appendLine("- $it") }
                    appendLine()
                }
                if (issue.expectedBehavior.isNotBlank()) {
                    appendLine("**Expected Behavior:** ${issue.expectedBehavior}")
                }
                if (issue.actualBehavior.isNotBlank()) {
                    appendLine("**Actual Behavior:** ${issue.actualBehavior}")
                }
                if (!issue.stackTrace.isNullOrBlank()) {
                    appendLine()
                    appendLine("**Captured Stack Trace / Evidence:**")
                    appendLine("```text")
                    appendLine(issue.stackTrace)
                    appendLine("```")
                }
                appendLine()
                appendLine("**Root Cause Hypothesis (${issue.hypothesisConfidence.label} Confidence):**")
                appendLine(issue.rootCauseHypothesis)
                appendLine()
                appendLine("**Remediation Recommendation:**")
                appendLine(issue.recommendedFix)
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
    }
}
