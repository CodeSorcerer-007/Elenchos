package com.example.elenchos.reporting

import com.example.elenchos.domain.model.Issue

object BugTrackerFormatter {

    fun formatIssueForGitHub(issue: Issue, packageName: String): String {
        return """
## [${issue.severity.code}] ${issue.title}

### Component / Screen
`${issue.affectedScreen}`

### Environment
- **Package:** `$packageName`
- **Severity:** ${issue.severity.label}
- **Confidence:** ${issue.confidence.label}
- **Category:** ${issue.category.label}

### Description
${issue.description}

### Steps to Reproduce
${issue.reproductionSteps.joinToString("\n") { "- $it" }}

### Expected Behavior
${issue.expectedBehavior}

### Actual Behavior
${issue.actualBehavior}

${if (!issue.stackTrace.isNullOrBlank()) "### Stack Trace\n```text\n${issue.stackTrace}\n```\n" else ""}

### Probable Root Cause
${issue.rootCauseHypothesis}

### Suggested Fix
${issue.recommendedFix}

---
*Reported automatically by [Elenchos Mobile QA Laboratory]*
        """.trimIndent()
    }
}
